package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.database.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** One loaded-only native survey and owner save at a time. Prepares unused projects; never buys or builds. */
public final class PaperAutomaticHomePlanning extends BukkitRunnable implements AutoCloseable {
    public interface Survey {Optional<PaperHomeSiteSurvey.Frame> advance(int budget,long now);}
    public interface Owner {
        CompletableFuture<HousingPlanningJobs.Page> page(UUID after);
        CompletableFuture<HousingProjectPreparation.Receipt> prepare(HousingPlanningJobs.Job job,PaperHomeSiteSurvey.Frame frame,long now);
    }
    private final Owner owner;private final AutomaticHomePlanningSettings settings;
    private final BiFunction<HousingPlanningJobs.Job,Long,Survey> surveys;private final LongSupplier clock;
    private final Consumer<String> diagnostic;private final Logger logger;private final ArrayDeque<HousingPlanningJobs.Job> waiting=new ArrayDeque<>();
    private UUID cursor;private long nextPage;private boolean closed;private HousingPlanningJobs.Job current;private Survey survey;
    private CompletableFuture<HousingPlanningJobs.Page> page;private CompletableFuture<HousingProjectPreparation.Receipt> saving;
    public PaperAutomaticHomePlanning(PopulationSession session,PaperConstructionLibrary library,AutomaticHomePlanningSettings settings,DwellingInspection.Policy policy,Logger logger){
        this(source(session,library,settings,policy),settings,(job,now)->new PaperHomeSiteSurvey(job.world(),job.need(),settings.searchRadius(),job.templateHeight(),now)::advance,
                System::currentTimeMillis,session::publishHomePlanningDiagnostic,logger);
    }
    private static Owner source(PopulationSession session,PaperConstructionLibrary library,AutomaticHomePlanningSettings settings,DwellingInspection.Policy policy){
        var templates=List.copyOf(library.templates().values());return new Owner(){
            public CompletableFuture<HousingPlanningJobs.Page> page(UUID after){return session.housingPlanningJobs(after,8,templates,library.housing(),policy,settings.maximumActiveHomes());}
            public CompletableFuture<HousingProjectPreparation.Receipt> prepare(HousingPlanningJobs.Job job,PaperHomeSiteSurvey.Frame frame,long now){
                var request=new HousingProjectPreparation.Request(new HousingProjects.Proposal(job.proposal(),job.household(),now),job.village(),job.world(),job.need(),frame.search(),frame.survey(),frame.terrain(),frame.states());
                return session.prepareLimitedHousingProject(request,templates,library.housing(),library.land(),policy,settings.maximumActiveHomes());
            }
        };
    }
    public PaperAutomaticHomePlanning(Owner owner,AutomaticHomePlanningSettings settings,BiFunction<HousingPlanningJobs.Job,Long,Survey> surveys,
                                     LongSupplier clock,Consumer<String> diagnostic,Logger logger){
        this.owner=Objects.requireNonNull(owner);this.settings=Objects.requireNonNull(settings);this.surveys=Objects.requireNonNull(surveys);
        this.clock=Objects.requireNonNull(clock);this.diagnostic=Objects.requireNonNull(diagnostic);this.logger=Objects.requireNonNull(logger);
        diagnostic.accept(settings.enabled()?"Home planning: waiting for verified housing need; physical work uses separate automatic-construction settings.":"Home planning: disabled; existing construction.yml files opt in explicitly.");
    }
    @Override public void run(){
        if(closed||!settings.enabled())return;long now=clock.getAsLong();
        try{
            if(saving!=null){
                if(!saving.isDone())return;var receipt=saving.join();saving=null;current=null;survey=null;
                diagnostic.accept("Home planning: retained unused project "+receipt.draft().id()+"; real materials, labor and native construction still required.");
            }
            if(page!=null){
                if(!page.isDone())return;var result=page.join();page=null;cursor=result.next();waiting.addAll(result.jobs());
                if(cursor==null)nextPage=now+settings.reviewSeconds()*1000L;
                if(waiting.isEmpty())diagnostic.accept("Home planning: reviewed "+result.reviewed()+" villages; absent "+result.absent()+"; at project cap "+result.projectHolds()+"; no new verified proposal.");
            }
            if(current==null){
                current=waiting.pollFirst();
                if(current==null){if(now>=nextPage)page=owner.page(cursor);return;}
                survey=surveys.apply(current,now);
            }
            var frame=survey.advance(settings.cellsPerTick(),now);
            if(frame.isEmpty())return;
            if(!frame.orElseThrow().survey().complete()){
                diagnostic.accept("Home planning: incomplete or changed loaded-area evidence; no project created.");current=null;survey=null;return;
            }
            saving=owner.prepare(current,frame.orElseThrow(),now);
        }catch(CompletionException|CancellationException|IllegalArgumentException|ArithmeticException failure){
            Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
            diagnostic.accept("Home planning paused: "+(cause instanceof BuildingRegistry.Declined?cause.getMessage():"review unavailable; saved projects retained")+".");
            if(!(cause instanceof BuildingRegistry.Declined||cause instanceof CancellationException))logger.log(Level.WARNING,"Automatic home preparation paused; retained history unchanged",cause);
            boolean villageFailure=current!=null;
            saving=null;current=null;survey=null;
            // A legitimate local refusal (no road/site, changed need/world) must not starve later villages.
            if(!villageFailure){page=null;waiting.clear();cursor=null;nextPage=now+settings.reviewSeconds()*1000L;}
        }
    }
    @Override public void close(){closed=true;waiting.clear();page=null;current=null;survey=null;saving=null;} // Accepted SQL saves drain independently.
}
