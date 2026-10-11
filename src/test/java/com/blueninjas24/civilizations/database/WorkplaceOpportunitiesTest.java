package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkplaceOpportunitiesTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    WorkplaceObservation.Marker barrel=new WorkplaceObservation.Marker(new VillageObservation.Position(1,64,2),WorkplaceObservation.Kind.BARREL);
    WorkplaceObservation.Marker smith=new WorkplaceObservation.Marker(new VillageObservation.Position(3,64,4),WorkplaceObservation.Kind.SMITHING_TABLE);
    @BeforeEach void open()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkplaceTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,0,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
    }
    WorkplaceOpportunities repo(){return new WorkplaceOpportunities(db.getConnection());}
    void observe(long time,boolean complete,WorkplaceObservation.Marker... markers)throws Exception{repo().observe(town,new WorkplaceObservation(world,0,0,time,complete,List.of(markers)));}
    WorkplaceOpportunities.Review review(long time)throws Exception{return repo().review(town,time,100,Set.of(world),512);}
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    @AfterEach void close(){db.close();}
    @Test void currentEvidenceIsDurableSortedAndCreatesNoAssets()throws Exception {
        observe(1000,true,smith,barrel);assertEquals(2,review(1000).candidates().size());assertEquals("warehouse",review(1000).candidates().getFirst().industry());
        observe(1000,true,barrel,smith);assertEquals(2,count("workplace_sightings"));assertEquals(1,count("workplace_surveys"));
        for(var t:List.of("businesses","business_sites","treasury_entries","industry_stores","business_employment"))assertEquals(0,count(t));
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkplaceTest"));db.initialize();assertEquals(2,review(1000).candidates().size());
    }
    @Test void emptyFrameClearsOnlyItsChunkAndNeverUsesHistoricalUnion()throws Exception {
        observe(1000,true,barrel,smith);var adjacent=new WorkplaceObservation.Marker(new VillageObservation.Position(17,64,2),WorkplaceObservation.Kind.BARREL);
        repo().observe(town,new WorkplaceObservation(world,1,0,1000,true,List.of(adjacent)));observe(1001,true);
        assertEquals(List.of(adjacent.position()),review(1001).candidates().stream().map(WorkplaceOpportunities.Candidate::position).toList());assertEquals(3,count("workplace_sightings"));
        observe(1000,true,barrel,smith);assertEquals(1,review(1001).candidates().size());
    }
    @Test void partialFutureStaleAbsentOrUnknownWorldNeverOfferCandidate()throws Exception {
        observe(1000,true,barrel);assertTrue(review(999).candidates().isEmpty());assertTrue(review(1101).candidates().isEmpty());
        assertTrue(repo().review(town,1000,100,Set.of(),512).candidates().isEmpty());assertTrue(repo().review(town,1000,100,null,512).candidates().isEmpty());
        observe(1001,false);assertTrue(review(1001).candidates().isEmpty());assertEquals(1,count("workplace_sightings"));assertEquals(1,count("workplace_markers"));
        observe(1002,true,barrel);assertEquals(1,review(1002).candidates().size());
    }
    @Test void sameTimestampConflictAndFinalFailureRollbackWholeCapture()throws Exception {
        observe(1000,true,barrel);assertThrows(SQLException.class,()->observe(1000,true,smith));assertEquals(barrel.position(),review(1000).candidates().getFirst().position());
        try(var s=db.getConnection().createStatement()){s.execute("CREATE TRIGGER reject_marker BEFORE INSERT ON workplace_markers BEGIN SELECT RAISE(ABORT,'fixture marker failure'); END");}
        assertThrows(SQLException.class,()->observe(1001,true,smith));assertEquals(barrel.position(),review(1001).candidates().getFirst().position());assertEquals(1,count("workplace_sightings"));assertTrue(db.getConnection().getAutoCommit());
        try(var s=db.getConnection().createStatement()){s.execute("DROP TRIGGER reject_marker");}observe(1001,true,smith);assertEquals("blacksmith",review(1001).candidates().getFirst().industry());
    }
    @Test void worldMismatchOrCandidateBudgetCannotProducePartialSelection()throws Exception {
        assertThrows(SQLException.class,()->repo().observe(town,new WorkplaceObservation(UUID.randomUUID(),0,0,1000,true,List.of(barrel))));assertEquals(0,count("workplace_surveys"));
        observe(1000,true,barrel,smith);assertTrue(repo().review(town,1000,100,Set.of(world),1).candidates().isEmpty());assertThrows(IllegalArgumentException.class,()->repo().review(town,1000,100,Set.of(world),513));
    }
    @Test void overlappingTownsDeferBothUntilCurrentConflictDisappears()throws Exception {
        observe(1000,true,barrel);UUID other=UUID.randomUUID();db.saveSettlement(new Settlement(other,"Other","Fixture",0,64,0,0,0,0));db.getVillageIdentityRepository().bindSettlement(other,world);
        repo().observe(other,new WorkplaceObservation(world,0,0,1000,true,List.of(barrel)));assertTrue(review(1000).candidates().isEmpty());assertTrue(repo().review(other,1000,100,Set.of(world),512).candidates().isEmpty());
        repo().observe(other,new WorkplaceObservation(world,0,0,1001,true,List.of()));assertEquals(1,review(1001).candidates().size());assertEquals(2,count("workplace_sightings"));
    }
    @Test void schema57UpgradeAddsOnlyEmptyTablesAndPreservesWorldAndSettlement()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),57);new SchemaMigrator().migrate(db.getConnection());assertEquals(1,count("settlements"));assertEquals(1,count("settlement_worlds"));
        for(var t:List.of("workplace_surveys","workplace_sightings","workplace_markers"))assertEquals(0,count(t));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void schema58FailureRollsBackNewTablesAndVersionThenRecovers()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),57);
        try(var s=db.getConnection().createStatement()){s.execute("CREATE VIEW workplace_markers AS SELECT 1");}
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(57,r.getInt(1));}
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name IN('workplace_surveys','workplace_sightings')")){assertEquals(0,r.getInt(1));}
        try(var s=db.getConnection().createStatement()){s.execute("DROP VIEW workplace_markers");}new SchemaMigrator().migrate(db.getConnection());assertEquals(1,count("settlements"));assertEquals(0,count("workplace_surveys"));
    }
    @Test void alreadyRegisteredBusinessSiteIsNotAFreeNewWorkplace()throws Exception {
        observe(1000,true,barrel,smith);UUID owner=UUID.randomUUID();db.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(owner,"Fixture",town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        var p=barrel.position();var site=new BusinessSite(UUID.randomUUID(),town,world,p.x(),p.y(),p.z(),1000,"Explicit fixture site");var requirements=new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(UUID.randomUUID(),town,"Fixture",owner,owner,"warehouse",site.id(),1,0),site,requirements,requirements,0,"Fixture"));
        assertEquals(List.of(smith.position()),review(1000).candidates().stream().map(WorkplaceOpportunities.Candidate::position).toList());assertEquals(2,count("workplace_sightings"));
    }
    @Test void actualSqlOwnerAppliesConfiguredMarkerTypesAndWorldPause()throws Exception {
        db.close();var clock=new java.util.concurrent.atomic.AtomicLong(1000);
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),Logger.getLogger("WorkplaceTest"),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),clock::get)) {
            session.configureWorkplaces(new WorkplaceObserverPolicy(true,16,512,400,1,Set.of(WorkplaceObservation.Kind.BARREL)));session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.observeWorkplaces(town,new WorkplaceObservation(world,0,0,1000,true,List.of(barrel,smith))).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1,session.currentWorkplaceOpportunities(town,100,512).get(5,java.util.concurrent.TimeUnit.SECONDS).candidates().size());
            session.updateLoadedWorlds(Set.of()).get(5,java.util.concurrent.TimeUnit.SECONDS);assertTrue(session.currentWorkplaceOpportunities(town,100,512).get(5,java.util.concurrent.TimeUnit.SECONDS).candidates().isEmpty());
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkplaceTest"));db.initialize();assertEquals(2,review(1000).candidates().size());
    }
}
