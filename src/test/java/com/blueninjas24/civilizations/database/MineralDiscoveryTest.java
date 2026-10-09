package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class MineralDiscoveryTest {
    @TempDir Path directory;
    DatabaseManager db;MineralDiscoveryRepository discovery;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID();
    Business mine;
    static final Map<Commodity,Long> BUNDLE=Map.of(Commodity.COAL,1L,Commodity.STONE,3L);
    void open() throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("DiscoveryTest"));db.initialize();discovery=new MineralDiscoveryRepository(db.getConnection());}
    @BeforeEach void setup() throws Exception {
        open();db.saveSettlement(new Settlement(town,"Test","World",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"World");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Miner",town,null,300,Resident.Status.ALIVE));mine=business(town,world,person,0);
        db.getBusinessRepository().hire(UUID.randomUUID(),mine.id(),person,0);
        db.getIndustryRepository().openStore(mine.id(),1024,Map.of(Commodity.COAL,1L,Commodity.STONE,1L),0);
    }
    @AfterEach void close(){db.close();}
    Business business(UUID settlement,UUID dimension,UUID founder,int x) throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),settlement,dimension,x,64,0,1,"Trusted test workplace");
        var b=new Business(UUID.randomUUID(),settlement,"Mine",founder,founder,"mining",site.id(),4,0);
        var req=new JobRequirements(216,Map.of());db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Test opportunity"));return b;
    }
    MineralObservation observation(long time,int... xs) {return new MineralObservation(world,time,true,Commodity.COAL,Arrays.stream(xs).mapToObj(x->new VillageObservation.Position(x,40,0)).toList());}
    MineralDiscoveryRepository.Discovery first() throws Exception {return discovery.discover(mine.siteId(),observation(10,0,1,2,3),2,BUNDLE);}
    long count(String table) throws Exception {try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String text) throws Exception {try(var s=db.getConnection().createStatement()){s.execute(text);}}
    @Test void coordinatesSurviveDepletionRestartAndRetuningWithoutReserveRefill() throws Exception {
        var saved=first();assertTrue(saved.created());assertEquals(4,saved.addedEvidence());assertEquals(8,saved.deposit().initialBatches());
        db.getIndustryRepository().mine(UUID.randomUUID(),mine.id(),saved.deposit().id(),8,0,8,8);
        db.close();open();var revisit=discovery.discover(mine.siteId(),observation(20,3,2,1,0),999,Map.of(Commodity.COAL,3L));
        assertFalse(revisit.created());assertEquals(0,revisit.addedEvidence());assertEquals(saved.deposit().id(),revisit.deposit().id());
        assertEquals(0,revisit.deposit().remainingBatches());assertEquals(BUNDLE,revisit.deposit().bundle());assertEquals(1,count("mineral_deposits"));
    }
    @Test void overlappingExtensionClaimsCoordinatesButNeverAddsReserves() throws Exception {
        var original=first();var extended=discovery.discover(mine.siteId(),observation(20,3,4,5),100,BUNDLE);
        assertEquals(original.deposit(),extended.deposit());assertEquals(2,extended.addedEvidence());assertEquals(6,discovery.claimedBlocks(original.deposit().id()));
        var fromOtherDirection=discovery.discover(mine.siteId(),observation(30,5,4),100,BUNDLE);
        assertEquals(original.deposit(),fromOtherDirection.deposit());assertEquals(0,fromOtherDirection.addedEvidence());
        assertEquals(original.deposit(),discovery.findAt(world,new VillageObservation.Position(5,40,0)).orElseThrow());
    }
    @Test void anotherSiteCannotClaimTheSameOreOrMintAnAlternateDeposit() throws Exception {
        var saved=first();var other=business(town,world,person,10);
        assertThrows(SQLException.class,()->discovery.discover(other.siteId(),observation(20,2,3,4),2,BUNDLE));
        assertEquals(1,count("mineral_deposits"));assertEquals(4,discovery.claimedBlocks(saved.deposit().id()));assertTrue(discovery.findAt(world,new VillageObservation.Position(4,40,0)).isEmpty());
    }
    @Test void surveysOverlappingTwoDepositsReportConflictAndDoNotMergeThem() throws Exception {
        var first=first();var other=business(town,world,person,10);var second=discovery.discover(other.siteId(),observation(10,100,101),2,BUNDLE);
        assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),observation(20,3,100,200),2,BUNDLE));
        assertEquals(2,count("mineral_deposits"));assertEquals(4,discovery.claimedBlocks(first.deposit().id()));assertEquals(2,discovery.claimedBlocks(second.deposit().id()));
    }
    @Test void equalCoordinatesInDifferentPermanentWorldsStayIndependent() throws Exception {
        var saved=first();var otherWorld=UUID.randomUUID();var otherTown=UUID.randomUUID();var otherPerson=UUID.randomUUID();
        db.saveSettlement(new Settlement(otherTown,"Other","World",0,64,0,1,0,0));db.getVillageIdentityRepository().observeWorld(otherWorld,"World");db.getVillageIdentityRepository().bindSettlement(otherTown,otherWorld);
        db.getPopulationRepository().insertResident(new Resident(otherPerson,"Miner",otherTown,null,300,Resident.Status.ALIVE));
        var other=business(otherTown,otherWorld,otherPerson,0);var evidence=new MineralObservation(otherWorld,10,true,Commodity.COAL,observation(10,0,1,2,3).ore());
        var separate=discovery.discover(other.siteId(),evidence,2,BUNDLE);assertNotEquals(saved.deposit().id(),separate.deposit().id());assertEquals(2,count("mineral_deposits"));
        db.getVillageIdentityRepository().observeWorld(world,"Renamed World");assertEquals(saved.deposit(),discovery.findAt(world,evidence.ore().getFirst()).orElseThrow());
    }
    @Test void partialEmptyWrongWorldAndUnsupportedYieldCannotInitialize() throws Exception {
        var partial=new MineralObservation(world,10,false,Commodity.COAL,observation(10,0).ore());
        assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),partial,2,BUNDLE));
        assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),observation(10),2,BUNDLE));
        var wrong=new MineralObservation(UUID.randomUUID(),10,true,Commodity.COAL,partial.ore());assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),wrong,2,BUNDLE));
        assertThrows(IllegalArgumentException.class,()->discovery.discover(mine.siteId(),observation(10,0),2,Map.of(Commodity.IRON,1L)));
        assertEquals(0,count("mineral_deposits"));assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void legacyDepositsRequireExplicitAdoptionRatherThanInventedEvidence() throws Exception {
        db.getIndustryRepository().discover(UUID.randomUUID(),mine.siteId(),4,8,BUNDLE);
        assertThrows(SQLException.class,()->first());assertEquals(0,count("mineral_discovery"));assertEquals(0,count("mineral_evidence"));assertEquals(1,count("mineral_deposits"));
    }
    @Test void disjointEvidenceCannotSilentlyExtendARegisteredSitesGeology() throws Exception {
        var saved=first();assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),observation(20,100),2,BUNDLE));
        assertEquals(4,discovery.claimedBlocks(saved.deposit().id()));assertEquals(saved.deposit(),db.getIndustryRepository().deposit(saved.deposit().id()).orElseThrow());
    }
    @Test void replacingOreKindOrPredatingDiscoveryCannotChangeIdentity() throws Exception {
        var saved=first();var iron=new MineralObservation(world,20,true,Commodity.IRON,observation(20,0).ore());
        assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),iron,2,Map.of(Commodity.IRON,1L)));
        assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),observation(9,0),2,BUNDLE));assertEquals(4,discovery.claimedBlocks(saved.deposit().id()));
    }
    @Test void failedCoordinateWriteRollsBackDepositYieldsMetadataAndPartialClaims() throws Exception {
        sql("CREATE TRIGGER fail_evidence BEFORE INSERT ON mineral_evidence WHEN NEW.x=2 BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->first());assertEquals(0,count("mineral_deposits"));assertEquals(0,count("mineral_yields"));assertEquals(0,count("mineral_discovery"));assertEquals(0,count("mineral_evidence"));
        sql("DROP TRIGGER fail_evidence");assertTrue(first().created());assertEquals(1,count("mineral_deposits"));
    }
    @Test void failedExtensionPreservesPreviouslySavedIdentityAndReserves() throws Exception {
        var saved=first();sql("CREATE TRIGGER fail_extension BEFORE INSERT ON mineral_evidence BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->discovery.discover(mine.siteId(),observation(20,3,4),2,BUNDLE));
        assertEquals(4,discovery.claimedBlocks(saved.deposit().id()));assertEquals(saved.deposit(),db.getIndustryRepository().deposit(saved.deposit().id()).orElseThrow());
        sql("DROP TRIGGER fail_extension");assertEquals(1,discovery.discover(mine.siteId(),observation(20,3,4),2,BUNDLE).addedEvidence());
    }
    @Test void schema31UpgradeFailureRollsBackAndDoesNotBackfillLegacyClaims() throws Exception {
        first();SchemaFixtures.rewind(db.getConnection(),31);sql("CREATE TABLE mineral_evidence(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(31,r.getInt(1));}
        assertEquals(1,count("mineral_deposits"));sql("DROP TABLE mineral_evidence");new SchemaMigrator().migrate(db.getConnection());
        assertEquals(0,count("mineral_discovery"));assertEquals(0,count("mineral_evidence"));assertThrows(SQLException.class,()->first());
    }
}
