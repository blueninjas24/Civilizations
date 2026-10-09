package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MiningStartupRepositoryTest {
    @TempDir Path directory;
    DatabaseManager db;MiningStartupRepository startup;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),worker=UUID.randomUUID();
    MiningStartupRepository.Request request;
    void open() throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("MiningStartupTest"));db.initialize();startup=new MiningStartupRepository(db.getConnection());}
    @BeforeEach void prepare() throws Exception {
        open();db.saveSettlement(new Settlement(town,"Test","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(worker,"Miner",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Test opening capital");
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,10,"Complete explicit test patch");
        var business=new Business(UUID.randomUUID(),town,"Mine",worker,worker,"mining",site.id(),2,0);
        var req=new JobRequirements(216,Map.of());
        request=new MiningStartupRepository.Request(new BusinessFounding(business,site,req,req,500,"Funded test opportunity"),
                new MineralObservation(world,10,true,Commodity.COAL,List.of(new VillageObservation.Position(0,40,0),new VillageObservation.Position(1,40,0))),
                4,Map.of(Commodity.COAL,1L,Commodity.STONE,2L),100,Map.of(Commodity.COAL,1L,Commodity.STONE,1L),1000,worker);
    }
    @AfterEach void close(){db.close();}
    void sql(String text) throws Exception {try(var s=db.getConnection().createStatement()){s.execute(text);}}
    long count(String table) throws Exception {try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    long treasury() throws Exception {return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    void unchanged() throws Exception {
        for(var table:List.of("businesses","business_sites","mineral_deposits","mineral_evidence","industry_stores","industry_stock","business_employment"))assertEquals(0,count(table),table);
        assertEquals(10000,treasury());assertEquals(1,count("treasury_entries"));assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void fundedStartupCommitsAllPartsAndReplaysAfterExtractionAndRestartWithoutRefill() throws Exception {
        var started=startup.start(request);assertTrue(started.discovery().created());assertEquals(8,started.discovery().deposit().initialBatches());
        assertEquals(8500,treasury());assertEquals(1000,started.store().hundredths());assertEquals(worker,started.firstEmployment().residentId());
        assertTrue(started.store().units().values().stream().allMatch(n->n==0));
        var id=started.founding().business().id();
        db.getIndustryRepository().mine(UUID.randomUUID(),id,started.discovery().deposit().id(),2,0,8,8);
        db.close();open();var replay=startup.start(request);
        assertFalse(replay.discovery().created());assertEquals(started.discovery().deposit().id(),replay.discovery().deposit().id());
        assertEquals(6,replay.discovery().deposit().remainingBatches());assertEquals(2,replay.store().units().get(Commodity.COAL));
        assertEquals(8500,treasury());assertEquals(3,count("treasury_entries"));assertEquals(1,count("business_employment"));
    }
    @Test void storeFailureRollsBackBusinessDepositStockAndBothFundingDebitsThenRetries() throws Exception {
        sql("CREATE TRIGGER fail_store BEFORE INSERT ON industry_stores BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->startup.start(request));unchanged();
        sql("DROP TRIGGER fail_store");assertTrue(startup.start(request).discovery().created());assertEquals(8500,treasury());
    }
    @Test void lastStepEmploymentFailureRollsBackEveryEarlierPart() throws Exception {
        sql("CREATE TRIGGER fail_hire BEFORE INSERT ON business_employment BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->startup.start(request));unchanged();
        sql("DROP TRIGGER fail_hire");assertNotNull(startup.start(request).firstEmployment());
    }
    @Test void combinedCapitalRefusalCannotLeavePartiallyFundedBusiness() throws Exception {
        var expensive=new MiningStartupRepository.Request(request.founding(),request.evidence(),4,request.bundle(),100,request.spacePerUnit(),10000,worker);
        assertThrows(SQLException.class,()->startup.start(expensive));unchanged();
    }
    @Test void staleExcludedPartialAndWrongWorldEvidenceCannotEstablishEvenBusinessIdentity() throws Exception {
        new OrePlacementRepository(db.getConnection()).record(world,List.of(request.evidence().ore().getFirst()),10);
        assertThrows(SQLException.class,()->startup.start(request));unchanged();
        var partial=new MineralObservation(world,10,false,Commodity.COAL,request.evidence().ore());
        assertThrows(SQLException.class,()->startup.start(new MiningStartupRepository.Request(request.founding(),partial,4,request.bundle(),100,request.spacePerUnit(),1000,worker)));unchanged();
        var wrong=new MineralObservation(UUID.randomUUID(),10,true,Commodity.COAL,request.evidence().ore());
        assertThrows(SQLException.class,()->startup.start(new MiningStartupRepository.Request(request.founding(),wrong,4,request.bundle(),100,request.spacePerUnit(),1000,worker)));unchanged();
    }
    @Test void transactionEntryPointsCannotRunWithTheWrongOwnerTransactionState() throws Exception {
        var businesses=db.getBusinessRepository();var industry=db.getIndustryRepository();
        assertThrows(SQLException.class,()->businesses.foundRows(request.founding()));
        assertThrows(SQLException.class,()->businesses.hireRows(UUID.randomUUID(),request.founding().business().id(),worker,0));
        assertThrows(SQLException.class,()->industry.openStoreRows(request.founding().business().id(),100,request.spacePerUnit(),1000));
        db.getConnection().setAutoCommit(false);assertThrows(SQLException.class,()->startup.start(request));
        assertThrows(SQLException.class,()->businesses.found(request.founding()));
        db.getConnection().rollback();db.getConnection().setAutoCommit(true);unchanged();
    }
    @Test void storageMustSupportEveryByproductAndFailedQualificationLeavesNothing() throws Exception {
        assertThrows(IllegalArgumentException.class,()->new MiningStartupRepository.Request(request.founding(),request.evidence(),4,request.bundle(),100,Map.of(Commodity.COAL,1L),1000,worker));
        db.getPopulationRepository().updateResident(new Resident(worker,"Miner",town,null,300,Resident.Status.DECEASED));
        assertThrows(SQLException.class,()->startup.start(request));unchanged();
    }
    @Test void startupReplayCannotSilentlyChooseADifferentFirstWorker() throws Exception {
        var saved=startup.start(request);var other=UUID.randomUUID();
        db.getPopulationRepository().insertResident(new Resident(other,"Other miner",town,null,300,Resident.Status.ALIVE));
        var changed=new MiningStartupRepository.Request(request.founding(),request.evidence(),4,request.bundle(),100,request.spacePerUnit(),1000,other);
        assertThrows(SQLException.class,()->startup.start(changed));
        assertEquals(8500,treasury());assertEquals(1,count("business_employment"));
        assertEquals(saved.firstEmployment(),db.getBusinessRepository().employmentHistory(request.founding().business().id()).getFirst());
    }
    @Test void competingPrimaryJobCannotLeaveANewMineOrSpendItsFunding() throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,10,40,0,10,"Existing workplace");
        var b=new Business(UUID.randomUUID(),town,"Existing farm",worker,worker,"farming",site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(b,site,requirements,requirements,0,"Existing job"));
        db.getBusinessRepository().hire(UUID.randomUUID(),b.id(),worker,0);
        assertThrows(SQLException.class,()->startup.start(request));
        assertEquals(1,count("businesses"));assertEquals(1,count("business_sites"));assertEquals(1,count("business_employment"));
        assertEquals(0,count("mineral_deposits"));assertEquals(0,count("industry_stores"));assertEquals(10000,treasury());
    }
}
