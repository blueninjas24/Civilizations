package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import com.blueninjas24.civilizations.database.DwellingHousing;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** One globally budgeted loaded-only survey. Never moves blocks/entities or starts construction. */
public final class PaperHousingRuntime extends BukkitRunnable implements AutoCloseable {
    public record Settings(boolean enabled,int cellsPerTick,int reviewSeconds) {
        public Settings{if(cellsPerTick<16||cellsPerTick>256||reviewSeconds<5||reviewSeconds>600)throw new IllegalArgumentException("Invalid housing-runtime limits");}
        public static Settings read(ConfigurationSection config) {
            var section=config.getConfigurationSection("housing-runtime");
            if(section==null){if(config.contains("housing-runtime"))throw new IllegalArgumentException("housing-runtime must be a section");return new Settings(true,128,15);}
            if(!Set.of("enabled","cells-per-tick","review-seconds").containsAll(section.getKeys(false)))throw new IllegalArgumentException("Unknown housing-runtime setting");
            Object enabled=section.get("enabled",true),cells=section.get("cells-per-tick",128),seconds=section.get("review-seconds",15);
            if(!(enabled instanceof Boolean flag)||!(cells instanceof Integer)||!(seconds instanceof Integer))throw new IllegalArgumentException("Housing settings need boolean enabled and whole-number limits");
            return new Settings(flag,(Integer)cells,(Integer)seconds);
        }
    }
    public interface Owner {
        CompletableFuture<PopulationSession.HousingPage> page(UUID after);
        CompletableFuture<DwellingHousing.Report> review(BuildingLayout layout,DwellingInspection.Observation observation);
    }
    private final Owner owner;private final Settings settings;private final BiFunction<BuildingLayout,Long,PaperDwellingReview.Survey> surveys;
    private final LongSupplier clock;private final Logger logger;
    private final ArrayDeque<BuildingLayout> waiting=new ArrayDeque<>();
    private UUID cursor;private long nextPage;private boolean closed;
    private CompletableFuture<PopulationSession.HousingPage> page;private CompletableFuture<DwellingHousing.Report> saving;
    private BuildingLayout current;private PaperDwellingReview.Survey survey;
    public PaperHousingRuntime(PopulationSession session,Settings settings,DwellingInspection.Policy policy,Logger logger) {
        this(new Owner(){
            public CompletableFuture<PopulationSession.HousingPage> page(UUID after){return session.housingPage(after,8);}
            public CompletableFuture<DwellingHousing.Report> review(BuildingLayout layout,DwellingInspection.Observation observation){return session.reconcileHousing(layout,observation,policy);}
        },settings,(layout,time)->new PaperDwellingSurvey(layout,time,true)::advance,System::currentTimeMillis,logger);
    }
    public PaperHousingRuntime(Owner owner,Settings settings,BiFunction<BuildingLayout,Long,PaperDwellingReview.Survey> surveys,LongSupplier clock,Logger logger) {
        this.owner=Objects.requireNonNull(owner);this.settings=Objects.requireNonNull(settings);this.surveys=Objects.requireNonNull(surveys);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);
    }
    @Override public void run() {
        if(closed||!settings.enabled())return;long time=clock.getAsLong();
        try {
            if(saving!=null){if(!saving.isDone())return;saving.join();saving=null;current=null;survey=null;}
            if(page!=null) {
                if(!page.isDone())return;var result=page.join();page=null;cursor=result.next();waiting.addAll(result.layouts());
                if(cursor==null)nextPage=time+settings.reviewSeconds()*1000L;
            }
            if(current==null) {
                current=waiting.pollFirst();
                if(current==null){if(time>=nextPage)page=owner.page(cursor);return;}
                survey=surveys.apply(current,time);
            }
            var frame=survey.advance(settings.cellsPerTick(),time);
            if(frame.isPresent())saving=owner.review(current,frame.get());
        }catch(CompletionException|CancellationException|IllegalArgumentException failure) {
            Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
            if(!(cause instanceof BuildingRegistry.Declined||cause instanceof CancellationException))logger.log(Level.WARNING,"Housing review paused; saved families retained",cause);
            saving=null;page=null;current=null;survey=null;waiting.clear();cursor=null;nextPage=time+settings.reviewSeconds()*1000L;
        }
    }
    @Override public void close(){closed=true;waiting.clear();page=null;current=null;survey=null;saving=null;}
}
