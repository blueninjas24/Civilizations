package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.database.DwellingOccupancy.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DwellingOccupancyTest {
    @TempDir Path directory;
    private static UUID id(long n){return new UUID(0,n);}
    private final UUID town=id(1),world=id(2),donor=id(3),building=id(4),a=id(5),b=id(6),family=id(7),other=id(8),consent=id(9);
    private static Position p(int x,int y,int z){return new Position(x,y,z);}
    private static Bounds box(int x,int y,int z,int xx,int yy,int zz){return new Bounds(p(x,y,z),p(xx,yy,zz));}
    private BuildingLayout layout(){var bounds=box(0,0,0,9,3,5);return new BuildingLayout(building,town,world,Kind.HOUSING,bounds,bounds,p(1,1,0),List.of(box(0,0,0,9,3,1)),List.of(
        new Unit(a,p(1,1,2),List.of(box(0,0,2,4,3,5)),List.of(new Bed(p(1,1,3),Direction.EAST),new Bed(p(1,1,4),Direction.EAST))),
        new Unit(b,p(6,1,2),List.of(box(5,0,2,9,3,5)),List.of(new Bed(p(6,1,3),Direction.EAST),new Bed(p(6,1,4),Direction.EAST)))));}
    private Map<Position,Cell> cells(){var result=new HashMap<Position,Cell>();for(var p:DwellingInspection.requiredCells(layout()))result.put(p,Cell.of(p.y()==0||p.y()==3?Type.SUPPORT:Type.PASSABLE));
        for(var u:layout().units())for(var bed:u.beds()){result.put(bed.foot(),Cell.bed(false,"minecraft:white_bed",bed.facing()));result.put(bed.head(),Cell.bed(true,"minecraft:white_bed",bed.facing()));}return result;}
    private Assessment assessment(long time){return assessment(time,cells(),true,Policy.prototype());}
    private Assessment assessment(long time,Map<Position,Cell> cells,boolean complete,Policy policy){return DwellingInspection.inspect(layout(),new Observation(world,layout().fingerprint(),time,complete,cells),time,30_000,policy);}
    private Context context(long time){return new Context(world,true,Policy.prototype(),time);}
    private Connection open(int version)throws Exception {var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("occupancy.sqlite"));sql(c,"PRAGMA foreign_keys=ON");SchemaMigrator.throughVersion(version).migrate(c);return c;}
    private void seed(Connection c)throws Exception {
        try(var s=c.prepareStatement("INSERT INTO settlements VALUES(?,'Retained village','fixture',0,64,0,1,3,9)")){s.setString(1,town.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO observed_worlds VALUES(?,'fixture')")){s.setString(1,world.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,world.toString());s.executeUpdate();}
        var people=new PopulationRepository(c);people.insertHousehold(new Household(family,town,"Existing family"));people.insertHousehold(new Household(other,town,"Other family"));
        people.insertResident(new Resident(id(10),"First",town,family,240,Resident.Status.ALIVE));people.insertResident(new Resident(id(11),"Second",town,family,120,Resident.Status.ALIVE));
        people.insertResident(new Resident(id(12),"Third",town,other,240,Resident.Status.ALIVE));
        sql(c,"INSERT INTO household_capacity VALUES('"+family+"',7)");
        new BuildingRegistry(c).donate(consent,layout(),donor,BuildingRegistry.Authorization.playerDeclaration(donor,true),null,1000);
    }
    private static void sql(Connection c,String sql)throws SQLException{try(var s=c.createStatement()){s.execute(sql);}}
    private static long number(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    private static void healthy(Connection c)throws SQLException {assertEquals(0,number(c,"SELECT count(*) FROM pragma_foreign_key_check"));try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){assertEquals("ok",r.getString(1));}}
    private Assignment assign(UUID op,UUID household,UUID unit,UUID review,UUID previous,long now){return new Assignment(op,household,unit,review,previous,now);}

    private ConstructionDraft homeDraft(Connection c,long identity)throws Exception {
        var template=com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        var draft=new ConstructionDraft(id(identity),town,world,p(100+(int)identity,64,100),Transform.identity(),template,1100);
        return new ConstructionDraftRepository(c).save(draft);
    }
    private void verifiedShortage(Connection c)throws Exception {
        if(number(c,"SELECT count(*) FROM demographic_clock")==0)new DemographicRepository(c).initializeClock(42,PopulationPolicy.prototype());
        sql(c,"UPDATE household_capacity SET capacity=0");
        var damaged=cells();damaged.put(p(1,3,3),Cell.of(Type.PASSABLE));damaged.put(p(6,3,3),Cell.of(Type.PASSABLE));
        new DwellingOccupancy(c).review(id(20),building,assessment(1100,damaged,true,Policy.prototype()),context(1100));
    }
    private HousingProjectPreparation.Request preparation(long identity){
        var cells=new HashMap<Position,ConstructionPreview.Cell>();var terrain=new HashMap<Position,ConstructionLandPolicy.Kind>();
        for(int x=180;x<=220;x++)for(int z=180;z<=220;z++)for(int y=63;y<=68;y++){
            var at=p(x,y,z);boolean road=x==200&&y==63&&z==200;
            cells.put(at,new ConstructionPreview.Cell(y>63,y==63,road,false));
            terrain.put(at,road?ConstructionLandPolicy.Kind.ROAD:y==63?ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND:ConstructionLandPolicy.Kind.AIR);
        }
        return new HousingProjectPreparation.Request(new HousingProjects.Proposal(id(identity),family,1200),town,world,p(208,64,200),
                new ConstructionPreview.Bounds(p(180,63,180),p(220,70,220)),new ConstructionSitePlanner.Survey(world,1100,true,true,cells,List.of(),List.of()),terrain);
    }
    private HousingProjectPreparation.Receipt prepare(Connection c,HousingProjectPreparation.Request request,long time)throws Exception {
        var template=com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        return new HousingProjectPreparation(c).prepare(request,context(time),List.of(template),HousingProjectPlanner.prototype(),new ConstructionLandPolicy(4),42,0);
    }
    @Test void automaticDryPreparationCommitsFamilyPlanAndSiteTogetherAndOnlyReplaysHistory()throws Exception {
        var request=preparation(70);HousingProjectPreparation.Receipt saved;
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);verifiedShortage(c);saved=prepare(c,request,1200);assertTrue(saved.family().active());assertTrue(saved.site().active());
            assertEquals(ConstructionSitePlanner.Status.READY,saved.review().orElseThrow().status());
            assertEquals(saved.draft().origin(),saved.review().orElseThrow().selected().orElseThrow().origin());
            var replay=prepare(c,request,8000);assertTrue(replay.review().isEmpty());assertEquals(saved.draft(),replay.draft());assertEquals(saved.site(),replay.site());
            new ConstructionSiteReservations(c).releaseUnused(saved.draft().id(),8000);
            new HousingProjects(c).cancel(id(80),saved.draft().id(),8100,context(8100));
            var cancelled=prepare(c,request,8200);assertFalse(cancelled.family().active());assertFalse(cancelled.site().active());assertTrue(cancelled.review().isEmpty());
            assertEquals(0,number(c,"SELECT count(*) FROM construction_supply_orders"));assertEquals(0,number(c,"SELECT count(*) FROM construction_block_journal"));
            assertEquals(3,number(c,"SELECT count(*) FROM residents"));assertEquals(0,HousingCapacity.read(c,town).get(family));healthy(c);
        }
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){var replay=prepare(c,request,9000);assertEquals(saved.draft(),replay.draft());assertFalse(replay.family().active());healthy(c);}
    }
    @Test void atomicHomePreparationRollsBackBothEarlierWritesIfSiteReservationFails()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);verifiedShortage(c);var request=preparation(70);
            sql(c,"CREATE TRIGGER fail_preparation BEFORE INSERT ON construction_site_reservations BEGIN SELECT RAISE(ABORT,'injected'); END");
            assertThrows(SQLException.class,()->prepare(c,request,1200));assertTrue(c.getAutoCommit());
            assertEquals(0,number(c,"SELECT count(*) FROM construction_drafts"));assertEquals(0,number(c,"SELECT count(*) FROM housing_project_bindings"));
            assertEquals(0,number(c,"SELECT count(*) FROM construction_site_reservations"));
            sql(c,"DROP TRIGGER fail_preparation");assertTrue(prepare(c,request,1200).site().active());healthy(c);
        }
    }
    @Test void automaticHomePreparationRechecksSavedProtectionAndCannotUseUnsurveyedMargins()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);verifiedShortage(c);var request=preparation(70);
            var incomplete=new HousingProjectPreparation.Request(request.proposal(),town,world,request.need(),request.search(),request.survey(),Map.of());
            assertThrows(BuildingRegistry.Declined.class,()->prepare(c,incomplete,1200));
            var outside=new HousingProjectPreparation.Request(request.proposal(),town,world,request.need(),new ConstructionPreview.Bounds(p(200,63,200),p(209,70,209)),request.survey(),request.terrain());
            assertThrows(BuildingRegistry.Declined.class,()->prepare(c,outside,1200));
            assertThrows(BuildingRegistry.Declined.class,()->prepare(c,request,8000));
            // A saved exclusion can arrive after native evidence was captured; the SQL owner must still see it.
            new BuildingRegistry(c).exclude(new BuildingRegistry.Exclusion(id(81),world,donor,box(180,0,180,220,100,220),1150),BuildingRegistry.Authorization.playerDeclaration(donor,true));
            assertThrows(BuildingRegistry.Declined.class,()->prepare(c,request,1200));
            assertEquals(0,number(c,"SELECT count(*) FROM construction_drafts"));assertEquals(0,number(c,"SELECT count(*) FROM housing_project_bindings"));healthy(c);
        }
    }
    private void requireProjectAuthority(Connection c,UUID draft)throws Exception{
        c.setAutoCommit(false);try{new ConstructionSiteReservations(c).requireActiveRows(new ConstructionDraftRepository(c).find(draft).orElseThrow());}
        finally{c.rollback();c.setAutoCommit(true);}
    }
    @Test void unstartedPreparedHomesPauseForRehousingUnknownCapacityFamilyGrowthOrSavedDeath()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);verifiedShortage(c);var receipt=prepare(c,preparation(70),1200);var draft=receipt.draft();requireProjectAuthority(c,draft.id());
            sql(c,"UPDATE household_capacity SET capacity=7");
            assertThrows(SQLException.class,()->requireProjectAuthority(c,draft.id()));
            sql(c,"DELETE FROM household_capacity");assertThrows(SQLException.class,()->requireProjectAuthority(c,draft.id()));
            sql(c,"INSERT INTO household_capacity VALUES('"+family+"',0)");
            var people=new PopulationRepository(c);for(int i=30;i<33;i++)people.insertResident(new Resident(id(i),"New fixture member",town,family,240,Resident.Status.ALIVE));
            assertThrows(SQLException.class,()->requireProjectAuthority(c,draft.id()));
            sql(c,"UPDATE residents SET status='DECEASED' WHERE id IN ('"+id(30)+"','"+id(31)+"','"+id(32)+"')");requireProjectAuthority(c,draft.id());
            sql(c,"UPDATE residents SET status='DECEASED' WHERE household_id='"+family+"'");assertThrows(SQLException.class,()->requireProjectAuthority(c,draft.id()));
            assertTrue(new HousingProjects(c).find(draft.id()).orElseThrow().active());assertTrue(new ConstructionSiteReservations(c).find(draft.id()).orElseThrow().active());
            assertEquals(0,number(c,"SELECT count(*) FROM construction_supply_orders"));assertEquals(0,number(c,"SELECT count(*) FROM construction_block_journal"));healthy(c);
        }
    }
    @Test void changedHousingNeedDoesNotStrandADurableIntentThatMayAlreadyExistInTheWorld()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);verifiedShortage(c);var draft=prepare(c,preparation(70),1200).draft();
            // Development journal intent only; no native placement or economic authority is manufactured.
            var intent=new ConstructionBlockJournal(c).prepare(id(90),draft.id(),0,"minecraft:cobblestone");
            sql(c,"UPDATE household_capacity SET capacity=7");requireProjectAuthority(c,draft.id());
            assertEquals(intent,new ConstructionBlockJournal(c).find(id(90)).orElseThrow());
            assertThrows(BuildingRegistry.Declined.class,()->new HousingProjects(c).cancel(id(91),draft.id(),1300,context(1300)));
            assertTrue(new ConstructionSiteReservations(c).find(draft.id()).orElseThrow().active());healthy(c);
        }
    }
    @Test void homeProjectsReserveOnlyTheirFamilyAndKeepReceiptsAcrossRestart()throws Exception {
        HousingProjects.Binding saved;
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);verifiedShortage(c);var draft=homeDraft(c,70);var projects=new HousingProjects(c);
            var proposal=new HousingProjects.Proposal(draft.id(),family,1200);
            saved=projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0);
            assertEquals(saved,projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0));
            assertEquals(2,saved.residents());assertEquals(0,saved.previousCapacity());
            sql(c,"INSERT INTO household_capacity VALUES('"+other+"',0)");
            var decisions=new HousingPlanning(c).review(town,context(1200),List.of(draft.template()),HousingProjectPlanner.prototype(),42,0).plan().recommendations();
            assertEquals(HousingProjectPlanner.Action.WAIT_PROJECT,decisions.stream().filter(r->r.household().equals(family)).findFirst().orElseThrow().action());
            assertEquals(HousingProjectPlanner.Action.PROPOSE,decisions.stream().filter(r->r.household().equals(other)).findFirst().orElseThrow().action());
            var duplicate=homeDraft(c,71);
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(new HousingProjects.Proposal(duplicate.id(),family,1200),context(1200),HousingProjectPlanner.prototype(),42,0));
            projects.bind(new HousingProjects.Proposal(duplicate.id(),other,1200),context(1200),HousingProjectPlanner.prototype(),42,0);
            assertEquals(2,number(c,"SELECT count(*) FROM housing_project_bindings"));
            assertEquals(0,number(c,"SELECT count(*) FROM construction_site_reservations"));
            assertEquals(3,number(c,"SELECT count(*) FROM residents"));assertEquals(0,HousingCapacity.read(c,town).get(family));healthy(c);
        }
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){assertEquals(saved,new HousingProjects(c).find(id(70)).orElseThrow());healthy(c);}
    }
    @Test void homeBindingRequiresCurrentNeedVacancyEvidenceCalendarWorldAndDryDraft()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);var draft=homeDraft(c,70);var proposal=new HousingProjects.Proposal(draft.id(),family,1200);var projects=new HousingProjects(c);
            new DemographicRepository(c).initializeClock(42,PopulationPolicy.prototype());
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0)); // already housed
            sql(c,"UPDATE household_capacity SET capacity=0");
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0)); // no survey
            var occupancy=new DwellingOccupancy(c);occupancy.review(id(19),building,assessment(1100),context(1100));
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0)); // reuse first
            verifiedShortage(c);
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,context(40_000),HousingProjectPlanner.prototype(),42,0));
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,new Context(world,false,Policy.prototype(),1200),HousingProjectPlanner.prototype(),42,0));
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,new Context(id(99),true,Policy.prototype(),1200),HousingProjectPlanner.prototype(),42,0));
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,1));
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(new HousingProjects.Proposal(draft.id(),other,1200),context(1200),HousingProjectPlanner.prototype(),42,0));
            new ConstructionSiteReservations(c).reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),world,1200,true,true),1200);
            assertThrows(BuildingRegistry.Declined.class,()->projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0));
            assertEquals(0,number(c,"SELECT count(*) FROM housing_project_bindings"));healthy(c);
        }
    }
    @Test void cancelledUnusedHomesRetainHistoryAndCannotResumeLandOrHoldAnotherFamily()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);verifiedShortage(c);var draft=homeDraft(c,70);var projects=new HousingProjects(c);
            var proposal=new HousingProjects.Proposal(draft.id(),family,1200);
            var saved=projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0);
            var sites=new ConstructionSiteReservations(c);
            sites.reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),world,1250,true,true),1250);
            assertThrows(BuildingRegistry.Declined.class,()->projects.cancel(id(80),draft.id(),1300,context(1300)));
            sites.releaseUnused(draft.id(),1300);
            var cancelled=projects.cancel(id(80),draft.id(),1400,context(1400));assertFalse(cancelled.active());
            assertEquals(cancelled,projects.cancel(id(80),draft.id(),1400,context(1500)));
            assertEquals(cancelled,projects.bind(proposal,context(1500),HousingProjectPlanner.prototype(),42,0));
            assertThrows(BuildingRegistry.Declined.class,()->projects.cancel(id(81),draft.id(),1400,context(1500)));
            assertThrows(SQLException.class,()->sites.reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),world,1500,true,true),1500));
            var next=homeDraft(c,71);projects.bind(new HousingProjects.Proposal(next.id(),family,1500),context(1500),HousingProjectPlanner.prototype(),42,0);
            projects.cancel(id(82),next.id(),1600,context(1600));
            assertThrows(SQLException.class,()->sites.reserve(next.id(),new ConstructionMaterials.Authority(next.id(),world,1600,true,true),1600)); // cancelled before ever reserving
            assertEquals(saved.planHash(),cancelled.planHash());assertEquals(2,number(c,"SELECT count(*) FROM housing_project_bindings"));healthy(c);
        }
    }
    @Test void schema113Preserves112HistoryAndHousingProjectFailuresRollBack()throws Exception {
        try(var c=open(112)){
            seed(c);sql(c,"CREATE TABLE housing_project_bindings(injected INTEGER)");
            assertThrows(SQLException.class,()->new SchemaMigrator().migrate(c));assertEquals(112,number(c,"PRAGMA user_version"));
            assertEquals(3,number(c,"SELECT count(*) FROM residents"));sql(c,"DROP TABLE housing_project_bindings");new SchemaMigrator().migrate(c);
            verifiedShortage(c);var draft=homeDraft(c,70);var projects=new HousingProjects(c);var proposal=new HousingProjects.Proposal(draft.id(),family,1200);
            sql(c,"CREATE TRIGGER fail_binding BEFORE INSERT ON housing_project_bindings BEGIN SELECT RAISE(ABORT,'injected'); END");
            assertThrows(SQLException.class,()->projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0));
            assertTrue(projects.find(draft.id()).isEmpty());assertTrue(c.getAutoCommit());sql(c,"DROP TRIGGER fail_binding");
            projects.bind(proposal,context(1200),HousingProjectPlanner.prototype(),42,0);
            sql(c,"CREATE TRIGGER fail_cancel BEFORE UPDATE ON housing_project_bindings BEGIN SELECT RAISE(ABORT,'injected'); END");
            assertThrows(SQLException.class,()->projects.cancel(id(80),draft.id(),1300,context(1300)));assertTrue(projects.find(draft.id()).orElseThrow().active());
            sql(c,"DROP TRIGGER fail_cancel");assertFalse(projects.cancel(id(80),draft.id(),1300,context(1300)).active());
            assertEquals(113,number(c,"PRAGMA user_version"));assertEquals(1,number(c,"SELECT count(*) FROM building_records"));healthy(c);
        }
    }

    @Test void approvedNewAdultHouseholdCanTakeInspectedVacancyWithoutChangingExistingFamilies()throws Exception{
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);new DemographicRepository(c).initializeClock(42,PopulationPolicy.prototype());
            new PopulationRepository(c).insertResident(new Resident(id(15),"Unassigned adult",town,null,240,Resident.Status.ALIVE));
            var housing=new DwellingHousing(c);var policy=HouseholdFormationPolicy.prototype();
            var result=housing.reconcile(layout(),assessment(1100),context(1100),policy,216,0);
            assertEquals(1,result.assigned());var formed=new PopulationRepository(c).findResident(id(15)).orElseThrow().householdId();
            assertNotNull(formed);assertEquals(2,HousingCapacity.read(c,town).get(formed));
            assertEquals(family,new PopulationRepository(c).findResident(id(10)).orElseThrow().householdId());
            assertEquals(1,number(c,"SELECT count(*) FROM household_formations"));
            housing.reconcile(layout(),assessment(1200),context(1200),policy,216,0);
            assertEquals(1,number(c,"SELECT count(*) FROM household_formations"));healthy(c);
        }
    }

    @Test void savedHousingPlanningUsesRealVacanciesAndHoldsUnknownOrStaleEvidenceWithoutWrites()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){
            seed(c);sql(c,"UPDATE household_capacity SET capacity=0 WHERE household_id='"+family+"'");
            var planning=new HousingPlanning(c);var planner=HousingProjectPlanner.prototype();
            var before=planning.review(town,context(1100),List.of(),planner,42,1);
            assertEquals(1,before.unknownHouseholds());assertFalse(before.vacanciesVerified());
            assertEquals(HousingProjectPlanner.Action.WAIT_SURVEY,before.plan().recommendations().getFirst().action());
            var occupancy=new DwellingOccupancy(c);occupancy.review(id(20),building,assessment(1100),context(1100));
            long writes=number(c,"SELECT total_changes()");
            var report=planning.review(town,context(1100),List.of(),planner,42,1);
            assertTrue(report.vacanciesVerified());assertEquals(1,report.unknownHouseholds());
            assertEquals(HousingProjectPlanner.Action.REUSE,report.plan().recommendations().getFirst().action());
            assertEquals(a,report.plan().recommendations().getFirst().vacantHome());
            assertEquals(report,planning.review(town,context(1100),List.of(),planner,42,1));
            assertEquals(writes,number(c,"SELECT total_changes()"));assertEquals(0,number(c,"SELECT count(*) FROM dwelling_active_occupancy"));
            var stale=planning.review(town,context(40_000),List.of(),planner,42,1);
            assertEquals(HousingProjectPlanner.Action.WAIT_SURVEY,stale.plan().recommendations().getFirst().action());
            var absent=planning.review(town,new Context(world,false,Policy.prototype(),1100),List.of(),planner,42,1);
            assertEquals(HousingProjectPlanner.Action.WAIT_WORLD,absent.plan().recommendations().getFirst().action());
            assertThrows(BuildingRegistry.Declined.class,()->planning.review(town,new Context(id(99),true,Policy.prototype(),1100),List.of(),planner,42,1));
            var template=com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
            var draft=new ConstructionDraft(id(70),town,world,p(100,64,100),Transform.identity(),template,1100);
            new ConstructionDraftRepository(c).save(draft);
            new ConstructionSiteReservations(c).reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),world,1100,true,true),1100);
            var pending=planning.review(town,context(1100),List.of(template),planner,42,1);
            assertTrue(pending.pendingHome());assertEquals(HousingProjectPlanner.Action.WAIT_PROJECT,pending.plan().recommendations().getFirst().action());
            healthy(c);
        }
    }

    @Test void wholeHouseholdAndExclusiveUnitSurviveReopenWithoutTouchingLegacyCapacity()throws Exception {
        var request=assign(id(21),family,a,id(20),null,1100);
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);assertEquals(Outcome.RECORDED,r.review(id(20),building,assessment(1100),context(1100)));
            assertEquals(request,r.assign(request,context(1100)));assertEquals(request,r.assign(request,context(1100)));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(22),other,a,id(20),null,1100),context(1100)));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(23),family,b,id(20),null,1100),context(1100)));
            assertEquals(List.of(b),r.vacancies(building,context(1100)).units().stream().map(UnitStatus::unit).toList());
            assertEquals(7,number(c,"SELECT capacity FROM household_capacity"));assertEquals(3,number(c,"SELECT count(*) FROM residents"));healthy(c);}
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){var r=new DwellingOccupancy(c);assertEquals(request,r.forHousehold(family).orElseThrow());assertEquals(request,r.forUnit(a).orElseThrow());healthy(c);}
    }
    @Test void oversizedFamilyEmptyHouseholdWrongVillageAbsentWorldAndStaleOrChangedPolicyAreRejected()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);r.review(id(20),building,assessment(1100),context(1100));
            var people=new PopulationRepository(c);people.insertResident(new Resident(id(13),"Fourth",town,family,120,Resident.Status.ALIVE));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),family,a,id(20),null,1100),context(1100)));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),other,a,id(20),null,40_000),context(40_000)));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),other,a,id(20),null,1100),new Context(world,false,Policy.prototype(),1100)));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),other,a,id(20),null,1100),new Context(id(99),true,Policy.prototype(),1100)));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),other,a,id(20),null,1100),new Context(world,true,new Policy(false),1100)));
            sql(c,"UPDATE residents SET status='DECEASED' WHERE household_id='"+other+"'");
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),other,a,id(20),null,1100),context(1100)));
            sql(c,"INSERT INTO settlements VALUES('"+id(90)+"','Foreign','fixture',0,64,0,1,0,0)");people.insertHousehold(new Household(id(91),id(90),"Foreign family"));people.insertResident(new Resident(id(92),"Foreign",id(90),id(91),240,Resident.Status.ALIVE));
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),id(91),a,id(20),null,1100),context(1100)));assertEquals(0,number(c,"SELECT count(*) FROM dwelling_active_occupancy"));healthy(c);}
    }
    @Test void incompleteOldAndReplayReviewsNeverEraseOrResurrectCurrentState()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);r.review(id(20),building,assessment(1100),context(1100));var assigned=assign(id(21),family,a,id(20),null,1100);r.assign(assigned,context(1100));
            assertEquals(Outcome.UNVERIFIED,r.review(id(22),building,assessment(1200,cells(),false,Policy.prototype()),context(1200)));
            assertEquals(id(20),r.unit(a).orElseThrow().review().operation());assertEquals(assigned,r.forUnit(a).orElseThrow());
            var changed=cells();changed.put(p(1,3,3),Cell.of(Type.PASSABLE));r.review(id(23),building,assessment(1300,changed,true,Policy.prototype()),context(1300));
            assertEquals(0,r.unit(a).orElseThrow().beds());assertEquals(2,r.unit(b).orElseThrow().beds());assertEquals(assigned,r.forUnit(a).orElseThrow());
            assertEquals(Outcome.REPLAY,r.review(id(20),building,assessment(1100),context(1100)));assertEquals(id(23),r.unit(a).orElseThrow().review().operation());
            assertEquals(Outcome.OLDER,r.review(id(24),building,assessment(1200),context(1300)));
            assertThrows(BuildingRegistry.Declined.class,()->r.review(id(20),building,assessment(1200),context(1200)));
            assertFalse(r.vacancies(building,context(40_000)).verified());healthy(c);}
    }
    @Test void atomicMovesRetainOldHomeOnSqlFailureAndOldAssignmentReplayDoesNotMoveBack()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);r.review(id(20),building,assessment(1100),context(1100));var first=assign(id(21),family,a,id(20),null,1100);r.assign(first,context(1100));
            var move=assign(id(22),family,b,id(20),first.operation(),1200);sql(c,"CREATE TRIGGER fail_move BEFORE INSERT ON dwelling_assignments WHEN NEW.operation_id='"+move.operation()+"' BEGIN SELECT RAISE(ABORT,'injected'); END");
            assertThrows(SQLException.class,()->r.assign(move,context(1200)));assertEquals(first,r.forHousehold(family).orElseThrow());assertEquals(0,number(c,"SELECT count(*) FROM dwelling_departures"));
            sql(c,"DROP TRIGGER fail_move");assertEquals(move,r.assign(move,context(1200)));assertTrue(r.forUnit(a).isEmpty());assertEquals(1,number(c,"SELECT count(*) FROM dwelling_departures"));
            assertEquals(first,r.assign(first,context(1100)));assertEquals(move,r.forHousehold(family).orElseThrow());healthy(c);}
    }
    @Test void reclaimAndOccupancyWithdrawalAreOneTransactionAndOldReclaimCannotEndLaterRedonation()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);var registry=new BuildingRegistry(c);r.review(id(20),building,assessment(1100),context(1100));var first=assign(id(21),family,a,id(20),null,1100);r.assign(first,context(1100));
            sql(c,"CREATE TRIGGER fail_departure BEFORE INSERT ON dwelling_departures BEGIN SELECT RAISE(ABORT,'injected'); END");
            var authority=BuildingRegistry.Authorization.playerDeclaration(donor,true);
            assertThrows(SQLException.class,()->registry.reclaim(id(22),building,authority,consent,1200));assertTrue(registry.hasDonationPermission(building));assertEquals(first,r.forUnit(a).orElseThrow());
            sql(c,"DROP TRIGGER fail_departure");registry.reclaim(id(22),building,authority,consent,1200);assertTrue(r.forUnit(a).isEmpty());assertFalse(registry.hasDonationPermission(building));
            assertEquals(1,number(c,"SELECT count(*) FROM dwelling_departures WHERE reason='RECLAIMED'"));
            registry.donate(id(23),layout(),donor,authority,id(22),1300);
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(24),family,a,id(20),null,1400),context(1400)));
            r.review(id(25),building,assessment(1400),context(1400));var second=assign(id(24),family,a,id(25),null,1400);r.assign(second,context(1400));
            registry.reclaim(id(22),building,authority,consent,1200);assertEquals(second,r.forUnit(a).orElseThrow());assertTrue(registry.hasDonationPermission(building));healthy(c);}
    }
    @Test void onlySavedDeathOfEveryHouseholdMemberFreesTheUnitAndOldDepartureReplayIsHarmless()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);r.review(id(20),building,assessment(1100),context(1100));var first=assign(id(21),family,a,id(20),null,1100);r.assign(first,context(1100));
            assertThrows(BuildingRegistry.Declined.class,()->r.releaseEmpty(id(22),first.operation(),context(1200)));
            sql(c,"UPDATE residents SET status='DECEASED' WHERE household_id='"+family+"'");var departure=r.releaseEmpty(id(22),first.operation(),context(1200));assertEquals("EMPTY",departure.reason());assertTrue(r.forUnit(a).isEmpty());
            r.assign(assign(id(23),other,a,id(20),null,1300),context(1300));assertEquals(departure,r.releaseEmpty(id(22),first.operation(),context(1200)));assertEquals(other,r.forUnit(a).orElseThrow().household());healthy(c);}
    }
    @Test void genuine108UpgradeIsPreservingAndFailed109RollsBackThenRecovers()throws Exception {
        try(var c=open(108)){seed(c);assertEquals(108,number(c,"PRAGMA user_version"));sql(c,"CREATE TABLE dwelling_review_units(injected INTEGER)");
            assertThrows(SQLException.class,()->new SchemaMigrator().migrate(c));assertEquals(108,number(c,"PRAGMA user_version"));
            assertEquals(0,number(c,"SELECT count(*) FROM sqlite_master WHERE name='dwelling_reviews'"));assertEquals(3,number(c,"SELECT count(*) FROM residents"));
            sql(c,"DROP TABLE dwelling_review_units");new SchemaMigrator().migrate(c);assertEquals(SchemaMigrator.CURRENT_VERSION,number(c,"PRAGMA user_version"));
            assertEquals(1,number(c,"SELECT count(*) FROM building_records"));assertEquals(7,number(c,"SELECT capacity FROM household_capacity"));healthy(c);}
    }

    @Test void sharedCapacityPreservesLegacyThenUsesAssignedHomeThroughDamageUnknownAndReclamation()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);assertEquals(Map.of(family,7),HousingCapacity.read(c));
            var r=new DwellingOccupancy(c);r.review(id(20),building,assessment(1100),context(1100));
            r.assign(assign(id(21),family,a,id(20),null,1100),context(1100));
            assertEquals(2,HousingCapacity.read(c).get(family));
            assertEquals(2,new HouseholdNeedsRepository(c).population(town).householdCapacity().get(family));
            sql(c,"UPDATE household_capacity SET capacity=99");assertEquals(2,HousingCapacity.read(c).get(family));
            var changed=cells();changed.put(p(1,3,3),Cell.of(Type.PASSABLE));r.review(id(22),building,assessment(1200,changed,true,Policy.prototype()),context(1200));
            assertEquals(0,HousingCapacity.read(c).get(family));
            r.review(id(23),building,assessment(1300,cells(),false,Policy.prototype()),context(1300));assertEquals(0,HousingCapacity.read(c).get(family));
            r.review(id(24),building,assessment(1400),context(1400));assertEquals(2,HousingCapacity.read(c).get(family));
            new BuildingRegistry(c).reclaim(id(25),building,BuildingRegistry.Authorization.playerDeclaration(donor,true),consent,1500);
            assertEquals(0,HousingCapacity.read(c).get(family));assertEquals(3,number(c,"SELECT count(*) FROM residents"));healthy(c);
        }
    }

    @Test void otherLivingLegacyFamilyIsProtectedAndKnownEmptyBedSourcesAreNotDoubleCounted()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);r.review(id(20),building,assessment(1100),context(1100));
            sql(c,"INSERT INTO village_beds VALUES('"+world+"',2,1,3,'"+town+"','"+other+"')");
            assertThrows(BuildingRegistry.Declined.class,()->r.assign(assign(id(21),family,a,id(20),null,1100),context(1100)));
            assertEquals(0,number(c,"SELECT count(*) FROM dwelling_bed_claims"));
            sql(c,"UPDATE residents SET status='DECEASED' WHERE household_id='"+other+"'");
            sql(c,"INSERT INTO household_capacity VALUES('"+other+"',4)");
            sql(c,"INSERT INTO household_capacity_sources VALUES('"+world+"',2,1,3,'"+other+"',2),('"+world+"',20,1,3,'"+other+"',2)");
            r.assign(assign(id(21),family,a,id(20),null,1100),context(1100));
            assertEquals(2,HousingCapacity.read(c).get(family));assertEquals(2,HousingCapacity.read(c).get(other));
            sql(c,"DELETE FROM household_capacity_sources");assertFalse(HousingCapacity.read(c).containsKey(other));healthy(c);
        }
    }

    @Test void schema110BackfillsExistingAssignmentsAndRollsBackNewTablesOnFailure()throws Exception {
        try(var c=open(109)){seed(c);sql(c,"INSERT INTO dwelling_reviews VALUES('"+id(20)+"','"+building+"','"+consent+"','"+"0".repeat(64)+"',1100,1100,1)");
            sql(c,"INSERT INTO dwelling_review_units VALUES('"+id(20)+"','"+a+"',2,'')");
            sql(c,"INSERT INTO dwelling_current_reviews VALUES('"+building+"','"+id(20)+"')");
            sql(c,"INSERT INTO dwelling_assignments VALUES('"+id(21)+"','"+family+"','"+a+"','"+id(20)+"',NULL,1100)");
            sql(c,"INSERT INTO dwelling_active_occupancy VALUES('"+family+"','"+a+"','"+id(21)+"')");
            sql(c,"CREATE TABLE dwelling_bed_claims(injected INTEGER)");
            assertThrows(SQLException.class,()->new SchemaMigrator().migrate(c));assertEquals(109,number(c,"PRAGMA user_version"));
            assertEquals(0,number(c,"SELECT count(*) FROM sqlite_master WHERE name='household_capacity_sources'"));
            sql(c,"DROP TABLE dwelling_bed_claims");new SchemaMigrator().migrate(c);
            assertEquals(2,number(c,"SELECT count(*) FROM dwelling_bed_claims"));assertEquals(2,HousingCapacity.read(c).get(family));
            assertEquals(7,number(c,"SELECT capacity FROM household_capacity"));healthy(c);
        }
    }

    @Test void housingReconciliationUsesVacancyForOvercrowdedFamilyThenMovesItAfterConfirmedDamage()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);sql(c,"UPDATE household_capacity SET capacity=1");
            var housing=new DwellingHousing(c);var first=housing.reconcile(layout(),assessment(1100),context(1100));
            assertEquals(1,first.assigned());var occupancy=new DwellingOccupancy(c);
            assertEquals(a,occupancy.forHousehold(family).orElseThrow().unit());assertEquals(2,HousingCapacity.read(c).get(family));
            assertTrue(housing.summary(layout()).stream().anyMatch(s->s.contains("2 living residents; saved effective capacity 2")));
            assertEquals(0,housing.reconcile(layout(),assessment(1200),context(1200)).assigned());
            var changed=cells();changed.put(p(1,3,3),Cell.of(Type.PASSABLE));
            assertEquals(1,housing.reconcile(layout(),assessment(1300,changed,true,Policy.prototype()),context(1300)).assigned());
            assertEquals(b,occupancy.forHousehold(family).orElseThrow().unit());assertTrue(occupancy.forUnit(a).isEmpty());
            assertEquals(2,HousingCapacity.read(c).get(family));assertEquals(3,number(c,"SELECT count(*) FROM residents"));healthy(c);
        }
    }

    @Test void housingReconciliationUpgradesIncumbentWithoutMovingOrSplittingOtherLegacyFamilies()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);sql(c,"INSERT INTO village_beds VALUES('"+world+"',2,1,3,'"+town+"','"+family+"')");
            var housing=new DwellingHousing(c);assertEquals(1,housing.reconcile(layout(),assessment(1100),context(1100)).assigned());
            assertEquals(a,new DwellingOccupancy(c).forHousehold(family).orElseThrow().unit());
        }
    }

    @Test void reclamationConfirmationRejectsChangedOccupantsThenRemovesCapacityWithoutLegacyFallback()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var r=new DwellingOccupancy(c);r.review(id(20),building,assessment(1100),context(1100));
            var first=assign(id(21),family,a,id(20),null,1100);r.assign(first,context(1100));
            var requests=new com.blueninjas24.civilizations.application.BuildingConsentRequests(new BuildingRegistry(c),com.blueninjas24.civilizations.application.BuildingConsentRequests.Policy.prototype());
            var authority=new com.blueninjas24.civilizations.application.BuildingConsentRequests.Context(donor,world,false,true,false,Set.of(world));
            var preview=requests.reclamation(authority,building,1200);assertEquals(Set.of(first.operation()),preview.housingAssignments());
            r.assign(assign(id(22),family,b,id(20),first.operation(),1250),context(1250));
            assertThrows(BuildingRegistry.Declined.class,()->requests.confirm(authority,preview.token(),false,1300));
            assertTrue(new BuildingRegistry(c).hasDonationPermission(building));
            var current=requests.reclamation(authority,building,1350);requests.confirm(authority,current.token(),false,1400);
            assertEquals(0,HousingCapacity.read(c).get(family));assertEquals(3,number(c,"SELECT count(*) FROM residents"));healthy(c);
        }
    }

    @Test void reclaimedUnassignedHomeWithdrawsOnlyItsProvenLegacyBedContribution()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);
            sql(c,"INSERT INTO village_beds VALUES('"+world+"',2,1,3,'"+town+"','"+family+"')");
            sql(c,"INSERT INTO household_capacity_sources VALUES('"+world+"',2,1,3,'"+family+"',2),('"+world+"',20,1,3,'"+family+"',5)");
            var registry=new BuildingRegistry(c);assertEquals(Set.of(family),registry.legacyHousingFamilies(building));
            registry.reclaim(id(21),building,BuildingRegistry.Authorization.playerDeclaration(donor,true),consent,1200);
            assertEquals(5,HousingCapacity.read(c).get(family));assertEquals(4,number(c,"SELECT count(*) FROM dwelling_bed_claims"));
            assertEquals(3,number(c,"SELECT count(*) FROM residents"));healthy(c);
        }
    }

    @Test void committedReclamationWithLostCommitAcknowledgementReturnsSavedReceiptOnRetry()throws Exception {
        try(var c=open(SchemaMigrator.CURRENT_VERSION)){seed(c);var occupancy=new DwellingOccupancy(c);occupancy.review(id(20),building,assessment(1100),context(1100));
            occupancy.assign(assign(id(21),family,a,id(20),null,1100),context(1100));
            var loseAck=new java.util.concurrent.atomic.AtomicBoolean(true);
            var proxy=(Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,args)->{
                if(m.getName().equals("commit")&&loseAck.getAndSet(false)){c.commit();throw new SQLException("Injected lost commit acknowledgement");}
                try{return m.invoke(c,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            });
            var requests=new com.blueninjas24.civilizations.application.BuildingConsentRequests(new BuildingRegistry(proxy),com.blueninjas24.civilizations.application.BuildingConsentRequests.Policy.prototype());
            var authority=new com.blueninjas24.civilizations.application.BuildingConsentRequests.Context(donor,world,false,true,false,Set.of(world));
            var preview=requests.reclamation(authority,building,1200);
            assertThrows(SQLException.class,()->requests.confirm(authority,preview.token(),false,1300));
            assertFalse(new BuildingRegistry(c).hasDonationPermission(building));assertTrue(occupancy.forUnit(a).isEmpty());
            var receipt=requests.confirm(authority,preview.token(),false,1400);assertEquals(preview.token(),receipt.operation());
            assertEquals(1300,receipt.atMillis());assertEquals(1,number(c,"SELECT count(*) FROM dwelling_departures"));healthy(c);
        }
    }
}
