package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockBirthOwnerTest {
    @TempDir Path directory;LivestockRepositoryTest f;PopulationSession session;Claim claim;
    @BeforeEach void setup()throws Exception {
        f=new LivestockRepositoryTest();f.directory=directory;f.setup();var capture=new LivestockCapture(f.db.getConnection());assertTrue(capture.begin(1000).trusted());
        f.openHerd();f.feed(100);f.advance(new UUID(9,1),1);var birth=new LivestockBirths(f.db.getConnection()).page(f.business.id(),0,-1,1).getFirst();
        claim=new Claim(birth.id(),new UUID(20,1),f.world,LivestockHerd.Species.COW,0,64,0,3000);capture.finish(2000,true);
    }
    @AfterEach void close(){if(session!=null)session.close();f.close();}
    void start(boolean worlds)throws Exception {
        f.db.close();session=new PopulationSession(directory.toFile(),Logger.getLogger("Birth owner fixture"),PopulationSession.Settings.prototype(),()->3000);
        if(worlds)session.start(false,Set.of(f.world)).join();else session.start(false).join();
    }
    @Test void committedVillageIdentityPublishesBeforeClaimFutureCompletesAndConfirmedChildLossDrainsTheFence()throws Exception {
        start(true);assertTrue(session.livestockCaptureReady());assertEquals(State.CLAIMED,session.claimLivestockBirth(claim).join().state());
        assertTrue(session.villageAnimals().contains(claim.entity()));assertTrue(session.activateLivestockBirth(claim.birth(),claim.entity()).join());
        var death=new LivestockLosses.Observation(new UUID(30,1),claim.entity(),f.world,4000,"Explicit offspring owner fixture death");
        session.stageLivestockLoss(death);assertFalse(session.livestockCaptureReady());assertTrue(session.recordLivestockLoss(death).join().isPresent());assertTrue(session.livestockCaptureReady());
        assertEquals(State.DECEASED,session.livestockBirthProjection(claim.birth()).join().orElseThrow().state());assertFalse(session.activateLivestockBirth(claim.birth(),claim.entity()).join());
        session.close();f.reopen();assertEquals(3,f.repo().load(f.business.id()).orElseThrow().state().heads());assertEquals(1,f.count("livestock_birth_losses"));
    }
    @Test void unknownWorldMasksCannotCreateAClaimOrPublishAnotherAnimal()throws Exception {
        start(false);assertThrows(CompletionException.class,()->session.claimLivestockBirth(claim).join());assertFalse(session.villageAnimals().contains(claim.entity()));
        assertTrue(session.livestockBirthProjection(claim.birth()).join().isEmpty());
    }
    @Test void untrustedHistoryRefusesNewClaimsButExactCommittedRecoveryNeverRepairsTheGap()throws Exception {
        new LivestockCapture(f.db.getConnection()).markGap(2500);start(true);assertFalse(session.livestockCaptureReady());
        assertThrows(CompletionException.class,()->session.claimLivestockBirth(claim).join());assertFalse(session.villageAnimals().contains(claim.entity()));
    }
    @Test void alreadyCommittedPendingBodyCanActivateDuringRecoveryWithoutCreatingAnotherOrClearingUntrustedHistory()throws Exception {
        new LivestockBirthProjections(f.db.getConnection()).claim(claim,Set.of(f.world));new LivestockCapture(f.db.getConnection()).markGap(2500);
        start(true);assertFalse(session.livestockCaptureReady());assertEquals(State.CLAIMED,session.claimLivestockBirth(claim).join().state());
        assertTrue(session.activateLivestockBirth(claim.birth(),claim.entity()).join());assertFalse(session.livestockCaptureReady());
        assertFalse(session.failUnspawnedLivestockBirth(claim.birth(),claim.entity()).join());
        assertEquals(State.ACTIVE,session.livestockBirthProjection(claim.birth()).join().orElseThrow().state());
    }
}
