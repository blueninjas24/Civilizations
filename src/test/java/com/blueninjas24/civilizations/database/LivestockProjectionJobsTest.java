package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockProjectionJobsTest {
    @TempDir Path directory;LivestockRepositoryTest f;
    @BeforeEach void setup()throws Exception{f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();f.feed(100);for(int p=1;p<=7;p++)f.advance(new UUID(100,p),p);}
    @AfterEach void close(){f.close();}
    LivestockBirthProjections repo(){return new LivestockBirthProjections(f.db.getConnection());}
    @Test void immutableSeekPagesCarrySavedWorldAgeSpeciesAndOptionalCurrentBody()throws Exception {
        var first=repo().page(null,1);assertEquals(1,first.size());var a=first.getFirst();assertEquals(7,a.herdPeriod());assertEquals(f.world,a.world());assertEquals(LivestockHerd.Species.COW,a.species());assertTrue(a.projection().isEmpty());
        var next=repo().page(a.birth().id(),8);assertEquals(1,next.size());assertNotEquals(a.birth().id(),next.getFirst().birth().id());assertTrue(repo().page(next.getFirst().birth().id(),8).isEmpty());assertThrows(UnsupportedOperationException.class,first::clear);assertThrows(IllegalArgumentException.class,()->repo().page(null,65));
    }
    @Test void claimedActiveAndFailedRemainVisibleButConfirmedDeathNeverRequestsAnotherBody()throws Exception {
        var jobs=repo().page(null,8);var a=jobs.getFirst();var b=jobs.getLast();
        var c=new Claim(a.birth().id(),new UUID(110,1),f.world,a.species(),0,64,0,1000);repo().claim(c,Set.of(f.world));assertEquals(State.CLAIMED,repo().page(null,8).getFirst().projection().orElseThrow().state());
        repo().activate(a.birth().id(),c.entity());assertEquals(State.ACTIVE,repo().page(null,8).getFirst().projection().orElseThrow().state());
        var d=new Claim(b.birth().id(),new UUID(110,2),f.world,b.species(),0,64,0,1000);repo().claim(d,Set.of(f.world));repo().failedUnspawned(b.birth().id(),d.entity());
        new LivestockLosses(f.db.getConnection()).record(new LivestockLosses.Observation(new UUID(110,3),c.entity(),f.world,2000,"Explicit fixture child death"));
        var remaining=repo().page(null,8);assertEquals(1,remaining.size());assertEquals(b.birth(),remaining.getFirst().birth());assertEquals(State.FAILED,remaining.getFirst().projection().orElseThrow().state());
        f.db.close();f.reopen();assertEquals(remaining,repo().page(null,8));
    }
    @Test void readingJobsDoesNotClaimAnimalsCreateGoodsOrGuessAnonymousBirths()throws Exception {
        var s=new LivestockLossesTest();s.f=f;var before=s.snapshot();var jobs=repo().page(null,8);assertEquals(2,jobs.size());assertEquals(before,s.snapshot());
        f.sql("DELETE FROM livestock_births WHERE id='"+jobs.getFirst().birth().id()+"'");assertEquals(1,repo().page(null,8).size());assertEquals(1,new LivestockBirths(f.db.getConnection()).unmappedBirths(f.business.id()));assertEquals(3,f.count("animal_ownership"));
    }
}
