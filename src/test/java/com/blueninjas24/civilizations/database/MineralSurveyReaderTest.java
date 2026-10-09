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

class MineralSurveyReaderTest {
    @TempDir Path directory;
    DatabaseManager db;MineralSurveyReader reader;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID();
    MineralObservation coal(boolean complete,int...xs){return new MineralObservation(world,10,complete,Commodity.COAL,Arrays.stream(xs).mapToObj(x->new VillageObservation.Position(x,40,0)).toList());}
    List<MineralObservation> pair(MineralObservation coal){return List.of(coal,new MineralObservation(world,10,true,Commodity.IRON,List.of()));}
    @BeforeEach void open() throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("SurveyTest"));db.initialize();reader=new MineralSurveyReader(db.getConnection());
        db.saveSettlement(new Settlement(town,"Survey town","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));
    }
    @AfterEach void close(){db.close();}
    Business mine(int x) throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,40,0,10,"Explicit test site");
        var b=new Business(UUID.randomUUID(),town,"Mine",person,person,"mining",site.id(),4,0);
        var requirements=new JobRequirements(216,Map.of());db.getBusinessRepository().found(new BusinessFounding(b,site,requirements,requirements,0,"Test"));return b;
    }
    @Test void excludedBridgeSplitsPositiveEvidenceWithoutCreatingBusinessReservesOrFunds() throws Exception {
        new OrePlacementRepository(db.getConnection()).record(world,List.of(new VillageObservation.Position(1,40,0)),1);
        var result=reader.read(town,pair(coal(true,0,1,2)));
        assertEquals(2,result.size());assertTrue(result.stream().allMatch(p->p.savedDeposit()==null));
        assertThrows(UnsupportedOperationException.class,result::clear);
        assertTrue(db.getBusinessRepository().findBusinesses(town).isEmpty());
        assertTrue(new TreasuryRepository(db.getConnection()).load(town).isEmpty());
        assertTrue(new MineralDiscoveryRepository(db.getConnection()).findAt(world,new VillageObservation.Position(0,40,0)).isEmpty());
    }
    @Test void splitObservedPatchesRetainOneExistingDepletedIdentity() throws Exception {
        var b=mine(0);var discovery=new MineralDiscoveryRepository(db.getConnection());
        var saved=discovery.discover(b.siteId(),coal(true,0,1,2),2,Map.of(Commodity.COAL,1L));
        db.getBusinessRepository().hire(UUID.randomUUID(),b.id(),person,0);
        db.getIndustryRepository().openStore(b.id(),100,Map.of(Commodity.COAL,1L),0);
        db.getIndustryRepository().mine(UUID.randomUUID(),b.id(),saved.deposit().id(),6,0,8,8);
        var result=reader.read(town,pair(coal(true,0,2)));
        assertEquals(2,result.size());assertTrue(result.stream().allMatch(p->p.savedDeposit().equals(saved.deposit().id())));
        assertEquals(0,db.getIndustryRepository().deposit(saved.deposit().id()).orElseThrow().remainingBatches());
    }
    @Test void conflictingDepositAndMineralEvidenceDoesNotBecomeAnOpportunity() throws Exception {
        var a=mine(0);var b=mine(2);var discovery=new MineralDiscoveryRepository(db.getConnection());
        discovery.discover(a.siteId(),coal(true,0),2,Map.of(Commodity.COAL,1L));
        discovery.discover(b.siteId(),coal(true,2),2,Map.of(Commodity.COAL,1L));
        assertThrows(SQLException.class,()->reader.read(town,pair(coal(true,0,1,2))));
        var iron=new MineralObservation(world,10,true,Commodity.IRON,coal(true,0).ore());
        assertThrows(SQLException.class,()->reader.read(town,List.of(coal(true),iron)));
    }
    @Test void absentSettlementWrongWorldPartialEvidenceAndCaptureGapAreRejected() throws Exception {
        assertThrows(SQLException.class,()->reader.read(UUID.randomUUID(),pair(coal(true,0))));
        assertThrows(SQLException.class,()->reader.read(town,pair(coal(false,0))));
        var wrong=new MineralObservation(UUID.randomUUID(),10,true,Commodity.IRON,List.of());
        assertThrows(SQLException.class,()->reader.read(town,List.of(coal(true,0),wrong)));
        new OreTrackingRepository(db.getConnection()).begin(10);new OreTrackingRepository(db.getConnection()).markGap(11);
        assertThrows(SQLException.class,()->reader.read(town,pair(coal(true,0))));
    }
    @Test void duplicateKindsInconsistentTimesAndCombinedOverflowAreRejected() throws Exception {
        var iron=new MineralObservation(world,10,true,Commodity.IRON,coal(true,0).ore());
        assertThrows(SQLException.class,()->reader.read(town,List.of(coal(true,0),iron)));
        var later=new MineralObservation(world,11,true,Commodity.IRON,List.of());
        assertThrows(SQLException.class,()->reader.read(town,List.of(coal(true,0),later)));
        var full=coal(true,java.util.stream.IntStream.range(0,512).toArray());
        var extra=new MineralObservation(world,10,true,Commodity.IRON,coal(true,512).ore());
        assertThrows(SQLException.class,()->reader.read(town,List.of(full,extra)));
        assertThrows(IllegalArgumentException.class,()->reader.read(town,List.of(coal(true))));
        assertTrue(reader.read(town,pair(coal(true))).isEmpty());
    }
}
