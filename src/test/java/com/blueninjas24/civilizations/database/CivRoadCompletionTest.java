package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CivRoadCompletionTest {
    @TempDir Path directory;ConstructionRoadIntentsTest f;
    @BeforeEach void setup()throws Exception{f=new ConstructionRoadIntentsTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    private Connection c(){return f.f.c();}
    private CivRoadCompletion roads(){return new CivRoadCompletion(c());}
    private Position p(int x,int y){return new Position(x,y,0);}
    private Map<Position,String> cells(){var cells=new HashMap<Position,String>();for(int x=220;x<=222;x++)for(int y=63;y<=65;y++)cells.put(p(x,y),y>63?"minecraft:air":x==220?"minecraft:dirt_path":"minecraft:grass_block[snowy=false]");return cells;}
    private RoadGroundReview.Observation observation(Map<Position,String> cells){return new RoadGroundReview.Observation(f.f.f.world,6100,true,cells,Set.of());}
    private long count(String table)throws Exception{return f.work.count(table);}
    private long scalar(String sql)throws SQLException{try(var s=c().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    private Map<Position,String> finished()throws Exception{
        f.work.materials(1);f.work.runtime(true).pulse(6000,false);var cells=cells();
        for(int ordinal=0;ordinal<2;ordinal++){
            var intent=new ConstructionRoadIntents(c()).prepare(UUID.randomUUID(),f.f.publicDraft.id(),ordinal,"minecraft:cobblestone",f.design,observation(cells),6101,Set.of(f.f.f.world));
            new ConstructionBlockJournal(c()).acknowledge(intent.operation(),intent.after());cells.put(intent.at(),intent.after());
        }
        return cells;
    }
    private CivRoadCompletion.Receipt complete(UUID op,Map<Position,String> cells)throws Exception{return roads().complete(op,f.f.publicDraft.id(),observation(cells),0,6102,Set.of(f.f.f.world));}
    @Test void completedPaidRoadRetainsIdentityAndExactCellsAcrossRestartWithoutMoreMoneyOrWrites()throws Exception {
        var cells=finished();long money=f.f.money();var op=UUID.randomUUID();var receipt=complete(op,cells);
        assertFalse(new ConstructionSiteReservations(c()).find(f.f.publicDraft.id()).orElseThrow().active());
        var bounds=new ConstructionPreview.Bounds(p(220,63),p(222,65));
        assertEquals(Map.of(p(221,63),"minecraft:cobblestone",p(222,63),"minecraft:cobblestone"),roads().expectedStates(f.f.f.world,bounds));
        assertTrue(roads().expectedStates(UUID.randomUUID(),bounds).isEmpty());assertEquals(1,count("civ_roads"));assertEquals(2,count("civ_road_cells"));
        f.f.f.db.close();f.f.f.open();assertEquals(receipt,roads().findDraft(f.f.publicDraft.id()).orElseThrow());
        assertEquals(receipt,roads().complete(op,receipt.draft(),observation(Map.of()),0,99000,Set.of()));
        assertThrows(SQLException.class,()->complete(UUID.randomUUID(),cells));assertEquals(money,f.f.money());
        assertEquals(1,count("construction_supply_orders"));assertEquals(2,count("construction_block_journal"));assertEquals(0,scalar("SELECT count(*) FROM pragma_foreign_key_check"));
        assertThrows(SQLException.class,()->f.f.f.sql("DELETE FROM civ_road_cells"));assertThrows(SQLException.class,()->f.f.f.sql("UPDATE civ_roads SET completed_ms=7000"));
        var old=f.f.publicDraft;var replacement=new ConstructionDraft(UUID.randomUUID(),old.settlement(),old.world(),old.origin(),old.transform(),old.template(),7000);
        new ConstructionDraftRepository(c()).save(replacement);
        assertThrows(SQLException.class,()->new ConstructionSiteReservations(c()).reserve(replacement.id(),new ConstructionMaterials.Authority(replacement.id(),replacement.world(),7000,true,true),7001));
        assertTrue(new ConstructionSiteReservations(c()).find(replacement.id()).isEmpty());
    }
    @Test void partialStaleChangedOrNewlyProtectedCompletionNeverReleasesTheSite()throws Exception {
        assertThrows(SQLException.class,()->complete(UUID.randomUUID(),cells()));var cells=finished();var op=UUID.randomUUID();
        assertThrows(SQLException.class,()->roads().complete(op,f.f.publicDraft.id(),observation(cells),0,12000,Set.of(f.f.f.world)));
        assertThrows(SQLException.class,()->roads().complete(op,f.f.publicDraft.id(),observation(cells),0,6102,Set.of()));
        var edited=new HashMap<>(cells);edited.put(p(222,63),"minecraft:air");assertThrows(SQLException.class,()->complete(op,edited));
        UUID actor=UUID.randomUUID();new BuildingRegistry(c()).exclude(new BuildingRegistry.Exclusion(UUID.randomUUID(),f.f.f.world,actor,new BuildingLayout.Bounds(p(220,63),p(220,65)),6100),BuildingRegistry.Authorization.playerDeclaration(actor,true));
        assertThrows(SQLException.class,()->complete(op,cells));assertTrue(new ConstructionSiteReservations(c()).find(f.f.publicDraft.id()).orElseThrow().active());
        assertEquals(0,count("civ_roads"));assertEquals(0,count("civ_road_cells"));assertTrue(c().getAutoCommit());
    }
    @Test void lateCellFailureRollsBackOriginCellsAndSiteReleaseThenExactRetryWorks()throws Exception {
        var cells=finished();long money=f.f.money();var op=UUID.randomUUID();
        f.f.f.sql("CREATE TRIGGER reject_road_cell AFTER INSERT ON civ_road_cells WHEN NEW.world_x=222 BEGIN SELECT RAISE(ABORT,'fixture late road cell failure'); END");
        assertThrows(SQLException.class,()->complete(op,cells));assertEquals(0,count("civ_roads"));assertEquals(0,count("civ_road_cells"));
        assertTrue(new ConstructionSiteReservations(c()).find(f.f.publicDraft.id()).orElseThrow().active());assertEquals(money,f.f.money());
        f.f.f.sql("DROP TRIGGER reject_road_cell");complete(op,cells);assertEquals(2,count("civ_road_cells"));
    }
    @Test void populated117UpgradePreservesPaidHistoryAndLateFailureLeavesNoPartialRegistry()throws Exception {
        finished();var intents=new ArrayList<ConstructionBlockJournal.Intent>();
        try(var s=c().createStatement();var r=s.executeQuery("SELECT operation_id FROM construction_block_journal ORDER BY ordinal")){while(r.next())intents.add(new ConstructionBlockJournal(c()).find(UUID.fromString(r.getString(1))).orElseThrow());}
        SchemaFixtures.rewind(c(),117);long money=f.f.money();var migrations=new ArrayList<SchemaMigrator.Migration>();
        for(int i=0;i<117;i++)migrations.add(connection->{});
        migrations.add(connection->{CivRoadCompletion.createSchema(connection);throw new SQLException("fixture late118 failure");});
        assertThrows(SQLException.class,()->new SchemaMigrator(migrations).migrate(c()));
        assertEquals(117,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT count(*) FROM sqlite_master WHERE name IN('civ_roads','civ_road_cells')"));
        new SchemaMigrator().migrate(c());assertEquals(118,scalar("PRAGMA user_version"));assertEquals(0,count("civ_roads"));
        for(var intent:intents)assertEquals(intent,new ConstructionBlockJournal(c()).find(intent.operation()).orElseThrow());
        assertEquals(money,f.f.money());assertTrue(new ConstructionSiteReservations(c()).find(f.f.publicDraft.id()).orElseThrow().active());
        assertEquals(0,scalar("SELECT count(*) FROM pragma_foreign_key_check"));
    }
}
