package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockBirthsTest {
    @TempDir Path directory;LivestockRepositoryTest f;
    @BeforeEach void setup()throws Exception{f=new LivestockRepositoryTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    LivestockBirths births(){return new LivestockBirths(f.db.getConnection());}
    void open()throws Exception{f.openHerd();f.feed(100);}
    List<LivestockBirth> all()throws Exception{return births().page(f.business.id(),0,-1,256);}
    Map<String,List<List<Object>>> snapshot()throws Exception{var s=new LivestockLossesTest();s.f=f;return s.snapshot();}
    @Test void newbornsSaveWithTheProductionReceiptAndPersistWithoutClaimingAPhysicalAnimal()throws Exception {
        open();assertTrue(all().isEmpty());var beforeOwners=f.count("animal_ownership");var plan=f.advance(new UUID(9,1),1);
        assertEquals(1,plan.births());assertEquals(LivestockBirth.batch(f.business.id(),1,1,12),all());assertEquals(0,births().unmappedBirths(f.business.id()));
        var first=all().getFirst();assertEquals(first,births().find(first.id()).orElseThrow());f.db.close();f.reopen();assertEquals(List.of(first),all());
        assertEquals(beforeOwners,f.count("animal_ownership"));assertEquals(3,f.count("livestock_initial_animals"));assertEquals(2,f.units(f.milk));
    }
    @Test void replayCannotCreateAnotherBirthOrRewriteAnEarlierMaturityDate()throws Exception {
        open();var operation=new UUID(9,1);var plan=f.advance(operation,1);var before=snapshot();assertEquals(plan,f.advance(operation,1));assertEquals(before,snapshot());
        var p=LivestockPolicy.prototype();var retuned=new LivestockPolicy(true,3,p.breedingDelayPeriods(),p.feedPerBirth(),p.workPerBirth(),p.maximumBirthsPerPeriod(),p.cow(),p.chicken());
        assertThrows(SQLException.class,()->f.repo().advance(operation,f.business.id(),1,10,10,retuned,BusinessOperationPolicy.prototype(),true));assertEquals(before,snapshot());
        for(int period=2;period<=7;period++)f.repo().advance(new UUID(9,period),f.business.id(),period,10,10,retuned,BusinessOperationPolicy.prototype(),true);
        assertEquals(2,all().size());assertEquals(13,all().getFirst().maturePeriod());assertEquals(10,all().getLast().maturePeriod());assertEquals(0,births().unmappedBirths(f.business.id()));
    }
    @Test void identityInsertFailureRollsBackFeedGoodsCohortsAndReceiptBeforeExactRetry()throws Exception {
        open();var before=snapshot();f.sql("CREATE TRIGGER fail_birth_id BEFORE INSERT ON livestock_births BEGIN SELECT RAISE(ABORT,'explicit identity failure'); END");
        assertThrows(SQLException.class,()->f.advance(new UUID(9,1),1));assertEquals(before,snapshot());assertTrue(f.db.getConnection().getAutoCommit());
        f.sql("DROP TRIGGER fail_birth_id");assertEquals(1,f.advance(new UUID(9,1),1).births());assertEquals(1,all().size());assertEquals(94,f.units(f.grain));assertEquals(2,f.units(f.milk));
    }
    @Test void noFeedAbsentWorldAndDisabledPolicyDoNotInventBirths()throws Exception {
        f.openHerd();assertEquals(0,f.advance(new UUID(9,1),1).births());assertTrue(all().isEmpty());
        f.feed(100);f.repo().advance(new UUID(9,2),f.business.id(),2,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false);
        f.repo().advance(new UUID(9,3),f.business.id(),2,10,10,LivestockPolicy.disabled(),BusinessOperationPolicy.prototype(),true);
        assertTrue(all().isEmpty());assertEquals(0,births().unmappedBirths(f.business.id()));assertEquals(100,f.units(f.grain));
    }
    @Test void boundedChronologicalPagesDoNotSkipSiblingsOrRepeatTheCursor()throws Exception {
        var roster=new HashMap<>(f.roster);
        for(int n=20;n<22;n++){var id=new UUID(0,n);roster.put(id,0);new AnimalOwnershipRepository(f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,f.world,LivestockAdoption.Ownership.VILLAGE,null,f.town,1,"Explicit larger sibling fixture"));}
        f.repo().open(f.business.id(),LivestockHerd.Species.COW,f.grain,8,roster);f.feed(100);var p=LivestockPolicy.prototype();
        var twins=new LivestockPolicy(true,12,6,4,2,2,p.cow(),p.chicken());assertEquals(2,f.repo().advance(new UUID(9,1),f.business.id(),1,10,10,twins,BusinessOperationPolicy.prototype(),true).births());
        var a=births().page(f.business.id(),0,-1,1).getFirst();var b=births().page(f.business.id(),a.birthPeriod(),a.ordinal(),1).getFirst();
        assertEquals(0,a.ordinal());assertEquals(1,b.ordinal());assertNotEquals(a.id(),b.id());assertTrue(births().page(f.business.id(),b.birthPeriod(),b.ordinal(),1).isEmpty());
        assertThrows(IllegalArgumentException.class,()->births().page(f.business.id(),0,-1,257));assertThrows(IllegalArgumentException.class,()->births().page(f.business.id(),-1,-1,1));
        assertThrows(UnsupportedOperationException.class,()->births().page(f.business.id(),0,-1,1).clear());
    }
    @Test void rowsNeedTheProductionTransactionAndExactSavedBirthCountAndMaturity()throws Exception {
        open();f.advance(new UUID(9,1),1);assertThrows(SQLException.class,()->births().recordRows(f.business.id(),1,1,12));
        var connection=f.db.getConnection();connection.setAutoCommit(false);
        try {
            assertEquals(all(),births().recordRows(f.business.id(),1,1,12));
            assertThrows(SQLException.class,()->births().recordRows(f.business.id(),1,2,12));
            assertThrows(SQLException.class,()->births().recordRows(f.business.id(),1,1,3));
            assertThrows(SQLException.class,()->births().recordRows(f.business.id(),99,1,12));
        }finally{connection.rollback();connection.setAutoCommit(true);}
        assertEquals(1,all().size());assertEquals(13,all().getFirst().maturePeriod());
    }
    @Test void olderAnonymousBirthsArePreservedAndReportedRatherThanBackfilledAsInventedAnimals()throws Exception {
        open();f.advance(new UUID(9,1),1);SchemaFixtures.rewind(f.db.getConnection(),84);var before=snapshot();
        new SchemaMigrator().migrate(f.db.getConnection());var after=snapshot();assertEquals(List.of(),after.remove("livestock_learning_reviews"));assertEquals(List.of(),after.remove("livestock_learning_entries"));assertEquals(List.of(),after.remove("livestock_slaughter"));assertEquals(List.of(),after.remove("quarry_founder_courses"));assertEquals(List.of(),after.remove("quarry_founder_reviews"));assertEquals(List.of(),after.remove("quarry_survey_members"));assertEquals(List.of(),after.remove("quarry_surveys"));assertEquals(List.of(),after.remove("quarry_opportunities"));assertEquals(List.of(),after.remove("forestry_work"));assertEquals(List.of(),after.remove("forestry_supports"));assertEquals(List.of(),after.remove("forestry_habitat_changes"));assertEquals(List.of(),after.remove("forestry_resources"));assertEquals(List.of(),after.remove("forestry_tracking"));assertEquals(List.of(),after.remove("forestry_exclusions"));assertEquals(List.of(),after.remove("forestry_source_losses"));assertEquals(List.of(),after.remove("active_pen_claims"));assertEquals(List.of(),after.remove("pen_releases"));assertEquals(List.of(),after.remove("livestock_presence"));assertEquals(List.of(),after.remove("livestock_birth_losses"));assertEquals(List.of(),after.remove("livestock_birth_projections"));assertEquals(List.of(),after.remove("livestock_birth_entities"));assertEquals(List.of(),after.remove("livestock_births"));assertEquals(before,after);
        assertEquals(1,births().unmappedBirths(f.business.id()));assertTrue(all().isEmpty());assertEquals(4,f.repo().load(f.business.id()).orElseThrow().state().heads());
        f.advance(new UUID(9,1),1);assertTrue(all().isEmpty());assertEquals(1,births().unmappedBirths(f.business.id()));
    }
    @Test void schema84FailureRollsBackThenRetriesWithoutChangingOldAssetsOrVersions()throws Exception {
        open();f.advance(new UUID(9,1),1);SchemaFixtures.rewind(f.db.getConnection(),84);f.sql("CREATE VIEW livestock_births AS SELECT 1 AS blocker");var before=snapshot();
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));assertEquals(before,snapshot());
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertTrue(r.next());assertEquals(84,r.getInt(1));}
        f.sql("DROP VIEW livestock_births");new SchemaMigrator().migrate(f.db.getConnection());var after=snapshot();assertEquals(List.of(),after.remove("livestock_learning_reviews"));assertEquals(List.of(),after.remove("livestock_learning_entries"));assertEquals(List.of(),after.remove("livestock_slaughter"));assertEquals(List.of(),after.remove("quarry_founder_courses"));assertEquals(List.of(),after.remove("quarry_founder_reviews"));assertEquals(List.of(),after.remove("quarry_survey_members"));assertEquals(List.of(),after.remove("quarry_surveys"));assertEquals(List.of(),after.remove("quarry_opportunities"));assertEquals(List.of(),after.remove("forestry_work"));assertEquals(List.of(),after.remove("forestry_supports"));assertEquals(List.of(),after.remove("forestry_habitat_changes"));assertEquals(List.of(),after.remove("forestry_resources"));assertEquals(List.of(),after.remove("forestry_tracking"));assertEquals(List.of(),after.remove("forestry_exclusions"));assertEquals(List.of(),after.remove("forestry_source_losses"));assertEquals(List.of(),after.remove("active_pen_claims"));assertEquals(List.of(),after.remove("pen_releases"));assertEquals(List.of(),after.remove("livestock_presence"));assertEquals(List.of(),after.remove("livestock_birth_losses"));assertEquals(List.of(),after.remove("livestock_birth_projections"));assertEquals(List.of(),after.remove("livestock_birth_entities"));assertEquals(List.of(),after.remove("livestock_births"));assertEquals(before,after);
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void missingRegistryRowsAreVisibleAndHistoricalReplayDoesNotSilentlyRepairThem()throws Exception {
        open();f.advance(new UUID(9,1),1);f.sql("DELETE FROM livestock_births");assertEquals(1,births().unmappedBirths(f.business.id()));
        f.advance(new UUID(9,1),1);assertTrue(all().isEmpty());assertEquals(1,births().unmappedBirths(f.business.id()));
        assertThrows(SQLException.class,()->births().unmappedBirths(UUID.randomUUID()));
    }
    @Test void excessOrForgedIdentityRowsAreRejectedRatherThanConsideredReliableCoverage()throws Exception {
        open();f.advance(new UUID(9,1),1);var first=all().getFirst();
        f.sql("UPDATE livestock_births SET id='"+UUID.randomUUID()+"' WHERE id='"+first.id()+"'");assertThrows(SQLException.class,()->all());
        f.sql("UPDATE livestock_births SET id='"+first.id()+"'");var extra=LivestockBirth.batch(f.business.id(),1,2,12).getLast();
        try(var s=f.db.getConnection().prepareStatement("INSERT INTO livestock_births VALUES(?,?,?,?,?)")) {
            s.setString(1,extra.id().toString());s.setString(2,extra.business().toString());s.setLong(3,extra.birthPeriod());s.setInt(4,extra.ordinal());s.setLong(5,extra.maturePeriod());s.executeUpdate();
        }
        assertThrows(SQLException.class,()->births().unmappedBirths(f.business.id()));
    }
}
