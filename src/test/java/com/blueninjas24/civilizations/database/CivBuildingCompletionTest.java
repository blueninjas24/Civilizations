package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.FarmObservation;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CivBuildingCompletionTest {
    @TempDir Path directory;ConstructionSupplyOrdersTest f;UUID family=UUID.randomUUID(),builder=UUID.randomUUID();long time=5000;
    @BeforeEach void setup()throws Exception{
        f=new ConstructionSupplyOrdersTest();f.directory=directory;f.setup();f.f.sql("UPDATE treasuries SET hundredths=100000");f.f.sql("UPDATE industry_stock SET units=50 WHERE business_id='"+f.seller+"'");
        var people=f.f.db.getPopulationRepository();people.insertHousehold(new Household(family,f.f.town,"Civ home beneficiary"));people.insertResident(new Resident(builder,"Unemployed fixture builder",f.f.town,family,300,Resident.Status.ALIVE));f.f.sql("INSERT INTO household_capacity VALUES('"+family+"',0)");
        // Existing valid proposal history; initial binding/site preparation is independently tested.
        try(var s=f.c().prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,3000,NULL,NULL)")){
            s.setString(1,f.publicDraft.id().toString());s.setString(2,family.toString());s.setString(3,f.f.town.toString());s.setString(4,f.f.world.toString());s.setString(5,f.publicDraft.template().fingerprint());s.executeUpdate();
        }
        f.f.economy.pulse(4000,false);
        new ConstructionVillageProcurement(f.c(),ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),f.runtime,BusinessOperationPolicy.prototype()).purchaseRemaining(f.publicDraft.id(),f.proof(f.publicDraft),4001,Set.of(f.f.world));
        f.f.economy.pulse(5000,false);assertEquals(3,ConstructionSupplyOrders.deliveryOnly(f.c()).deliverReady(Set.of(f.f.world),8));
    }
    @AfterEach void close(){f.close();}
    CivBuildingCompletion completion(){return new CivBuildingCompletion(f.c());}
    void finishNativeHistory()throws Exception{
        var runtime=new EconomyRuntimeRepository(f.c(),f.f.settings,FarmObservation.Policy.prototype(),216);runtime.configureFarmBusinesses(FarmBusinessPolicy.prototype());runtime.configureConstruction(new ConstructionLaborPolicy(true,1,1,100,25));runtime.startOnline(time,Set.of(f.f.world));
        int ordinal=0;var journal=new ConstructionBlockJournal(f.c());
        for(int stage=0;stage<f.publicDraft.template().stages().size();stage++){
            time+=1000;runtime.pulse(time,false);var terms=f.publicDraft.template().stages().get(stage);assertEquals(terms.work(),new ConstructionPublicWork(f.c()).work(f.publicDraft.id(),stage));
            for(var block:terms.blocks().entrySet()){
                var intent=journal.preparePublic(UUID.randomUUID(),f.publicDraft.id(),ordinal++,block.getValue(),new ConstructionMaterials.Authority(f.publicDraft.id(),f.f.world,time,true,true),time+1,Set.of(f.f.world));journal.acknowledge(intent.operation(),intent.after());
            }
        }
        assertEquals(ConstructionWorkQueue.State.COMPLETE,new ConstructionWorkQueue(f.c()).page(null,8,Set.of(f.f.world)).projects().getFirst().state());
    }
    CivBuildingCompletion.Evidence evidence(){
        var cells=new HashMap<Position,String>();for(var p:ConstructionPreview.requiredCells(f.publicDraft.template(),f.publicDraft.transform(),f.publicDraft.origin()))cells.put(p,p.y()<f.publicDraft.origin().y()?"minecraft:stone":"minecraft:air");
        for(var stage:f.publicDraft.template().stages())stage.blocks().forEach((p,s)->cells.put(f.publicDraft.origin().plus(p),s));
        return new CivBuildingCompletion.Evidence(new ConstructionMaterials.Authority(f.publicDraft.id(),f.f.world,time+1,true,true),cells);
    }
    CivBuildingCompletion.Receipt complete(UUID operation)throws Exception{return completion().completeHome(operation,f.publicDraft.id(),evidence(),time+2,Set.of(f.f.world));}
    long count(String table)throws Exception{try(var s=f.c().createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){return r.getLong(1);}}
    DwellingInspection.Assessment inspection(BuildingLayout layout){return inspection(layout,false);}
    DwellingInspection.Assessment inspection(BuildingLayout layout,boolean damaged){
        var cells=new HashMap<Position,DwellingInspection.Cell>();for(var p:DwellingInspection.requiredCells(layout))cells.put(p,DwellingInspection.Cell.of(DwellingInspection.Type.PASSABLE));
        for(var stage:f.publicDraft.template().stages())for(var block:stage.blocks().entrySet()){
            String state=block.getValue(),material=state.split("\\[",2)[0];var at=f.publicDraft.origin().plus(block.getKey());
            if(material.endsWith("_bed"))cells.put(at,DwellingInspection.Cell.bed(state.contains("part=head"),material,Direction.SOUTH));
            else cells.put(at,DwellingInspection.Cell.of(material.endsWith("_door")?DwellingInspection.Type.PASSABLE:DwellingInspection.Type.SUPPORT));
        }
        if(damaged)for(var unit:layout.units())for(var bed:unit.beds()){cells.put(bed.foot(),DwellingInspection.Cell.of(DwellingInspection.Type.PASSABLE));cells.put(bed.head(),DwellingInspection.Cell.of(DwellingInspection.Type.PASSABLE));}
        return DwellingInspection.inspect(layout,new DwellingInspection.Observation(layout.world(),layout.fingerprint(),time+3,true,cells),time+3,5000,DwellingInspection.Policy.prototype());
    }
    @Test void paidCompletedHomeRegistersVillageOriginReleasesProjectAndActivatesOnlyAfterRealInspection()throws Exception{
        finishNativeHistory();long cash=f.money();UUID operation=UUID.randomUUID();var saved=complete(operation);var registry=new BuildingRegistry(f.c());var entry=registry.find(saved.building()).orElseThrow();
        assertTrue(entry.villageOwned());assertNull(entry.donor());assertEquals(f.f.town,entry.latest().actor());assertTrue(registry.hasDonationPermission(entry.layout().id()));assertEquals(0,HousingCapacity.read(f.c(),f.f.town).get(family));
        var binding=new HousingProjects(f.c()).find(f.publicDraft.id()).orElseThrow();assertTrue(binding.completed());assertFalse(binding.active());assertEquals(entry.layout().id(),binding.completedBuilding());assertEquals(0,new HousingProjects(f.c()).activeCount(f.f.town,f.f.world));assertTrue(new HousingProjects(f.c()).activeHouseholds(f.f.town,f.f.world).isEmpty());
        assertFalse(new ConstructionSiteReservations(f.c()).find(f.publicDraft.id()).orElseThrow().active());assertEquals(cash,f.money());assertEquals(saved,complete(operation));
        var assessed=inspection(entry.layout());assertEquals(4,assessed.units().values().iterator().next().accessibleBeds());
        var report=new DwellingHousing(f.c()).reconcile(entry.layout(),assessed,new DwellingOccupancy.Context(f.f.world,true,DwellingInspection.Policy.prototype(),time+3));assertEquals(1,report.assigned());assertEquals(4,HousingCapacity.read(f.c(),f.f.town).get(family));assertEquals(3,count("residents"));
        assertEquals(0,count("household_capacity_sources"));assertEquals(4,count("dwelling_bed_claims"));
        f.f.db.close();f.f.open();assertEquals(saved,complete(operation));assertEquals(4,HousingCapacity.read(f.c(),f.f.town).get(family));
    }
    @Test void incompleteWrongWorldStaleAndChangedEvidenceNeverRegisterOrReleaseSite()throws Exception{
        assertThrows(SQLException.class,()->complete(UUID.randomUUID()));finishNativeHistory();var original=evidence();
        assertThrows(SQLException.class,()->completion().completeHome(UUID.randomUUID(),f.publicDraft.id(),original,time+2,Set.of()));
        assertThrows(SQLException.class,()->completion().completeHome(UUID.randomUUID(),f.publicDraft.id(),original,time+6002,Set.of(f.f.world)));
        var changed=new HashMap<>(original.cells());changed.put(f.publicDraft.origin(),"minecraft:diamond_block");
        assertThrows(SQLException.class,()->completion().completeHome(UUID.randomUUID(),f.publicDraft.id(),new CivBuildingCompletion.Evidence(original.authority(),changed),time+2,Set.of(f.f.world)));
        assertEquals(0,count("civ_built_buildings"));assertEquals(0,count("building_records"));assertTrue(new ConstructionSiteReservations(f.c()).find(f.publicDraft.id()).orElseThrow().active());
    }
    @Test void lateReleaseFailureRollsBackBuildingUseOriginAndFamilyCompletionTogether()throws Exception{
        finishNativeHistory();UUID operation=UUID.randomUUID();long cash=f.money();f.f.sql("CREATE TRIGGER reject_completion_release AFTER UPDATE OF released_ms ON construction_site_reservations BEGIN SELECT RAISE(ABORT,'fixture completion failure'); END");
        assertThrows(SQLException.class,()->complete(operation));for(var table:List.of("building_records","building_units","building_consent_events","civ_built_buildings"))assertEquals(0,count(table));assertTrue(new HousingProjects(f.c()).find(f.publicDraft.id()).orElseThrow().active());assertEquals(cash,f.money());
        f.f.sql("DROP TRIGGER reject_completion_release");assertTrue(new BuildingRegistry(f.c()).find(complete(operation).building()).orElseThrow().villageOwned());
    }
    @Test void villageContributorNeverBecomesAPlayerDonorEvenForMatchingUuidOrAdministrator()throws Exception{
        finishNativeHistory();var saved=complete(UUID.randomUUID());var registry=new BuildingRegistry(f.c());var layout=registry.find(saved.building()).orElseThrow().layout();
        for(boolean admin:List.of(false,true)){
            var authority=new BuildingRegistry.Authorization(f.f.town,admin,true,true);
            assertThrows(BuildingRegistry.Declined.class,()->registry.reclaim(UUID.randomUUID(),layout.id(),authority,saved.useConsent(),time+3));
            assertThrows(BuildingRegistry.Declined.class,()->registry.donate(UUID.randomUUID(),layout,f.f.town,authority,saved.useConsent(),time+3));
        }
        var request=new BuildingRenovations.Request(UUID.randomUUID(),layout.id(),saved.useConsent(),null,layout.fingerprint(),BuildingRenovations.Action.GRANT,time+3);
        assertThrows(BuildingRegistry.Declined.class,()->new BuildingRenovations(f.c()).confirm(request,new BuildingRenovations.Authority(f.f.town,f.f.world,true,true,true,true)));
    }
    @Test void authoredBedsRemainVillageIdentityAndCannotCountAsEarlyLegacyHousing()throws Exception{
        finishNativeHistory();assertEquals(0,count("civ_built_buildings"));var layout=CivHomeLayout.from(f.publicDraft);
        for(var bed:layout.units().getFirst().beds()){
            var head=bed.head();assertTrue(HousingCapacity.claimed(f.c(),f.f.world,head.x(),head.y(),head.z()));assertFalse(HousingCapacity.claimed(f.c(),UUID.randomUUID(),head.x(),head.y(),head.z()));
        }
        assertEquals(4,count("village_beds"));assertEquals(0,HousingCapacity.read(f.c(),f.f.town).get(family));assertEquals(0,count("dwelling_bed_claims"));
    }
    @Test void schema115FailureAndRetryPreservePlayerOwnershipPaidWorkAndFamilyHistory()throws Exception{
        finishNativeHistory();var p=f.publicDraft;var privatePlan=new ConstructionDraft(UUID.randomUUID(),p.settlement(),p.world(),p.origin().plus(new Position(100,0,0)),p.transform(),p.template(),1000);var playerLayout=CivHomeLayout.from(privatePlan);UUID donor=UUID.randomUUID();
        var registry=new BuildingRegistry(f.c());registry.donate(UUID.randomUUID(),playerLayout,donor,BuildingRegistry.Authorization.playerDeclaration(donor,true),null,1001);var before=registry.find(playerLayout.id()).orElseThrow();long cash=f.money();
        SchemaFixtures.rewind(f.c(),115);f.f.sql("CREATE TABLE civ_built_buildings(blocker INTEGER)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.c()));assertEquals(cash,f.money());assertEquals(before,registry.find(playerLayout.id()).orElseThrow());
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(115,r.getInt(1));}
        f.f.sql("DROP TABLE civ_built_buildings");new SchemaMigrator().migrate(f.c());assertEquals(before,registry.find(playerLayout.id()).orElseThrow());complete(UUID.randomUUID());
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void completedProjectDoesNotBlockAReplacementAfterConfirmedHousingLoss()throws Exception{
        finishNativeHistory();var saved=complete(UUID.randomUUID());var layout=new BuildingRegistry(f.c()).find(saved.building()).orElseThrow().layout();
        var policy=DwellingInspection.Policy.prototype();new DwellingHousing(f.c()).reconcile(layout,inspection(layout),new DwellingOccupancy.Context(f.f.world,true,policy,time+3));
        time+=1000;new DwellingHousing(f.c()).reconcile(layout,inspection(layout,true),new DwellingOccupancy.Context(f.f.world,true,policy,time+3));assertEquals(0,HousingCapacity.read(f.c(),f.f.town).get(family));
        var draft=new ConstructionDraft(UUID.randomUUID(),f.f.town,f.f.world,f.publicDraft.origin().plus(new Position(40,0,0)),f.publicDraft.transform(),f.publicDraft.template(),time+4);
        new ConstructionDraftRepository(f.c()).save(draft);new DemographicRepository(f.c()).initializeClock(42,PopulationPolicy.prototype());
        var binding=new HousingProjects(f.c()).bind(new HousingProjects.Proposal(draft.id(),family,time+4),new DwellingOccupancy.Context(f.f.world,true,policy,time+4),HousingProjectPlanner.prototype(),42,0);
        assertTrue(binding.active());assertTrue(new HousingProjects(f.c()).find(f.publicDraft.id()).orElseThrow().completed());assertEquals(1,new HousingProjects(f.c()).activeCount(f.f.town,f.f.world));
        assertEquals(saved,complete(saved.operation()));assertEquals(0,HousingCapacity.read(f.c(),f.f.town).get(family),"Completion replay cannot resurrect damaged housing");
    }

}
