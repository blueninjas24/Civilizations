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
