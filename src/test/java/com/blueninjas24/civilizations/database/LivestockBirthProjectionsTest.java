package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockBirthProjectionsTest {
    @TempDir Path directory;LivestockRepositoryTest f;LivestockBirth birth;
    @BeforeEach void setup()throws Exception {
        f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();f.feed(100);f.advance(new UUID(9,1),1);
        birth=new LivestockBirths(f.db.getConnection()).page(f.business.id(),0,-1,1).getFirst();
    }
    @AfterEach void close(){f.close();}
    LivestockBirthProjections repo(){return new LivestockBirthProjections(f.db.getConnection());}
    Claim claim(UUID entity){return new Claim(birth.id(),entity,f.world,LivestockHerd.Species.COW,0,64,0,1000);}
    Claim claim(){return claim(new UUID(10,1));}
    Map<String,List<List<Object>>> snapshot()throws Exception{var s=new LivestockLossesTest();s.f=f;return s.snapshot();}
    Projection save(Claim c)throws Exception{return repo().claim(c,Set.of(f.world));}
    @Test void claimCommitsOwnershipBeforeActivationWithoutAddingHeadsOrGoodsAndSurvivesRestart()throws Exception {
        var c=claim();var state=f.repo().load(f.business.id()).orElseThrow();var store=f.db.getIndustryRepository().store(f.business.id()).orElseThrow();
        assertEquals(State.CLAIMED,save(c).state());assertTrue(new AnimalOwnershipRepository(f.db.getConnection()).villageAnimals().contains(c.entity()));
        assertEquals(state,f.repo().load(f.business.id()).orElseThrow());assertEquals(store,f.db.getIndustryRepository().store(f.business.id()).orElseThrow());
        assertEquals(save(c),repo().claim(c,Set.of()));assertTrue(repo().activate(birth.id(),c.entity()));assertTrue(repo().activate(birth.id(),c.entity()));
        var saved=repo().find(birth.id()).orElseThrow();f.db.close();f.reopen();assertEquals(saved,repo().find(birth.id()).orElseThrow());assertEquals(State.ACTIVE,repo().claim(c,Set.of()).state());
        assertEquals(1,f.count("livestock_birth_entities"));assertEquals(4,f.repo().load(f.business.id()).orElseThrow().state().heads());
    }
    @Test void activeOrUnresolvedClaimNeverAuthorizesAnotherBodyOrMissingEntityReplacement()throws Exception {
        var c=claim();save(c);var before=snapshot();assertThrows(SQLException.class,()->save(claim(new UUID(10,2))));assertEquals(before,snapshot());
        assertFalse(repo().activate(birth.id(),new UUID(10,2)));assertFalse(repo().failedUnspawned(birth.id(),new UUID(10,2)));
        assertTrue(repo().activate(birth.id(),c.entity()));assertFalse(repo().failedUnspawned(birth.id(),c.entity()));assertThrows(SQLException.class,()->save(claim(new UUID(10,2))));
    }
    @Test void positivelyUnspawnedFailureKeepsEveryAliasAndStaleCallbacksCannotAffectTheNewAttempt()throws Exception {
        var first=claim();save(first);assertTrue(repo().failedUnspawned(birth.id(),first.entity()));assertTrue(repo().failedUnspawned(birth.id(),first.entity()));
        var second=claim(new UUID(10,2));assertEquals(State.CLAIMED,save(second).state());assertEquals(State.FAILED,repo().forEntity(first.entity()).orElseThrow().state());
        assertEquals(State.FAILED,repo().claim(first,Set.of()).state());assertFalse(repo().activate(birth.id(),first.entity()));assertFalse(repo().failedUnspawned(birth.id(),first.entity()));
        assertEquals(second,repo().find(birth.id()).orElseThrow().claim());assertEquals(2,f.count("livestock_birth_entities"));assertEquals(5,f.count("animal_ownership"));
    }
    @Test void playerOriginalAnimalWrongSpeciesWrongWorldAndAbsentWorldCannotBeClaimed()throws Exception {
        var playerAnimal=new UUID(11,1);new AnimalOwnershipRepository(f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(playerAnimal,f.world,LivestockAdoption.Ownership.PLAYER,f.owner,null,1,"Explicit player animal"));
        var before=snapshot();assertThrows(SQLException.class,()->save(claim(playerAnimal)));assertThrows(SQLException.class,()->save(claim(f.roster.keySet().iterator().next())));
        var c=claim();assertThrows(SQLException.class,()->repo().claim(c,Set.of()));
        assertThrows(SQLException.class,()->save(new Claim(c.birth(),c.entity(),c.world(),LivestockHerd.Species.CHICKEN,0,64,0,1000)));
        assertThrows(SQLException.class,()->repo().claim(new Claim(c.birth(),c.entity(),new UUID(99,1),c.species(),0,64,0,1000),Set.of(new UUID(99,1))));
        assertEquals(before,snapshot());
    }
    @Test void knownVillagerIdentityAndChangedClaimEvidenceCannotBeReusedForAnOffspring()throws Exception {
        var villager=new UUID(12,1);f.db.getVillageIdentityRepository().bindEntity(f.world,villager,f.owner);
        assertThrows(SQLException.class,()->save(claim(villager)));var c=claim();save(c);var before=snapshot();
        assertThrows(SQLException.class,()->save(new Claim(c.birth(),c.entity(),c.world(),c.species(),0,64,0,1001)));
        assertThrows(SQLException.class,()->save(new Claim(UUID.randomUUID(),c.entity(),c.world(),c.species(),0,64,0,1000)));assertEquals(before,snapshot());
    }
    @Test void finalProjectionFailureRollsBackOwnershipAndAliasForAnExactRetry()throws Exception {
        var before=snapshot();var c=claim();f.sql("CREATE TRIGGER refuse_birth_claim BEFORE INSERT ON livestock_birth_projections BEGIN SELECT RAISE(ABORT,'explicit claim failure'); END");
        assertThrows(SQLException.class,()->save(c));assertEquals(before,snapshot());assertTrue(f.db.getConnection().getAutoCommit());
        f.sql("DROP TRIGGER refuse_birth_claim");assertEquals(State.CLAIMED,save(c).state());assertEquals(4,f.count("animal_ownership"));
    }
    @Test void confirmedDeathIsPermanentAndCannotBecomeAnotherAttempt()throws Exception {
        var c=claim();save(c);new LivestockLosses(f.db.getConnection()).record(new LivestockLosses.Observation(new UUID(13,1),c.entity(),f.world,2000,"Explicit fixture death")).orElseThrow();
        assertEquals(State.DECEASED,repo().find(birth.id()).orElseThrow().state());assertEquals(State.DECEASED,save(c).state());
        assertFalse(repo().activate(birth.id(),c.entity()));assertFalse(repo().failedUnspawned(birth.id(),c.entity()));assertThrows(SQLException.class,()->save(claim(new UUID(10,2))));
        assertEquals(1,f.count("livestock_births"));assertEquals(1,f.count("livestock_birth_entities"));
    }
    @Test void anonymousOlderBirthHistoryBlocksNewPhysicalClaimsWithoutInventingMappings()throws Exception {
        for(int period=2;period<=7;period++)f.advance(new UUID(9,period),period);
        var children=new LivestockBirths(f.db.getConnection()).page(f.business.id(),0,-1,256);assertEquals(2,children.size());
        f.sql("DELETE FROM livestock_births WHERE id='"+children.getFirst().id()+"'");birth=children.getLast();var before=snapshot();
        assertThrows(SQLException.class,()->save(claim()));assertEquals(before,snapshot());assertEquals(1,new LivestockBirths(f.db.getConnection()).unmappedBirths(f.business.id()));
    }
    @Test void schema85PartialDdlFailureRollsBackAndRetryPreservesEveryBirthAndAsset()throws Exception {
        SchemaFixtures.rewind(f.db.getConnection(),85);var before=snapshot();f.sql("CREATE VIEW livestock_birth_projections AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));assertEquals(before,snapshot());
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertTrue(r.next());assertEquals(85,r.getInt(1));}
        f.sql("DROP VIEW livestock_birth_projections");new SchemaMigrator().migrate(f.db.getConnection());var after=snapshot();
        for(var table:List.of("livestock_birth_entities","livestock_birth_projections","livestock_birth_losses"))assertEquals(List.of(),after.remove(table));assertEquals(before,after);
        assertEquals(birth,new LivestockBirths(f.db.getConnection()).find(birth.id()).orElseThrow());
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void unknownReadsAndRejectedNestedWritesLeaveTheRegistryUntouched()throws Exception {
        var before=snapshot();assertTrue(repo().find(UUID.randomUUID()).isEmpty());assertTrue(repo().forEntity(UUID.randomUUID()).isEmpty());
        f.db.getConnection().setAutoCommit(false);try{assertThrows(SQLException.class,()->save(claim()));}finally{f.db.getConnection().rollback();f.db.getConnection().setAutoCommit(true);}
        assertEquals(before,snapshot());
    }
}
