package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.population.Household;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HousingRoadHandoffTest {
    @TempDir Path directory;ConstructionRoadIntentsTest f;ConstructionDraft road,home;UUID family;HousingProjects.Proposal proposal;
    @BeforeEach void setup()throws Exception{
        f=new ConstructionRoadIntentsTest();f.directory=directory;f.setup();road=f.f.publicDraft;
        f.work.materials(1);
        family=UUID.randomUUID();f.f.f.db.getPopulationRepository().insertHousehold(new Household(family,road.settlement(),"Access family"));
        f.f.f.sql("UPDATE residents SET household_id='"+family+"' WHERE id='"+f.work.worker+"'");f.f.f.sql("INSERT INTO household_capacity VALUES('"+family+"',0)");
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));var anchor=template.anchors().stream().filter(a->a.role()==Role.ROAD).findFirst().orElseThrow().at();
        home=new ConstructionDraft(UUID.randomUUID(),road.settlement(),road.world(),new Position(222-anchor.x(),63-anchor.y(),-anchor.z()),Transform.identity(),template,3000);
        new ConstructionDraftRepository(c()).save(home);proposal=new HousingProjects.Proposal(home.id(),family,3000);
        try(var s=c().prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,3000,NULL,NULL)")){
            s.setString(1,home.id().toString());s.setString(2,family.toString());s.setString(3,home.settlement().toString());s.setString(4,home.world().toString());s.setString(5,template.fingerprint());s.executeUpdate();
        }
        f.f.f.sql("INSERT INTO housing_road_projects VALUES('"+road.id()+"','"+home.id()+"',3000)");
        f.work.runtime(true).pulse(6000,false);assertEquals(2,f.work.work());
    }
    @AfterEach void close(){f.close();}
    private Connection c(){return f.f.c();}
    private long count(String table)throws SQLException{try(var s=c().createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){return r.getLong(1);}}
    private Map<Position,String> finishedRoad()throws Exception{
        var nativeCells=new HashMap<Position,String>();for(var at:RoadGroundReview.requiredCells(f.design.geometry(),0))nativeCells.put(at,at.y()>63?"minecraft:air":at.x()==220?"minecraft:dirt_path":"minecraft:grass_block[snowy=false]");
        for(int i=0;i<2;i++){
            var intent=new ConstructionRoadIntents(c()).prepare(UUID.randomUUID(),road.id(),i,"minecraft:cobblestone",f.design,new RoadGroundReview.Observation(road.world(),6100,true,nativeCells,Set.of()),6101,Set.of(road.world()));
            new ConstructionBlockJournal(c()).acknowledge(intent.operation(),intent.after());nativeCells.put(intent.at(),intent.after());
        }
        new CivRoadCompletion(c()).complete(UUID.randomUUID(),road.id(),new RoadGroundReview.Observation(road.world(),6101,true,nativeCells,Set.of()),0,6102,Set.of(road.world()));return nativeCells;
    }
    private HousingProjectPreparation.Request request(Map<Position,String> edits){
        var cells=new HashMap<Position,Cell>();var terrain=new HashMap<Position,ConstructionLandPolicy.Kind>();var states=new HashMap<Position,String>();
        for(int x=210;x<=232;x++)for(int z=-10;z<=14;z++)for(int y=63;y<=70;y++){
            var at=new Position(x,y,z);boolean path=y==63&&z==0&&x>=220&&x<=222;
            cells.put(at,new Cell(y>63,y==63,x==220&&path,false));terrain.put(at,y>63?ConstructionLandPolicy.Kind.AIR:x==220&&path?ConstructionLandPolicy.Kind.ROAD:ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND);
            states.put(at,y>63?"minecraft:air":path?x==220?"minecraft:dirt_path":"minecraft:cobblestone":"minecraft:grass_block[snowy=false]");
        }
        states.putAll(edits);
        var bounds=new Bounds(new Position(210,63,-10),new Position(232,70,14));
        return new HousingProjectPreparation.Request(proposal,home.settlement(),home.world(),home.origin(),bounds,new ConstructionSitePlanner.Survey(home.world(),6102,true,true,cells,List.of(),List.of()),terrain,states);
    }
    private HousingRoadHandoff.Receipt handoff(Map<Position,String> edits)throws SQLException{
        return new HousingRoadHandoff(c()).prepare(request(edits),new DwellingOccupancy.Context(home.world(),true,DwellingInspection.Policy.prototype(),6102),new ConstructionLandPolicy(0));
    }
    @Test void requiresRealCompletedRoadThenReservesFrozenHomeOnceWithoutPurchasingOrAddingCapacity()throws Exception{
        assertThrows(SQLException.class,()->handoff(Map.of()));assertTrue(new ConstructionSiteReservations(c()).find(home.id()).isEmpty());
        finishedRoad();long money=f.f.money(),orders=count("construction_supply_orders"),people=count("residents");var saved=handoff(Map.of());
        assertEquals(home,saved.home());assertTrue(saved.site().active());assertFalse(new ConstructionSiteReservations(c()).find(road.id()).orElseThrow().active());
        var status=new HousingProjectStatus(c()).find(home.id(),new DwellingOccupancy.Context(home.world(),true,DwellingInspection.Policy.prototype(),6102));
        assertTrue(status.pause().orElseThrow().contains("handed off"));
        assertEquals(orders,count("construction_supply_orders"));assertEquals(money,f.f.money());assertEquals(people,count("residents"));assertEquals(1,count("housing_project_bindings"));assertEquals(2,count("construction_block_journal"));
        assertEquals(saved,handoff(Map.of()));f.f.f.db.close();f.f.f.open();assertEquals(saved,handoff(Map.of()));
        assertEquals(0,HousingCapacity.read(c(),home.settlement()).get(family));
    }
    @Test void changedRoadNewProtectionOrSatisfiedFamilyCannotAcquireHomeLand()throws Exception{
        finishedRoad();assertThrows(SQLException.class,()->handoff(Map.of(new Position(221,63,0),"minecraft:dirt_path")));
        f.f.f.sql("UPDATE household_capacity SET capacity=1 WHERE household_id='"+family+"'");assertThrows(SQLException.class,()->handoff(Map.of()));
        f.f.f.sql("UPDATE household_capacity SET capacity=0 WHERE household_id='"+family+"'");UUID actor=UUID.randomUUID();
        new BuildingRegistry(c()).exclude(new BuildingRegistry.Exclusion(UUID.randomUUID(),home.world(),actor,new BuildingLayout.Bounds(home.origin(),home.origin()),6102),BuildingRegistry.Authorization.playerDeclaration(actor,true));
        assertThrows(SQLException.class,()->handoff(Map.of()));assertTrue(new ConstructionSiteReservations(c()).find(home.id()).isEmpty());assertEquals(1,count("civ_roads"));
    }
    @Test void failedFinalHomeSiteInsertRetainsRoadAndFamilyThenRetryWorks()throws Exception{
        finishedRoad();f.f.f.sql("CREATE TRIGGER fixture_handoff_failure BEFORE INSERT ON construction_site_reservations BEGIN SELECT RAISE(ABORT,'fixture handoff failure'); END");
        assertThrows(SQLException.class,()->handoff(Map.of()));assertTrue(c().getAutoCommit());assertEquals(1,count("civ_roads"));assertEquals(1,count("housing_road_projects"));assertEquals(1,count("housing_project_bindings"));
        assertTrue(new ConstructionSiteReservations(c()).find(home.id()).isEmpty());f.f.f.sql("DROP TRIGGER fixture_handoff_failure");assertTrue(handoff(Map.of()).site().active());
    }
}
