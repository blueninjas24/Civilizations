package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PopulationSessionTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong(1000);
    private PopulationSession.Settings settings(int budget, int offline) {
        return new PopulationSession.Settings(42, 30, offline, budget,
                new PopulationPolicy(12, 216, 240, 480, 720, 0, 0, 0), MigrationPolicy.disabled(),
                VillageRecognition.Policy.prototype(), VillageEnrollmentRepository.Policy.prototype());
    }
    private PopulationSession session(PopulationSession.Settings settings) {
        return new PopulationSession(directory.toFile(), Logger.getLogger("PopulationSessionTest"), settings, now::get);
    }
    private VillageObservation observed() {
        var bed = new VillageObservation.Position(0, 64, 0);
        var secondBed = new VillageObservation.Position(2, 64, 0);
        return new VillageObservation(new UUID(0, 10), "test-world", now.get(), true, List.of(bed, secondBed), List.of(
                new VillageObservation.Person(new UUID(0, 11), bed, true, "First"),
                new VillageObservation.Person(new UUID(0, 12), secondBed, true, "Second")));
    }
    private static <T> T await(CompletableFuture<T> result) throws Exception { return result.get(5, TimeUnit.SECONDS); }

    @Test void projectionOperationsPublishConsistentImmutableViewsAndDeathSurvivesRestart() throws Exception {
        var observation = observed();
        var entity = observation.villagers().getFirst().entityId(); UUID person;
        try (var session = session(settings(2, 12))) {
            await(session.start(false));
            person = await(session.observe(observation)).villages().getFirst().observedResidents().get(entity).id();
            var before = session.projectionView().orElseThrow();
            assertEquals(2, before.population().livingCount()); assertEquals(2, before.identities().projections().size());
            assertTrue(await(session.recordPhysicalDeath(observation.worldId(), entity, 1100, "FIRE")));
            var after = session.projectionView().orElseThrow();
            assertEquals(1, after.population().livingCount()); assertEquals(1, session.status().livingPeople());
            assertEquals(com.blueninjas24.civilizations.database.ResidentProjectionRepository.State.RETIRED,
                    after.identities().projections().get(person).state());
            assertEquals(2, before.population().livingCount());
            assertFalse(await(session.recordPhysicalDeath(observation.worldId(), entity, 1200, "FIRE")));
            assertEquals(0, await(session.observe(observation)).villages().getFirst().importedPeople());
            assertEquals(1, session.snapshot().orElseThrow().livingCount());
        }
        try (var restarted = session(settings(2, 12))) {
            assertEquals(1, await(restarted.start(false)).livingPeople());
            assertEquals(Resident.Status.DECEASED, restarted.projectionView().orElseThrow().population().residents().stream()
                    .filter(p -> p.id().equals(person)).findFirst().orElseThrow().status());
        }
    }

    @Test void workerImportsAndAgesWithoutGivingCallerDatabaseOrWorldObjects() throws Exception {
        try (var session = session(settings(2, 12))) {
            assertTrue(await(session.start(false)).ready());
            assertEquals(2, await(session.observe(observed())).villages().getFirst().importedPeople());
            now.addAndGet(90);
            var first = await(session.pulse());
            assertEquals(2, first.completedMonths()); assertEquals(1, first.remainingMonths());
            assertEquals(2, first.livingPeople());
            assertEquals(3, await(session.pulse()).completedMonths());
            assertTrue(session.snapshot().orElseThrow().residents().stream().allMatch(p -> p.ageSteps() == 243));
            assertEquals(0, await(session.observe(observed())).villages().getFirst().importedPeople());
        }
    }

    @Test void restartRetainsAgesFractionalTimeAndBacklogRatherThanDoubleAging() throws Exception {
        UUID resident;
        try (var session = session(settings(1, 12))) {
            await(session.start(false)); resident = await(session.observe(observed())).villages().getFirst().observedResidents().values().iterator().next().id();
            now.addAndGet(100); assertEquals(1, await(session.pulse()).completedMonths());
        }
        try (var reopened = session(settings(1, 12))) {
            assertEquals(2, await(reopened.start(false)).completedMonths());
            assertEquals(3, await(reopened.pulse()).completedMonths());
            assertEquals(3, await(reopened.pulse()).completedMonths());
            assertEquals(243, reopened.snapshot().orElseThrow().residents().stream().filter(p -> p.id().equals(resident)).findFirst().orElseThrow().ageSteps());
            now.addAndGet(20); assertEquals(4, await(reopened.pulse()).completedMonths());
        }
    }

    @Test void offlineCapIsSeparateFromBatchBudgetAndDoesNotDiscardPreviouslyQueuedMonths() throws Exception {
        try (var session = session(settings(1, 2))) {
            await(session.start(false)); now.addAndGet(150); assertEquals(4, await(session.pulse()).remainingMonths());
        }
        now.addAndGet(300);
        try (var reopened = session(settings(1, 2))) {
            var first = await(reopened.start(false));
            assertEquals(7, first.targetMonths()); assertEquals(2, first.completedMonths());
            while (reopened.status().remainingMonths() > 0) await(reopened.pulse());
            assertEquals(7, reopened.status().completedMonths());
        }
    }

    @Test void pendingCatchUpRejectsImportRatherThanAgingNewlySeenPeopleThroughOldHistory() throws Exception {
        try (var session = session(settings(1, 12))) {
            await(session.start(false)); now.addAndGet(150); await(session.pulse());
            var rejected = assertThrows(ExecutionException.class, () -> await(session.observe(observed())));
            assertInstanceOf(RejectedExecutionException.class, rejected.getCause());
            assertEquals(0, session.snapshot().orElseThrow().livingCount());
            while (session.status().remainingMonths() > 0) await(session.pulse());
            await(session.observe(observed()));
            assertTrue(session.snapshot().orElseThrow().residents().stream().allMatch(p -> p.ageSteps() == 240));
        }
    }

    @Test void allTimeAndDatabaseWorkRunOnDedicatedOwnerAndAutomaticTimerActuallyPulses() throws Exception {
        var callerThread = Thread.currentThread(); var ownerThread = new AtomicReference<Thread>();
        try (var session = new PopulationSession(directory.toFile(), Logger.getLogger("OwnerTest"), settings(2, 12), () -> {
            var previous = ownerThread.getAndSet(Thread.currentThread());
            if (previous != null) assertSame(previous, Thread.currentThread());
            return now.get();
        })) {
            await(session.start()); assertNotSame(callerThread, ownerThread.get());
            now.addAndGet(30);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (session.status().completedMonths() == 0 && System.nanoTime() < deadline) Thread.sleep(10);
            assertEquals(1, session.status().completedMonths());
            assertEquals("Civilizations-population", ownerThread.get().getName());
        }
    }

    @Test void cadenceAndSeedChangesFailWithoutReinterpretingSavedData() throws Exception {
        try (var session = session(settings(2, 12))) { await(session.start(false)); }
        var base = settings(2, 12);
        var changedCadence = new PopulationSession.Settings(42, 60, 12, 2, base.population(), base.migration(), base.recognition(), base.enrollment());
        try (var session = session(changedCadence)) { assertThrows(ExecutionException.class, () -> await(session.start(false))); }
        var changedSeed = new PopulationSession.Settings(99, 30, 12, 2, base.population(), base.migration(), base.recognition(), base.enrollment());
        try (var session = session(changedSeed)) { assertThrows(ExecutionException.class, () -> await(session.start(false))); }
        try (var session = session(base)) { assertTrue(await(session.start(false)).ready()); }
    }

    @Test void failedDemographicWriteRetainsQueuedTimeAndRetryProducesOriginalTarget() throws Exception {
        try (var session = session(settings(2, 12))) {
            await(session.start(false)); await(session.observe(observed()));
            // Test-owned temporary database only; install failure while the manual scheduler is idle.
            try (var external = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("civilizations.db")); var statement = external.createStatement()) {
                statement.execute("CREATE TRIGGER fail_runtime BEFORE UPDATE ON demographic_clock BEGIN SELECT RAISE(ABORT, 'injected'); END");
            }
            now.addAndGet(60);
            assertThrows(ExecutionException.class, () -> await(session.pulse()));
            assertEquals(2, session.status().targetMonths()); assertEquals(0, session.status().completedMonths());
            assertNotNull(session.status().failure());
            try (var external = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("civilizations.db")); var statement = external.createStatement()) {
                statement.execute("DROP TRIGGER fail_runtime");
            }
            assertEquals(2, await(session.pulse()).completedMonths());
            assertNull(session.status().failure());
            assertTrue(session.snapshot().orElseThrow().residents().stream().allMatch(p -> p.ageSteps() == 242));
        }
    }

    @Test void closingRejectsNewWorkAndRepeatedStartAndCloseAreSafe() throws Exception {
        var session = session(settings(2, 12));
        await(session.start(false));
        assertThrows(ExecutionException.class, () -> await(session.start(false)));
        session.close(); session.close();
        var rejected = assertThrows(ExecutionException.class, () -> await(session.pulse()));
        assertInstanceOf(RejectedExecutionException.class, rejected.getCause());
        assertThrows(IllegalArgumentException.class, () -> settings(0, 12));
        assertThrows(ArithmeticException.class, () -> new PopulationSession.Settings(42, Long.MAX_VALUE, 2, 1,
                PopulationPolicy.prototype(), MigrationPolicy.disabled(), VillageRecognition.Policy.prototype(), VillageEnrollmentRepository.Policy.prototype()));
    }

    @Test void completionCallbackCanCloseSessionWithoutWaitingForItsOwnThread() throws Exception {
        var session = session(settings(2, 12));
        try {
            session.start(false).thenAccept(ignored -> session.close()).get(2, TimeUnit.SECONDS);
            assertFalse(session.status().ready());
            assertThrows(ExecutionException.class, () -> await(session.pulse()));
        } finally { session.close(); }
    }

    @Test void overloadedQueueExplicitlyRejectsExcessWorkRatherThanAccumulatingWithoutBound() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var session = new PopulationSession(directory.toFile(), Logger.getLogger("QueueTest"), settings(2, 12), () -> {
            entered.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timed out"); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            return now.get();
        })) {
            try {
                var startup = session.start(false); assertTrue(entered.await(5, TimeUnit.SECONDS));
                var work = new java.util.ArrayList<CompletableFuture<PopulationSession.Status>>();
                for (int i = 0; i < 65; i++) work.add(session.pulse());
                assertTrue(work.stream().anyMatch(CompletableFuture::isCompletedExceptionally));
                release.countDown(); await(startup);
                for (var future : work) {
                    if (future.isCompletedExceptionally()) {
                        var failure = assertThrows(ExecutionException.class, () -> await(future));
                        assertInstanceOf(RejectedExecutionException.class, failure.getCause());
                    } else assertEquals(0, await(future).completedMonths());
                }
            } finally { release.countDown(); }
        }
    }
}
