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
    @Test void explicitBuildingExclusionRequiresConfirmationAndRetainsHistoryAfterOwnerRelease()throws Exception {
        var world=observed().worldId();var player=new UUID(0,81);
        var bounds=new com.blueninjas24.civilizations.construction.BuildingLayout.Bounds(
                new com.blueninjas24.civilizations.construction.ConstructionTemplate.Position(100,64,0),
                new com.blueninjas24.civilizations.construction.ConstructionTemplate.Position(104,68,4));
        var request=new com.blueninjas24.civilizations.database.BuildingRegistry.Exclusion(new UUID(0,82),world,player,bounds,1000);
        try(var session=session(settings(2,12))) {
            await(session.start(false,java.util.Set.of(world)));
            var refused=assertThrows(ExecutionException.class,()->await(session.protectBuilding(request,"Fixture",false)));
            assertInstanceOf(com.blueninjas24.civilizations.database.BuildingRegistry.Declined.class,refused.getCause());assertNull(session.status().failure());
            assertEquals(request,await(session.protectBuilding(request,"Fixture",true)));
            assertEquals(List.of(request.id()),await(session.buildingExclusionsAt(world,102,2)));
            assertEquals(request,await(session.buildingExclusion(request.id())).orElseThrow());
            assertThrows(ExecutionException.class,()->await(session.releaseBuildingExclusion(new UUID(0,83),request.id(),new UUID(0,99),world,false,1000)));
            assertThrows(ExecutionException.class,()->await(session.releaseBuildingExclusion(new UUID(0,83),request.id(),player,new UUID(0,99),false,1000)));
            assertNull(session.status().failure());
            assertNull(await(session.releaseBuildingExclusion(new UUID(0,83),request.id(),player,world,false,1000)));
            assertNull(await(session.releaseBuildingExclusion(new UUID(0,83),request.id(),player,world,false,1000)));
            assertTrue(await(session.buildingExclusionsAt(world,102,2)).isEmpty());
            assertEquals(request,await(session.buildingExclusion(request.id())).orElseThrow());
        }
    }
    @Test void playerBuildingDeclarationUsesOwnerQueueAndExpectedRefusalsDoNotMarkSimulationFailed()throws Exception {
        var world=observed().worldId();var player=new UUID(0,91);
        try(var session=session(settings(2,12))) {
            await(session.start(false,java.util.Set.of(world)));await(session.observe(observed()));
            var town=session.settlementNames().keySet().iterator().next();
            var bounds=new com.blueninjas24.civilizations.construction.BuildingLayout.Bounds(
                    new com.blueninjas24.civilizations.construction.ConstructionTemplate.Position(100,64,0),
                    new com.blueninjas24.civilizations.construction.ConstructionTemplate.Position(104,67,4));
            var entrance=new com.blueninjas24.civilizations.construction.ConstructionTemplate.Position(100,65,0);
            var unit=new com.blueninjas24.civilizations.construction.BuildingLayout.Unit(new UUID(0,92),entrance,List.of(bounds),
                    List.of(new com.blueninjas24.civilizations.construction.BuildingLayout.Bed(
                            new com.blueninjas24.civilizations.construction.ConstructionTemplate.Position(101,65,2),
                            com.blueninjas24.civilizations.construction.ConstructionTemplate.Direction.EAST)));
            var layout=new com.blueninjas24.civilizations.construction.BuildingLayout(new UUID(0,93),town,world,
                    com.blueninjas24.civilizations.construction.BuildingLayout.Kind.HOUSING,bounds,bounds,entrance,List.of(),List.of(unit));
            var preview=await(session.previewBuildingDonation(player,world,layout,true));
            var refused=assertThrows(ExecutionException.class,()->await(session.confirmBuildingConsent(player,world,preview.token(),false,true,true,false)));
            assertInstanceOf(com.blueninjas24.civilizations.database.BuildingRegistry.Declined.class,refused.getCause());assertNull(session.status().failure());
            await(session.updateLoadedWorlds(java.util.Set.of()));
            assertThrows(ExecutionException.class,()->await(session.confirmBuildingConsent(player,world,preview.token(),true,true,true,false)));
            assertNull(session.status().failure());await(session.updateLoadedWorlds(java.util.Set.of(world)));
            var saved=await(session.confirmBuildingConsent(player,world,preview.token(),true,true,true,false));
            assertFalse(saved.administrator());
            assertEquals(saved,await(session.confirmBuildingConsent(player,world,preview.token(),true,true,true,false)));
            var reclaim=await(session.previewBuildingReclamation(player,world,layout.id(),true,false));
            assertEquals(com.blueninjas24.civilizations.database.BuildingRegistry.Action.RECLAIM,
                    await(session.confirmBuildingConsent(player,world,reclaim.token(),false,true,true,false)).action());
            assertNull(session.status().failure());
        }
    }
    @Test void penProtectionAndReleaseUseOwnerQueueWithoutReportingExpectedRefusalsAsFailures()throws Exception {
        var world=observed().worldId();var player=new UUID(0,77);var penId=new UUID(0,78);
        var scan=new com.blueninjas24.civilizations.industry.PenScan(p->Math.abs(p.x())==2||Math.abs(p.z())==2
                ?com.blueninjas24.civilizations.industry.PenScan.Cell.BARRIER:com.blueninjas24.civilizations.industry.PenScan.Cell.INTERIOR,
                new com.blueninjas24.civilizations.industry.PenScan.Column(0,0),new com.blueninjas24.civilizations.industry.PenScan.Limits(4,16));
        while(!scan.complete())scan.advance(8);
        var pen=new com.blueninjas24.civilizations.industry.PenProtection(penId,world,player,64,3,64,0,
                new com.blueninjas24.civilizations.industry.PenScan.Column(0,0),scan.evidence(),java.util.Set.of(),1000);
        try(var session=session(settings(2,12))){
            await(session.start(false));await(session.observe(observed()));
            var refused=assertThrows(ExecutionException.class,()->await(session.protectPen(pen,false)));
            assertInstanceOf(com.blueninjas24.civilizations.database.PenProtectionLifecycle.Declined.class,refused.getCause());
            assertNull(session.status().failure());assertEquals(pen,await(session.protectPen(pen,true)));
            var request=new com.blueninjas24.civilizations.database.PenProtectionLifecycle.Release(new UUID(0,79),penId,player,2000,"Explicit owner release");
            assertEquals(request,await(session.releasePen(request,false)));assertEquals(pen,await(session.protectedPen(penId)).orElseThrow());
            assertNull(session.status().failure());
        }
    }
    @Test void currentSurveyReviewUsesOwnerClockAndWorldAvailabilityThenRetainsEmptyFrame()throws Exception {
        var world=observed().worldId();UUID town;
        try(var session=session(settings(2,12))) {
            session.configureOreTracking();await(session.start(false,java.util.Set.of(world)));await(session.observe(observed()));
            town=session.settlementNames().keySet().iterator().next();
            var position=new VillageObservation.Position(0,40,0);
            var coal=new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.COAL,List.of(position));
            var iron=new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.IRON,List.of());
            await(session.surveyMinerals(town,List.of(coal,iron)));
            assertEquals(1,await(session.currentMineralOpportunities(town,10)).candidates().size());
            await(session.updateLoadedWorlds(java.util.Set.of()));
            assertEquals(com.blueninjas24.civilizations.database.MineralOpportunities.Status.INACTIVE_WORLD,await(session.currentMineralOpportunities(town,10)).status());
            await(session.updateLoadedWorlds(java.util.Set.of(world)));now.set(1011);
            assertEquals(com.blueninjas24.civilizations.database.MineralOpportunities.Status.STALE,await(session.currentMineralOpportunities(town,10)).status());
            await(session.surveyMinerals(town,List.of(new com.blueninjas24.civilizations.industry.MineralObservation(world,1011,true,coal.mineral(),List.of()),
                new com.blueninjas24.civilizations.industry.MineralObservation(world,1011,true,iron.mineral(),List.of()))));
            assertTrue(await(session.currentMineralOpportunities(town,10)).candidates().isEmpty());
        }
        try(var session=session(settings(2,12))) {
            session.configureOreTracking();await(session.start(false,java.util.Set.of(world)));
            assertEquals(1011,await(session.currentMineralOpportunities(town,10)).capturedMillis());
            assertTrue(await(session.currentMineralOpportunities(town,10)).candidates().isEmpty());
            session.invalidateOreTracking("Fixture gap");assertThrows(ExecutionException.class,()->await(session.currentMineralOpportunities(town,10)));
        }
    }
    @Test void nativeMutationFreezesInputAndDrainsAcceptedWorkAtShutdown()throws Exception {
        var world=UUID.randomUUID();var position=new VillageObservation.Position(-1,40,-1);
        var removals=new java.util.ArrayList<>(List.of(position));var session=session(settings(2,12));session.configureOreTracking();await(session.start(false));
        var accepted=session.recordOreMutations(world,"Unenrolled fixture",List.of(),removals,1000,"Native removal");removals.clear();session.close();assertEquals(0,await(accepted).retiredBatches());
        try(var reopened=session(settings(2,12))) {
            reopened.configureOreTracking();await(reopened.start(false));assertTrue(reopened.oreDiscoveryReady());
            assertEquals(java.util.Set.of(position),await(reopened.orePlacementsInChunk(world,-1,-1,512)));
        }
    }
    @Test void rejectedNativeMutationPersistsGapAndHealthyCloseCannotRepairIt()throws Exception {
        var session=session(settings(2,12));session.configureOreTracking();await(session.start(false));
        assertThrows(ExecutionException.class,()->await(session.recordOreMutations(UUID.randomUUID(),"Fixture",List.of(),List.of(),1000,"Empty mutation")));
        assertFalse(session.oreDiscoveryReady());session.close();
        try(var reopened=session(settings(2,12))) {reopened.configureOreTracking();await(reopened.start(false));assertFalse(reopened.oreDiscoveryReady());}
    }
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
    @Test void liveSurveyUsesOwnerPlacementHistoryAndRejectsCaptureGaps() throws Exception {
        try(var session=session(settings(2,12))) {
            session.configureOreTracking();await(session.start(false));
            await(session.observe(observed()));var town=session.settlementNames().keySet().iterator().next();
            var world=observed().worldId();var position=new VillageObservation.Position(0,40,0);
            var coal=new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.COAL,List.of(position));
            var iron=new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.IRON,List.of());
            assertEquals(1,await(session.surveyMinerals(town,List.of(coal,iron))).size());
            await(session.recordOrePlacements(world,"test-world",List.of(position),1000));
            assertTrue(await(session.surveyMinerals(town,List.of(coal,iron))).isEmpty());
            session.invalidateOreTracking("injected gap");
            assertThrows(ExecutionException.class,()->await(session.surveyMinerals(town,List.of(coal,iron))));
        }
    }
    @Test void liveSurveyRefusesAbsentWorldAndResumesAfterWorldReturns() throws Exception {
        var world=observed().worldId();
        try(var session=session(settings(2,12))) {
            session.configureOreTracking();await(session.start(false,java.util.Set.of(world)));
            await(session.observe(observed()));var town=session.settlementNames().keySet().iterator().next();
            var pair=List.of(new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.COAL,List.of()),
                    new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.IRON,List.of()));
            await(session.updateLoadedWorlds(java.util.Set.of()));
            assertThrows(ExecutionException.class,()->await(session.surveyMinerals(town,pair)));
            await(session.updateLoadedWorlds(java.util.Set.of(world)));assertTrue(await(session.surveyMinerals(town,pair)).isEmpty());
        }
    }
    @Test void savedOpportunityDrainsAtShutdownAndReturnsWithRemovalExclusionAfterRestart()throws Exception {
        var world=observed().worldId();var position=new VillageObservation.Position(0,40,0);UUID town;
        var session=session(settings(2,12));session.configureOreTracking();await(session.start(false));await(session.observe(observed()));
        town=session.settlementNames().keySet().iterator().next();
        var evidence=List.of(new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.COAL,List.of(position)),
                new com.blueninjas24.civilizations.industry.MineralObservation(world,1000,true,com.blueninjas24.civilizations.economy.Commodity.IRON,List.of()));
        var saved=session.surveyMinerals(town,evidence);session.close();assertEquals(1,await(saved).size());
        try(var reopened=session(settings(2,12))) {
            reopened.configureOreTracking();await(reopened.start(false));assertTrue(reopened.oreDiscoveryReady());
            var row=await(reopened.mineralOpportunities(town,512)).getFirst();assertEquals(position,row.position());assertEquals(1000,row.lastSeenMillis());assertFalse(row.excluded());
            await(reopened.recordOreMutations(world,"test-world",List.of(),List.of(position),1001,"Fixture break"));
            assertTrue(await(reopened.mineralOpportunities(town,512)).getFirst().excluded());assertTrue(await(reopened.surveyMinerals(town,evidence)).isEmpty());
        }
    }
    @Test void captureGapBeforeStartupCannotBeOverwrittenBySuccessfulInitialization() throws Exception {
        try(var session=session(settings(2,12))) {
            session.configureOreTracking();session.invalidateOreTracking("early capture gap");await(session.start(false));
            assertFalse(session.oreDiscoveryReady());
            assertTrue(session.oreTrackingDiagnostic().contains("early capture gap"));
        }
        try(var reopened=session(settings(2,12))) {
            reopened.configureOreTracking();await(reopened.start(false));assertFalse(reopened.oreDiscoveryReady());
        }
    }
    @Test void configuredPlacementCaptureDrainsThenCleanRestartKeepsDiscoveryFenceHealthy() throws Exception {
        var world=UUID.randomUUID();var position=new VillageObservation.Position(0,40,0);
        var session=session(settings(2,12));session.configureOreTracking();await(session.start(false));
        assertTrue(session.oreDiscoveryReady());var saved=session.recordOrePlacements(world,"Fixture",List.of(position),1000);
        session.close();assertEquals(1,await(saved));
        try(var reopened=session(settings(2,12))) {
            reopened.configureOreTracking();await(reopened.start(false));assertTrue(reopened.oreDiscoveryReady());
            assertEquals(java.util.Set.of(position),await(reopened.orePlacementsInChunk(world,0,0,512)));
        }
    }
    @Test void captureFailurePersistsDiscoveryPauseButRetainsPopulationAndCanStillSaveExclusions() throws Exception {
        var session=session(settings(2,12));session.configureOreTracking();await(session.start(false));await(session.observe(observed()));
        var failed=session.recordOrePlacements(observed().worldId(),"Fixture",List.of(),1000);
        assertThrows(ExecutionException.class,()->failed.get(5,TimeUnit.SECONDS));assertFalse(session.oreDiscoveryReady());session.close();
        try(var reopened=session(settings(2,12))) {
            reopened.configureOreTracking();await(reopened.start(false));assertFalse(reopened.oreDiscoveryReady());
            assertEquals(2,reopened.snapshot().orElseThrow().livingCount());
            assertEquals(1,await(reopened.recordOrePlacements(observed().worldId(),"Fixture",List.of(new VillageObservation.Position(0,40,0)),1000)));
            assertTrue(reopened.oreTrackingDiagnostic().contains("recovery"));
        }
    }
    @Test void placementOwnerSavesBeforeTownEnrollmentAndDrainsAtShutdown() throws Exception {
        var world=UUID.randomUUID(); var position=new VillageObservation.Position(-1,-60,-1);
        var evidence=new java.util.ArrayList<>(List.of(position));
        var session=session(settings(2,12));await(session.start(false));
        var saved=session.recordOrePlacements(world,"Unenrolled world",evidence,1000);
        evidence.clear();session.close();assertEquals(1,await(saved));
        try(var reopened=session(settings(2,12))) {
            await(reopened.start(false));
            assertEquals(java.util.Set.of(position),await(reopened.orePlacementsInChunk(world,-1,-1,512)));
            assertTrue(reopened.settlementNames().isEmpty());
        }
    }
    @Test void acceptedShutdownDeathBatchDrainsBeforeDatabaseClose() throws Exception {
        var observation = observed();
        var session = session(settings(2, 12));
        await(session.start(false)); await(session.observe(observation));
        var saved = session.flushPhysicalDeaths(observation.villagers().stream().map(person ->
                new PopulationSession.DeathObservation(observation.worldId(), person.entityId(), 1200, "FIRE")).toList());
        session.close(); assertTrue(await(saved));
        try (var reopened = session(settings(2, 12))) {
            assertEquals(0, await(reopened.start(false)).livingPeople());
            assertEquals(2, reopened.projectionView().orElseThrow().recentDeaths().size());
        }
    }

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
    @Test void stoppingImmediatelyDuringAutomaticStartupDrainsAndClosesCaptureCleanly()throws Exception {
        var session=session(settings(2,12));var startup=session.start(true);session.close();
        assertDoesNotThrow(()->startup.get(5,TimeUnit.SECONDS));assertFalse(session.status().ready());assertNull(session.status().failure());
        try(var external=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"));var s=external.createStatement();var r=s.executeQuery("SELECT clean_stop,trusted FROM livestock_capture WHERE id=1")) {
            assertTrue(r.next());assertEquals(1,r.getInt(1));assertEquals(1,r.getInt(2));
        }
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
