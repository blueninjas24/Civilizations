package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.database.LivestockPresence.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockPresenceTest {
    @TempDir Path directory;LivestockRepositoryTest f;UUID adult=new UUID(0,10),young=new UUID(0,12);
    @BeforeEach void setup()throws Exception{f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();f.feed(100);}
    @AfterEach void close(){f.close();}
    LivestockPresence repo(){return new LivestockPresence(f.db.getConnection());}
    Sight sight(UUID id,int x,long time){return new Sight(id,LivestockHerd.Species.COW,f.world,new Position(x,64,0),time);}
    long unavailable(long period,int radius)throws Exception{return repo().unavailableAdultsRows(f.business.id(),period,new LivestockWorkingArea(radius));}
    @Test void confirmedAwayPausesOnlyThatAnimalAndEmptyObservationRestartDoesNotResetEvidence()throws Exception {
        assertEquals(1,repo().record(List.of(sight(adult,40,1000))));assertEquals(1,unavailable(1,32));
        assertEquals(0,repo().record(List.of()));f.db.close();f.reopen();assertEquals(1,unavailable(1,32));
        var p=f.advance(new UUID(40,1),1);assertEquals(1,p.renewableUnits());assertEquals(0,p.births());assertEquals(3,p.after().heads());assertEquals(99,f.units(f.grain));
        assertEquals(1,repo().record(List.of(sight(adult,0,2000))));assertEquals(0,unavailable(2,32));
        var returned=f.advance(new UUID(40,2),2);assertEquals(3,returned.renewableUnits());assertEquals(1,returned.births());assertEquals(4,returned.after().heads());
        assertEquals(3,new AnimalOwnershipRepository(f.db.getConnection()).villageAnimals().size());
    }
    @Test void awayYoungAgesNormallyAndCountsOnlyWhenMatureIncludingOffspring()throws Exception {
        repo().record(List.of(sight(young,40,1000)));assertEquals(0,unavailable(1,32));assertEquals(1,unavailable(2,32));
        var p=f.advance(new UUID(41,1),1);var child=new LivestockBirths(f.db.getConnection()).page(f.business.id(),0,-1,1).getFirst();
        UUID entity=new UUID(50,1);var claims=new LivestockBirthProjections(f.db.getConnection());claims.claim(new Claim(child.id(),entity,f.world,LivestockHerd.Species.COW,0,64,0,1000),Set.of(f.world));
        assertEquals(3,repo().page(null,16).size());assertEquals(0,repo().record(List.of(sight(entity,40,2000))));claims.activate(child.id(),entity);
        assertEquals(1,repo().record(List.of(sight(entity,40,2000))));assertEquals(1,unavailable(12,32));assertEquals(2,unavailable(13,32));
        var mature=f.advance(new UUID(41,2),2);assertEquals(1,mature.matured());assertEquals(3,mature.after().adults());assertEquals(2,mature.renewableUnits());assertEquals(4,mature.after().heads());
    }
    @Test void missingNeverSeenKeepsEligibilityAndRadiusRetuningUsesSavedPositiveCoordinates()throws Exception {
        assertEquals(0,unavailable(1,32));repo().record(List.of(sight(adult,32,1000)));assertEquals(0,unavailable(1,32));assertEquals(1,unavailable(1,31));
        var p=f.repo().advance(new UUID(42,1),f.business.id(),1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true,new LivestockWorkingArea(31));
        assertEquals(1,p.renewableUnits());assertTrue(p.after().heads()==3);assertEquals(0,p.births());
        assertThrows(SQLException.class,()->f.advance(new UUID(42,1),1));
        assertEquals(p,f.repo().advance(new UUID(42,1),f.business.id(),1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true,new LivestockWorkingArea(31)));
    }
    @Test void olderEvidenceCannotUndoReturnAndConflictingSameTimeBatchRollsBack()throws Exception {
        repo().record(List.of(sight(adult,40,1000)));repo().record(List.of(sight(adult,0,2000)));assertEquals(0,repo().record(List.of(sight(adult,40,1500))));
        assertEquals(0,repo().record(List.of(sight(adult,0,2000))));assertThrows(SQLException.class,()->repo().record(List.of(sight(young,40,2100),sight(adult,40,2000))));
        assertTrue(repo().find(young).isEmpty());assertEquals(0,unavailable(2,32));assertTrue(f.db.getConnection().getAutoCommit());
    }
    @Test void unknownPlayerAndDeadAnimalsCannotBecomeMembersOrBeResurrectedBySightings()throws Exception {
        var unknown=UUID.randomUUID();assertEquals(0,repo().record(List.of(sight(unknown,0,1000))));
        repo().record(List.of(sight(adult,40,1000)));new LivestockLosses(f.db.getConnection()).record(new LivestockLosses.Observation(UUID.randomUUID(),adult,f.world,2000,"Fixture death"));
        assertEquals(0,unavailable(1,32));assertEquals(0,repo().record(List.of(sight(adult,0,3000))));assertEquals(40,repo().find(adult).orElseThrow().position().x());
        assertEquals(2,repo().page(null,16).size());assertEquals(2,f.repo().load(f.business.id()).orElseThrow().state().heads());
    }
    @Test void foreignWorldCountsAsAwayWithoutTransferringOwnershipAndPagesAreBounded()throws Exception {
        repo().record(List.of(new Sight(adult,LivestockHerd.Species.COW,UUID.randomUUID(),new Position(0,64,0),1000)));
        assertEquals(1,unavailable(1,256));assertEquals(f.world,new AnimalOwnershipRepository(f.db.getConnection()).load(adult).orElseThrow().originWorld());
        var first=repo().page(null,1);assertEquals(1,first.size());var rest=repo().page(first.getFirst().animal(),16);assertEquals(2,rest.size());assertFalse(rest.stream().anyMatch(m->m.animal().equals(first.getFirst().animal())));
        assertThrows(UnsupportedOperationException.class,()->first.clear());assertThrows(IllegalArgumentException.class,()->repo().page(null,257));
    }
    @Test void databaseFailureWrongSpeciesAndNestedWritesPreserveTheWholePreviousState()throws Exception {
        f.sql("CREATE TRIGGER fail_presence BEFORE INSERT ON livestock_presence BEGIN SELECT RAISE(ABORT,'fixture position failure'); END");
        assertThrows(SQLException.class,()->repo().record(List.of(sight(adult,40,1000))));assertTrue(repo().find(adult).isEmpty());f.sql("DROP TRIGGER fail_presence");
        assertThrows(SQLException.class,()->repo().record(List.of(sight(young,40,1000),new Sight(adult,LivestockHerd.Species.CHICKEN,f.world,new Position(0,64,0),1000))));assertEquals(0,f.count("livestock_presence"));
        f.db.getConnection().setAutoCommit(false);try{assertThrows(SQLException.class,()->repo().record(List.of(sight(adult,40,1000))));}finally{f.db.getConnection().rollback();f.db.getConnection().setAutoCommit(true);}
        assertThrows(IllegalArgumentException.class,()->repo().record(List.of(sight(adult,0,1),sight(adult,0,1))));assertEquals(3,f.repo().load(f.business.id()).orElseThrow().state().heads());
    }
    @Test void schema86MigrationAndFailurePreserveEveryPriorAssetAndCreateNoPresence()throws Exception {
        SchemaFixtures.rewind(f.db.getConnection(),86);var helper=new LivestockLossesTest();helper.f=f;var before=helper.snapshot();
        f.sql("CREATE VIEW livestock_presence AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));assertEquals(before,helper.snapshot());f.sql("DROP VIEW livestock_presence");
        new SchemaMigrator().migrate(f.db.getConnection());var after=helper.snapshot();assertEquals(List.of(),after.remove("livestock_learning_reviews"));assertEquals(List.of(),after.remove("livestock_learning_entries"));assertEquals(List.of(),after.remove("active_pen_claims"));assertEquals(List.of(),after.remove("pen_releases"));assertEquals(List.of(),after.remove("livestock_presence"));assertEquals(before,after);
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
