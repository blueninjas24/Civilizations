package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionRoadIntentsTest {
    @TempDir Path directory;
    ConstructionPublicWorkTest work;ConstructionSupplyOrdersTest f;LocalRoadDesign.Design design;
    @BeforeEach void setup()throws Exception{
        work=new ConstructionPublicWorkTest();work.directory=directory;work.setup();f=work.f;
        var recipe=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"));
        design=LocalRoadDesign.freeze(new LocalRoadAccessPlanner.Result(LocalRoadAccessPlanner.Status.READY,List.of(p(222,63),p(221,63),p(220,63)),2,3),recipe).orElseThrow();
        f.publicDraft=new ConstructionDraft(UUID.randomUUID(),f.f.town,f.f.world,design.origin(),Transform.identity(),design.template(),3000);
        new ConstructionDraftRepository(f.c()).save(f.publicDraft);new ConstructionSiteReservations(f.c()).reserve(f.publicDraft.id(),f.proof(f.publicDraft),3001);
    }
    @AfterEach void close(){work.close();}
    private static Position p(int x,int y){return new Position(x,y,0);}
    private Map<Position,String> cells(){var cells=new HashMap<Position,String>();for(int x=220;x<=222;x++)for(int y=63;y<=65;y++)cells.put(p(x,y),y>63?"minecraft:air":x==220?"minecraft:dirt_path":"minecraft:grass_block[snowy=false]");return cells;}
    private RoadGroundReview.Observation observation(Map<Position,String> cells){return new RoadGroundReview.Observation(f.f.world,6100,true,cells,Set.of());}
    private ConstructionBlockJournal.Intent prepare(UUID operation,int ordinal,Map<Position,String> cells)throws Exception{return new ConstructionRoadIntents(f.c()).prepare(operation,f.publicDraft.id(),ordinal,"minecraft:cobblestone",design,observation(cells),6101,Set.of(f.f.world));}
    private void fundAndWork()throws Exception{work.materials(1);work.runtime(true).pulse(6000,false);assertEquals(2,work.work());}
    private long count(String table)throws Exception{return work.count(table);}
    @Test void publicRoadLaborWaitsWhenItsFutureFamilyHomeIsNoLongerNeeded()throws Exception{
        work.materials(1);
        var family=UUID.randomUUID();f.f.db.getPopulationRepository().insertHousehold(new com.blueninjas24.civilizations.population.Household(family,f.f.town,"Access beneficiary"));
        f.f.sql("UPDATE residents SET household_id='"+family+"' WHERE id='"+work.worker+"'");
        f.f.sql("INSERT INTO household_capacity VALUES('"+family+"',1)");
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        var anchor=template.anchors().stream().filter(a->a.role()==Role.ROAD).findFirst().orElseThrow().at();
        var home=new ConstructionDraft(UUID.randomUUID(),f.f.town,f.f.world,new Position(222-anchor.x(),63-anchor.y(),-anchor.z()),Transform.identity(),template,3000);
        new ConstructionDraftRepository(f.c()).save(home);
        // Previously valid saved purpose; its beneficiary has since gained adequate housing.
        try(var s=f.c().prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,3000,NULL,NULL)")){
            s.setString(1,home.id().toString());s.setString(2,family.toString());s.setString(3,f.f.town.toString());s.setString(4,f.f.world.toString());s.setString(5,template.fingerprint());s.executeUpdate();
        }
        f.f.sql("INSERT INTO housing_road_projects VALUES('"+f.publicDraft.id()+"','"+home.id()+"',3000)");
        var runtime=work.runtime(true);runtime.pulse(6000,false);
        assertEquals(0,work.work());assertEquals(0,count("construction_public_work"));
        assertEquals(1,f.orders().stock(f.publicDraft.id()).get(com.blueninjas24.civilizations.economy.Commodity.STONE));
        f.f.sql("UPDATE household_capacity SET capacity=0 WHERE household_id='"+family+"'");runtime.pulse(7000,false);
        assertEquals(2,work.work());assertEquals(1,count("construction_public_work"));assertEquals(0,count("construction_block_journal"));
    }
    @Test void paidNaturalBeforeStateAndExactPrefixSurviveRestartAndReplayWithoutMoreMoney()throws Exception{
        fundAndWork();long money=f.money();var operation=UUID.randomUUID();var first=prepare(operation,0,cells());
        assertEquals("minecraft:grass_block[snowy=false]",first.before());assertEquals(p(221,63),first.at());
        assertThrows(SQLException.class,()->prepare(UUID.randomUUID(),1,cells()));assertEquals(1,count("construction_road_ground"));
        var journal=new ConstructionBlockJournal(f.c());journal.acknowledge(operation,"minecraft:cobblestone");var cells=cells();cells.put(first.at(),"minecraft:cobblestone");
        var second=prepare(UUID.randomUUID(),1,cells);assertEquals(p(222,63),second.at());assertEquals(2,count("construction_road_ground"));
        f.f.db.close();f.f.open();assertEquals(second,new ConstructionBlockJournal(f.c()).find(second.operation()).orElseThrow());
        var replay=new ConstructionRoadIntents(f.c()).prepare(second.operation(),f.publicDraft.id(),1,"minecraft:cobblestone",design,observation(Map.of()),99000,Set.of());
        assertEquals(second,replay);assertEquals(money,f.money());assertEquals(1,count("construction_supply_orders"));
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void unfundedOrUnworkedStageCannotCreateGroundReceipt()throws Exception{
        assertThrows(SQLException.class,()->prepare(UUID.randomUUID(),0,cells()));assertEquals(0,count("construction_road_ground"));
        work.materials(1);assertThrows(SQLException.class,()->prepare(UUID.randomUUID(),0,cells()));assertEquals(0,count("construction_block_journal"));
    }
    @Test void editedMissingAbsentOrProtectedEvidenceRollsBackTheWholePreparation()throws Exception{
        fundAndWork();var operation=UUID.randomUUID();var bad=cells();bad.put(p(222,63),"minecraft:diamond_ore");
        assertThrows(SQLException.class,()->prepare(operation,0,bad));
        assertThrows(SQLException.class,()->new ConstructionRoadIntents(f.c()).prepare(operation,f.publicDraft.id(),0,"minecraft:cobblestone",design,observation(cells()),6101,Set.of()));
        assertThrows(SQLException.class,()->new ConstructionRoadIntents(f.c()).prepare(operation,f.publicDraft.id(),0,"minecraft:cobblestone",design,new RoadGroundReview.Observation(f.f.world,6100,true,cells(),Set.of(p(220,64))),6101,Set.of(f.f.world)));
        assertEquals(0,count("construction_road_ground"));assertEquals(0,count("construction_block_journal"));assertTrue(f.c().getAutoCommit());
    }
    @Test void savedProtectionAtRetainedEndpointOverridesAnOlderClearNativeClaim()throws Exception{
        fundAndWork();UUID actor=UUID.randomUUID();var bounds=new com.blueninjas24.civilizations.construction.BuildingLayout.Bounds(p(220,63),p(220,65));
        new BuildingRegistry(f.c()).exclude(new BuildingRegistry.Exclusion(UUID.randomUUID(),f.f.world,actor,bounds,6100),BuildingRegistry.Authorization.playerDeclaration(actor,true));
        assertThrows(SQLException.class,()->prepare(UUID.randomUUID(),0,cells()));
        assertEquals(0,count("construction_road_ground"));assertEquals(0,count("construction_block_journal"));assertEquals(2,work.work());
    }
    @Test void nativeRenewalAlwaysChecksFreshEvidenceWhileHistoryReplayRemainsOnlyAReceipt()throws Exception{
        fundAndWork();UUID operation=UUID.randomUUID();var first=prepare(operation,0,cells());var road=new ConstructionRoadIntents(f.c());
        assertThrows(SQLException.class,()->road.prepareCurrent(operation,f.publicDraft.id(),0,"minecraft:cobblestone",design.geometry(),observation(cells()),99000,Set.of(f.f.world),0));
        assertThrows(SQLException.class,()->road.prepareCurrent(operation,f.publicDraft.id(),0,"minecraft:cobblestone",design.geometry(),observation(cells()),6101,Set.of(),0));
        var after=cells();after.put(first.at(),first.after());
        assertEquals(first,road.prepareCurrent(operation,f.publicDraft.id(),0,"minecraft:cobblestone",design.geometry(),observation(after),6101,Set.of(f.f.world),0));
        assertEquals(1,count("construction_road_ground"));assertEquals(1,count("construction_block_journal"));
    }
    @Test void lateJournalFailureRollsBackReceiptAndExactRetryKeepsPaidAssets()throws Exception{
        fundAndWork();long money=f.money();var operation=UUID.randomUUID();
        f.f.sql("CREATE TRIGGER reject_road AFTER INSERT ON construction_block_journal WHEN NEW.before_state<>'minecraft:air' BEGIN SELECT RAISE(ABORT,'fixture late road failure'); END");
        assertThrows(SQLException.class,()->prepare(operation,0,cells()));assertEquals(0,count("construction_road_ground"));assertEquals(money,f.money());assertEquals(2,work.work());
        f.f.sql("DROP TRIGGER reject_road");prepare(operation,0,cells());assertEquals(1,count("construction_road_ground"));
        assertThrows(SQLException.class,()->f.f.sql("DELETE FROM construction_road_ground"));
        assertThrows(SQLException.class,()->new ConstructionBlockJournal(f.c()).prepare(operation,f.publicDraft.id(),0,"minecraft:cobblestone"));
    }
    @Test void callerCannotInventCompletedRoadProvenanceForPlayerPaving()throws Exception {
        fundAndWork();var cells=cells();cells.put(p(220,63),"minecraft:stone_bricks");
        var forged=new RoadGroundReview.Observation(f.f.world,6100,true,cells,Set.of(),Map.of(p(220,63),"minecraft:stone_bricks"));
        assertThrows(SQLException.class,()->new ConstructionRoadIntents(f.c()).prepare(UUID.randomUUID(),f.publicDraft.id(),0,"minecraft:cobblestone",design,forged,6101,Set.of(f.f.world)));
        assertEquals(0,count("construction_road_ground"));assertEquals(0,count("construction_block_journal"));
    }
}
