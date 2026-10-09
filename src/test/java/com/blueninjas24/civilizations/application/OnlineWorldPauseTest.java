package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class OnlineWorldPauseTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong(1000);
    private final UUID first = new UUID(0, 10), second = new UUID(0, 20);
    private PopulationSession session(int budget) {
        var settings = new PopulationSession.Settings(42, 30, 12, budget,
                new PopulationPolicy(12,216,240,480,720,0,0,0), MigrationPolicy.disabled(),
                VillageRecognition.Policy.prototype(), VillageEnrollmentRepository.Policy.prototype());
        var bands = new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for (var condition : CommodityAccounting.Condition.values()) bands.put(condition,100);
        var economy = new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,java.math.BigDecimal.valueOf(2)),bands);
        return new PopulationSession(directory.toFile(), Logger.getLogger("OnlinePauseTest"), settings, now::get,
                economy, FarmObservation.Policy.prototype());
    }
    private static <T> T await(CompletableFuture<T> result) throws Exception { return result.get(5, TimeUnit.SECONDS); }
    private UUID enroll(PopulationSession session, UUID world) throws Exception {
        var bed = new VillageObservation.Position(0,64,0);
        var other = new VillageObservation.Position(2,64,0);
        var result = await(session.observe(new VillageObservation(world,"world-"+world,now.get(),true,
                List.of(bed,other), List.of(new VillageObservation.Person(new UUID(1,world.getLeastSignificantBits()),bed,true,"Resident"),
                        new VillageObservation.Person(new UUID(2,world.getLeastSignificantBits()),other,true,"Second resident")))));
        assertTrue(result.failures().isEmpty());
        return result.villages().getFirst().observedResidents().values().iterator().next().id();
    }
    private long age(PopulationSession session, UUID person) {
        return session.snapshot().orElseThrow().residents().stream().filter(p -> p.id().equals(person)).findFirst().orElseThrow().ageSteps();
    }
    @Test void absentWorldDoesNotAgeWhileOtherWorldContinuesAndReturningDoesNotCatchUp() throws Exception {
        try (var session = session(120)) {
            await(session.start(false,Set.of(first,second)));
            var a = enroll(session,first); var b = enroll(session,second);
            now.addAndGet(30); await(session.pulse());
            assertEquals(241,age(session,a)); assertEquals(241,age(session,b));
            await(session.updateLoadedWorlds(Set.of(first)));
            now.addAndGet(3000); await(session.pulse());
            assertEquals(341,age(session,a)); assertEquals(241,age(session,b));
            await(session.updateLoadedWorlds(Set.of(first,second)));
            await(session.pulse()); assertEquals(241,age(session,b));
            now.addAndGet(30); await(session.pulse()); assertEquals(242,age(session,b));
        }
    }
    @Test void allWorldsAbsentAndServerRestartPreserveAgeAndPartialOnlineMonth() throws Exception {
        UUID person;
        try (var session = session(120)) {
            await(session.start(false,Set.of(first))); person = enroll(session,first);
            now.addAndGet(19); await(session.pulse());
            await(session.updateLoadedWorlds(Set.of()));
            now.addAndGet(1_000_000); assertEquals(0,await(session.pulse()).completedMonths());
        }
        now.addAndGet(100_000_000);
        try (var session = session(120)) {
            assertEquals(0,await(session.start(false,Set.of(first))).completedMonths());
            assertEquals(240,age(session,person));
            now.addAndGet(11); assertEquals(1,await(session.pulse()).completedMonths()); assertEquals(241,age(session,person));
        }
    }
    @Test void savedWorldMasksRetainAlreadyEarnedBoundedQueueAcrossWorldChangeAndRestart() throws Exception {
        UUID a,b;
        try (var session = session(1)) {
            await(session.start(false,Set.of(first,second))); a=enroll(session,first); b=enroll(session,second);
            now.addAndGet(90); assertEquals(2,await(session.pulse()).remainingMonths());
            await(session.updateLoadedWorlds(Set.of(first))); // second already earned these three months
            now.addAndGet(60); await(session.pulse());
        }
        now.addAndGet(100_000_000);
        try (var session = session(1)) {
            assertEquals(5,await(session.start(false,Set.of(first))).targetMonths());
            while(session.status().remainingMonths()>0) await(session.pulse());
            assertEquals(245,age(session,a)); assertEquals(243,age(session,b));
        }
    }
    @Test void foodStocksAndEconomicPeriodsPauseThroughAbsenceAndDowntime() throws Exception {
        UUID town;
        long period;
        Object stock;
        try (var session=session(120)) {
            await(session.start(false,Set.of(first))); var person=enroll(session,first);
            town=session.snapshot().orElseThrow().residents().stream().filter(p->p.id().equals(person)).findFirst().orElseThrow().settlementId();
            await(session.observeFarms(new FarmObservation(town,first,now.get(),true,0)));
            now.addAndGet(2000); await(session.pulse());
            await(session.pulse()); // Finish both periods already earned before pausing.
            await(session.updateLoadedWorlds(Set.of()));
            var before=session.economyView().get(town).account().state(); period=before.completedPeriods(); stock=before.stocks();
            assertEquals(2,period);
            now.addAndGet(100_000_000); await(session.pulse());
            assertEquals(before,session.economyView().get(town).account().state());
        }
        now.addAndGet(100_000_000);
        try (var session=session(120)) {
            await(session.start(false,Set.of(first)));
            var after=session.economyView().get(town).account().state();
            assertEquals(period,after.completedPeriods()); assertEquals(stock,after.stocks());
        }
    }
}
