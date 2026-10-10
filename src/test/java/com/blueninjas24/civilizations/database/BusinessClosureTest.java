package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.BusinessDiagnostics;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class BusinessClosureTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID(),worker=UUID.randomUUID(),buyer=UUID.randomUUID(),deposit=UUID.randomUUID();
    Business mine,warehouse;UUID job;BusinessFounding founding;
    final Map<Commodity,Long> space=Map.of(Commodity.COAL,1L,Commodity.STONE,1L);
    void open()throws SQLException {db=new DatabaseManager(directory.toFile(),Logger.getLogger("ClosureTest"));db.initialize();}
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Closure Fixture","World",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"World");db.getVillageIdentityRepository().bindSettlement(town,world);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Test-owned starting capital");
        for(var id:List.of(owner,worker,buyer)) {
            db.getPopulationRepository().insertResident(new Resident(id,"Fixture",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(id,"mining",id.equals(worker)?5:20);
        }
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,0,"Test-owned site");
        mine=new Business(UUID.randomUUID(),town,"Mine",owner,owner,"mining",site.id(),2,0);
        founding=new BusinessFounding(mine,site,new JobRequirements(216,Map.of("mining",10L)),new JobRequirements(216,Map.of("mining",1L)),0,"Test opportunity");
        db.getBusinessRepository().found(founding);job=UUID.randomUUID();db.getBusinessRepository().hire(job,mine.id(),worker,0);
        var otherSite=new BusinessSite(UUID.randomUUID(),town,world,10,40,0,0,"Test warehouse");
        warehouse=new Business(UUID.randomUUID(),town,"Warehouse",buyer,buyer,"warehouse",otherSite.id(),1,0);
        var none=new JobRequirements(216,Map.of());db.getBusinessRepository().found(new BusinessFounding(warehouse,otherSite,none,none,0,"Test warehouse"));
        var industry=db.getIndustryRepository();industry.openStore(mine.id(),100,space,300);industry.openStore(warehouse.id(),100,space,1000);
        industry.discover(deposit,mine.siteId(),1,10,Map.of(Commodity.COAL,1L));industry.mine(UUID.randomUUID(),mine.id(),deposit,1,1,1,1);
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture",town,null,300,Resident.Status.DECEASED));
    }
    @AfterEach void close(){db.close();}
    void sql(String query)throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(query);}}
    BusinessSaleListings offers(){return new BusinessSaleListings(db.getConnection());}
    BusinessClosure closures(){return new BusinessClosure(db.getConnection());}
    void expired()throws SQLException {offers().open(mine.id(),200,1000,1000,1);offers().observe(2000,Set.of(world));}
    @Test void expiryClosesAtomicallyRetainsAssetsHistoryAndNeverResurrectsJobsAfterRestart()throws Exception {
        expired();var stock=db.getIndustryRepository().store(mine.id()).orElseThrow();var reserve=db.getIndustryRepository().deposit(deposit).orElseThrow();
        var saved=closures().closeIfExpired(mine.id(),2).orElseThrow();assertEquals(owner,saved.seller());assertEquals(2,saved.step());
        assertEquals(stock,db.getIndustryRepository().store(mine.id()).orElseThrow());assertEquals(reserve,db.getIndustryRepository().deposit(deposit).orElseThrow());
        assertEquals(founding,db.getBusinessRepository().findFounding(mine.id()).orElseThrow());assertEquals(mine,db.getBusinessRepository().findBusiness(mine.id()).orElseThrow());
        assertEquals(2,db.getBusinessRepository().findEmployment(job).orElseThrow().endedStep());assertTrue(db.getBusinessRepository().currentWorkers(mine.id()).isEmpty());
        assertFalse(new BusinessOperations(db.getConnection(),BusinessOperationPolicy.disabled()).evaluate(mine.id()).permitted());
        db.close();open();assertEquals(saved,closures().closeIfExpired(mine.id(),99).orElseThrow());
        assertEquals(2,db.getBusinessRepository().hire(job,mine.id(),worker,0).endedStep()); // historical replay only
        assertThrows(SQLException.class,()->db.getBusinessRepository().hire(UUID.randomUUID(),mine.id(),worker,3));
        assertThrows(SQLException.class,()->db.getIndustryRepository().mine(UUID.randomUUID(),mine.id(),deposit,1,2,1,1));
        assertEquals(BusinessSaleListings.Status.EXPIRED,offers().open(mine.id(),200,1000,3000,3).status()); // immutable offer replay
        assertThrows(SQLException.class,()->offers().open(mine.id(),201,1000,3000,3));
        db.getPopulationRepository().recordSkillExperience(worker,"mining",20);
        assertTrue(new BusinessSuccession(db.getConnection()).candidate(mine.id()).isEmpty());
        assertTrue(new BusinessSuccession(db.getConnection()).succeed(UUID.randomUUID(),mine.id(),3).isEmpty());
        var snapshot=new BusinessRuntime(db.getConnection(),8,32).refresh(3,Set.of(world));
        assertTrue(BusinessDiagnostics.describe(snapshot,town).stream().anyMatch(s->s.contains("Closed at administrative month 2")));
        assertFalse(BusinessDiagnostics.describe(snapshot,town).stream().anyMatch(s->s.contains("succession not implemented")));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void openOrAbsentOfferDoesNotCloseAndQualifiedInheritanceWinsEvenAtExpiry()throws Exception {
        offers().open(mine.id(),200,1000,1000,1);assertTrue(closures().closeIfExpired(mine.id(),2).isEmpty());
        offers().observe(100000,Set.of());assertTrue(closures().closeIfExpired(mine.id(),2).isEmpty());
        offers().observe(101000,Set.of(world));db.getPopulationRepository().recordSkillExperience(worker,"mining",20);
        assertTrue(closures().closeIfExpired(mine.id(),2).isEmpty());
        new BusinessRuntime(db.getConnection(),8,32).refresh(2,Set.of(world));
        assertEquals(worker,db.getBusinessRepository().findBusiness(mine.id()).orElseThrow().ownerId());assertTrue(closures().find(mine.id()).isEmpty());
    }
    @Test void staffingFailureRollsBackClosureAndAllJobsWithSafeRetry()throws Exception {
        expired();sql("CREATE TRIGGER prevent_closure_job BEFORE UPDATE ON business_employment BEGIN SELECT RAISE(ABORT,'test job save failure'); END");
        assertThrows(SQLException.class,()->closures().closeIfExpired(mine.id(),2));assertTrue(closures().find(mine.id()).isEmpty());assertNull(db.getBusinessRepository().findEmployment(job).orElseThrow().endedStep());
        sql("DROP TRIGGER prevent_closure_job");assertTrue(closures().closeIfExpired(mine.id(),2).isPresent());assertEquals(1,db.getBusinessRepository().employmentHistory(mine.id()).size());
    }
    @Test void futureJobOrOfferDatesCannotCreateInventedPastClosure()throws Exception {
        offers().open(mine.id(),200,1000,1000,4);offers().observe(2000,Set.of(world));
        assertThrows(SQLException.class,()->closures().closeIfExpired(mine.id(),3));assertTrue(closures().find(mine.id()).isEmpty());
        sql("UPDATE business_employment SET started_step=5 WHERE business_id='"+mine.id()+"'");
        assertThrows(SQLException.class,()->closures().closeIfExpired(mine.id(),4));assertTrue(closures().find(mine.id()).isEmpty());
        assertTrue(closures().closeIfExpired(mine.id(),5).isPresent());
    }
    @Test void paidCargoStillArrivesAndAcceptedReplaySurvivesButClosedFirmCannotTradeAgain()throws Exception {
        var operation=UUID.randomUUID();var industry=db.getIndustryRepository();var shipment=industry.purchase(operation,mine.id(),warehouse.id(),Commodity.COAL,1,100,1,1,10);
        expired();closures().closeIfExpired(mine.id(),2);assertEquals(shipment,industry.purchase(operation,mine.id(),warehouse.id(),Commodity.COAL,1,100,1,1,10));
        assertThrows(SQLException.class,()->industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),Commodity.COAL,1,100,2,1,10));
        assertThrows(SQLException.class,()->industry.purchase(UUID.randomUUID(),warehouse.id(),mine.id(),Commodity.COAL,1,100,2,1,10));
        industry.deliver(operation,2);assertEquals(1,industry.store(warehouse.id()).orElseThrow().units().get(Commodity.COAL));
        assertEquals(400,industry.store(mine.id()).orElseThrow().hundredths());assertEquals(900,industry.store(warehouse.id()).orElseThrow().hundredths());
        assertTrue(new BusinessSales(db.getConnection(),BusinessPurchasePolicy.prototype()).estate(owner).isEmpty());
    }
    @Test void boundedRuntimeClosesOnlyPresentWorldAndExposesRetainedReason()throws Exception {
        expired();var runtime=new BusinessRuntime(db.getConnection(),8,32);runtime.refresh(2,Set.of());assertTrue(closures().find(mine.id()).isEmpty());
        var snapshot=runtime.refresh(2,Set.of(world));assertTrue(closures().find(mine.id()).isPresent());assertTrue(snapshot.businesses().get(mine.id()).lifecycle().startsWith("Closed"));
        assertEquals(0,snapshot.businesses().get(mine.id()).openAssignments());assertEquals(0,snapshot.businesses().get(mine.id()).workers());
    }
    @Test void expiredOfferStopsWorkBeforeClosurePageAndCannotReceiveNewCapital()throws Exception {
        expired();assertTrue(closures().find(mine.id()).isEmpty());
        assertFalse(new BusinessOperations(db.getConnection(),BusinessOperationPolicy.prototype()).evaluate(mine.id()).permitted());
        assertThrows(SQLException.class,()->db.getIndustryRepository().mine(UUID.randomUUID(),mine.id(),deposit,1,2,1,1));
        assertThrows(SQLException.class,()->db.getBusinessRepository().hire(UUID.randomUUID(),mine.id(),buyer,2));
        var site=new BusinessSite(UUID.randomUUID(),town,world,30,40,0,0,"Test closed site without store");
        var other=new Business(UUID.randomUUID(),town,"Unstaffed Mine",owner,owner,"mining",site.id(),1,0);
        // Found while alive, then restore the actual deceased seller before listing.
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture",town,null,300,Resident.Status.ALIVE));
        db.getBusinessRepository().found(new BusinessFounding(other,site,founding.founderRequirements(),founding.workerRequirements(),0,"Test opportunity"));
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture",town,null,300,Resident.Status.DECEASED));
        offers().open(other.id(),200,1000,1000,1);offers().observe(2000,Set.of(world));closures().closeIfExpired(other.id(),2);
        long cash=new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();
        assertThrows(SQLException.class,()->db.getIndustryRepository().openStore(other.id(),100,space,100));
        assertEquals(cash,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());assertTrue(db.getIndustryRepository().store(other.id()).isEmpty());
    }
    @Test void closedWorkshopCannotProcessAndClosedSaleCannotCharge()throws Exception {
        expired();closures().closeIfExpired(mine.id(),2);var recipe=new ProductionRecipe("test","mining",Map.of(Commodity.COAL,1L),Map.of(Commodity.STONE,1L),1,1);
        assertThrows(SQLException.class,()->db.getIndustryRepository().process(UUID.randomUUID(),mine.id(),recipe,1,2));
        var sale=new BusinessSales.Request(UUID.randomUUID(),mine.id(),owner,buyer,2,warehouse.id(),200,null,0);
        assertThrows(SQLException.class,()->new BusinessSales(db.getConnection(),BusinessPurchasePolicy.prototype()).sell(sale));
        assertEquals(1000,db.getIndustryRepository().store(warehouse.id()).orElseThrow().hundredths());
    }
    @Test void failedSchema40UpgradeRollsBackAndRetryRetainsOfferStockAndEmployment()throws Exception {
        expired();var before=offers().find(mine.id(),owner).orElseThrow();SchemaFixtures.rewind(db.getConnection(),40);
        sql("CREATE INDEX business_closure_history ON businesses(id)");db.close();
        var failed=new DatabaseManager(directory.toFile(),Logger.getLogger("ClosureMigration"));assertThrows(SQLException.class,failed::initialize);failed.close();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"));var s=c.createStatement()) {
            try(var r=s.executeQuery("PRAGMA user_version")){assertEquals(40,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='business_closures'")){assertEquals(0,r.getInt(1));}
            s.execute("DROP INDEX business_closure_history");
        }
        open();assertEquals(before,offers().find(mine.id(),owner).orElseThrow());assertNull(db.getBusinessRepository().findEmployment(job).orElseThrow().endedStep());
        assertEquals(1,db.getIndustryRepository().store(mine.id()).orElseThrow().units().get(Commodity.COAL));assertTrue(closures().closeIfExpired(mine.id(),2).isPresent());
    }
}
