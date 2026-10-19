package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class ResourceMutationCaptureTest {
    @TempDir Path directory;
    private ResourceMutationCapture.Changes changes() {
        var positions=new ArrayList<>(List.of(new Position(-1,64,-1)));
        var frozen=new ResourceMutationCapture.Changes(new UUID(0,1),"Fixture",positions,List.of(),positions,List.of(),1000,"Confirmed external fixture change");
        positions.clear();assertEquals(1,frozen.orePlacements().size());return frozen;
    }
    @Test void readinessReservationLastsThroughDeferredSaveAndClosesExactlyOnce() {
        var releases=new AtomicInteger();var gaps=new AtomicInteger();var saved=new CompletableFuture<Void>();
        var capture=new ResourceMutationCapture(releases::incrementAndGet,reason->gaps.incrementAndGet(),change->saved);
        var completed=capture.complete(changes());capture.close();
        assertFalse(completed.isDone());assertEquals(0,releases.get());assertEquals(0,gaps.get());
        saved.complete(null);completed.join();assertEquals(1,releases.get());capture.close();assertEquals(1,releases.get());
        assertThrows(IllegalStateException.class,capture::unchanged);
    }
    @Test void unchangedWritesDoNotSaveAndAmbiguousOrFailedHandoffsStayFenced() {
        var releases=new AtomicInteger();var gaps=new AtomicInteger();var writes=new AtomicInteger();
        var unchanged=new ResourceMutationCapture(releases::incrementAndGet,reason->gaps.incrementAndGet(),change->{writes.incrementAndGet();return CompletableFuture.completedFuture(null);});
        unchanged.unchanged();unchanged.close();assertEquals(1,releases.get());assertEquals(0,gaps.get());assertEquals(0,writes.get());
        new ResourceMutationCapture(releases::incrementAndGet,reason->gaps.incrementAndGet(),change->CompletableFuture.completedFuture(null)).close();
        var failed=new ResourceMutationCapture(releases::incrementAndGet,reason->gaps.incrementAndGet(),change->CompletableFuture.failedFuture(new IllegalStateException("fixture failed save")));
        assertThrows(CompletionException.class,()->failed.complete(changes()).join());failed.close();
        var rejected=new ResourceMutationCapture(releases::incrementAndGet,reason->gaps.incrementAndGet(),change->{throw new RejectedExecutionException("fixture queue full");});
        assertThrows(RejectedExecutionException.class,()->rejected.complete(changes()));rejected.close();
        assertEquals(4,releases.get());assertEquals(3,gaps.get());
    }
    private PopulationSession session() {
        var session=new PopulationSession(directory.toFile(),Logger.getLogger("ExternalCaptureTest"),PopulationSession.Settings.prototype(),()->1000);
        session.configureOreTracking();session.configureForestTracking();return session;
    }
    @Test void actualOwnerPersistsBothProvenancesAndCleanRestartKeepsThem()throws Exception {
        try(var session=session()) {
            session.start(false).get(5,TimeUnit.SECONDS);
            var capture=session.beginExternalResourceMutation();
            assertFalse(session.oreCaptureReady());assertFalse(session.forestCaptureReady());
            // Another bounded write can be reserved while a previous save is pending.
            var unchanged=session.beginExternalResourceMutation();unchanged.unchanged();
            assertFalse(session.oreCaptureReady());
            capture.complete(changes()).get(5,TimeUnit.SECONDS);capture.close();
            assertTrue(session.oreCaptureReady());assertTrue(session.forestCaptureReady());
        }
        try(var reopened=session()) {
            reopened.start(false).get(5,TimeUnit.SECONDS);assertTrue(reopened.oreDiscoveryReady());assertTrue(reopened.forestDiscoveryReady());
            var point=new Position(-1,64,-1);
            assertEquals(Set.of(point),reopened.orePlacementsInChunk(new UUID(0,1),-1,-1,512).get(5,TimeUnit.SECONDS));
            assertTrue(reopened.forestExclusions(new UUID(0,1),-1,-1).get(5,TimeUnit.SECONDS).contains(point));
        }
    }
    @Test void shutdownWithUnconfirmedWorldWriteCannotCertifyCleanHistory()throws Exception {
        var session=session();session.start(false).get(5,TimeUnit.SECONDS);
        var unfinished=session.beginExternalResourceMutation();session.close();
        unfinished.close();
        assertThrows(IllegalStateException.class,session::beginExternalResourceMutation);
        try(var reopened=session()) {
            reopened.start(false).get(5,TimeUnit.SECONDS);
            assertFalse(reopened.oreDiscoveryReady());assertFalse(reopened.forestDiscoveryReady());
        }
    }
}
