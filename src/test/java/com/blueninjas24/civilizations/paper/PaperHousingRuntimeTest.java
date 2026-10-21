package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession.HousingPage;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperHousingRuntimeTest {
    private BuildingLayout layout() {
        var bounds=new BuildingLayout.Bounds(new Position(0,0,0),new Position(4,3,4));var entrance=new Position(1,1,0);
        return new BuildingLayout(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),BuildingLayout.Kind.HOUSING,bounds,bounds,entrance,List.of(),
                List.of(new BuildingLayout.Unit(UUID.randomUUID(),entrance,List.of(bounds),List.of(new BuildingLayout.Bed(new Position(2,1,2),Direction.EAST)))));
    }
    @Test void eightQueuedHomesShareOneBudgetAndOnlyOneAcceptedSaveCanBeInFlight() {
        var layouts=new ArrayList<BuildingLayout>();for(int i=0;i<8;i++)layouts.add(layout());
        var time=new AtomicLong(1000);var pages=new AtomicInteger();var reads=new AtomicInteger();var reviews=new AtomicInteger();
        var pending=new CompletableFuture<DwellingHousing.Report>();
        var owner=new PaperHousingRuntime.Owner(){
            public CompletableFuture<HousingPage> page(UUID after){pages.incrementAndGet();return CompletableFuture.completedFuture(new HousingPage(layouts,null));}
            public CompletableFuture<DwellingHousing.Report> review(BuildingLayout layout,DwellingInspection.Observation frame){reviews.incrementAndGet();return pending;}
        };
        var runtime=new PaperHousingRuntime(owner,new PaperHousingRuntime.Settings(true,32,15),(layout,started)->(budget,now)->{
            reads.addAndGet(budget);return Optional.of(new DwellingInspection.Observation(layout.world(),layout.fingerprint(),now,false,Map.of()));
        },time::get,Logger.getAnonymousLogger());
        runtime.run();runtime.run();assertEquals(32,reads.get());assertEquals(1,reviews.get());
        for(int i=0;i<100;i++)runtime.run();assertEquals(32,reads.get());assertEquals(1,pages.get());
        runtime.close();assertFalse(pending.isCancelled());pending.complete(new DwellingHousing.Report(DwellingOccupancy.Outcome.UNVERIFIED,0,0,"Unknown"));
        runtime.run();assertEquals(32,reads.get());
    }
    @Test void idlePaginationWaitsForReviewIntervalAndDisabledRuntimeDoesNoWork() {
        var calls=new AtomicInteger();var time=new AtomicLong(1000);
        var owner=new PaperHousingRuntime.Owner(){
            public CompletableFuture<HousingPage> page(UUID after){calls.incrementAndGet();return CompletableFuture.completedFuture(new HousingPage(List.of(),null));}
            public CompletableFuture<DwellingHousing.Report> review(BuildingLayout l,DwellingInspection.Observation o){throw new AssertionError("No homes");}
        };
        var runtime=new PaperHousingRuntime(owner,new PaperHousingRuntime.Settings(true,128,15),(l,t)->{throw new AssertionError();},time::get,Logger.getAnonymousLogger());
        runtime.run();runtime.run();for(int i=0;i<100;i++)runtime.run();assertEquals(1,calls.get());
        time.set(16_000);runtime.run();assertEquals(2,calls.get());runtime.close();
        var off=new PaperHousingRuntime(owner,new PaperHousingRuntime.Settings(false,128,15),(l,t)->{throw new AssertionError();},time::get,Logger.getAnonymousLogger());
        off.run();assertEquals(2,calls.get());off.close();
    }
    @Test void editableSettingsKeepDefaultsAndRejectInvalidBudgets()throws Exception {
        var config=new YamlConfiguration();assertEquals(new PaperHousingRuntime.Settings(true,128,15),PaperHousingRuntime.Settings.read(config));
        config.loadFromString("housing-runtime:\n  enabled: false\n  cells-per-tick: 64\n  review-seconds: 60\n");
        assertEquals(new PaperHousingRuntime.Settings(false,64,60),PaperHousingRuntime.Settings.read(config));
        config.set("housing-runtime.cells-per-tick",0);assertThrows(IllegalArgumentException.class,()->PaperHousingRuntime.Settings.read(config));
    }
}
