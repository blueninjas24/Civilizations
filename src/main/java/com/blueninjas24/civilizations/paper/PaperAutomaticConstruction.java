package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Purpose;
import com.blueninjas24.civilizations.database.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** One shared bounded survey/SQL handoff/native step at a time. Existing owner ledgers earn all work and cargo. */
public final class PaperAutomaticConstruction extends BukkitRunnable implements AutoCloseable {
    public interface Survey {Optional<PaperConstructionWorkSurvey.Frame> advance(int budget,long now);}
    public interface Owner {
        CompletableFuture<ConstructionWorkQueue.Page> page(UUID after);
        CompletableFuture<Boolean> procure(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame);
        CompletableFuture<PopulationSession.PreparedConstructionStep> prepare(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame,String after);
        CompletableFuture<Void> acknowledge(PopulationSession.PreparedConstructionStep prepared,long observedMillis);
        CompletableFuture<Void> complete(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame);
    }
    public interface NativeStep {
        String after(ConstructionWorkQueue.Work work);
        PaperConstructionStep.Outcome attempt(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame,PopulationSession.PreparedConstructionStep prepared);
    }
    private final Owner owner;private final AutomaticConstructionSettings settings;private final NativeStep nativeStep;
    private final BiFunction<ConstructionWorkQueue.Work,Long,Survey> surveys;private final LongSupplier clock;
    private final Consumer<String> diagnostic;private final Logger logger;private final ArrayDeque<ConstructionWorkQueue.Work> waiting=new ArrayDeque<>();
    private CompletableFuture<ConstructionWorkQueue.Page> page;
    private CompletableFuture<Boolean> purchasing;private CompletableFuture<Void> acknowledging,completing;
    private CompletableFuture<PopulationSession.PreparedConstructionStep> preparing;
    private UUID cursor;private long nextCycle,nextNative;private boolean closed,cycleProgress;
    private ConstructionWorkQueue.Work current;private Survey survey;private PaperConstructionWorkSurvey.Frame frame;
    private int nativeAttempts;
    private String lastWarning;

    public PaperAutomaticConstruction(JavaPlugin host,PopulationSession session,PaperConstructionLibrary library,Logger logger){
        this(host,session,library,System::currentTimeMillis,logger);
    }
    /** Use the same clock as the owner in accelerated fixtures; ordinary runtime uses wall time. */
    public PaperAutomaticConstruction(JavaPlugin host,PopulationSession session,PaperConstructionLibrary library,LongSupplier clock,Logger logger){
        this(source(session,library),library.automaticConstruction(),nativeStep(host,session),
                (work,now)->new PaperConstructionWorkSurvey(work,now)::advance,clock,session::publishConstructionWorkDiagnostic,logger);
    }
    private static Owner source(PopulationSession session,PaperConstructionLibrary library){return new Owner(){
        public CompletableFuture<ConstructionWorkQueue.Page> page(UUID after){return session.automaticHomeConstructionPage(after,8);}
        public CompletableFuture<Boolean> procure(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame){
            return session.procureVillageConstructionInputs(work.draft().id(),frame.authority(),library.funding(),library.supplies()).thenApply(result->!result.purchases().isEmpty());
        }
        public CompletableFuture<PopulationSession.PreparedConstructionStep> prepare(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame,String after){
            UUID operation=work.pending().map(ConstructionBlockJournal.Intent::operation).orElseGet(()->identity("block",work.draft().id(),work.nextOrdinal()));
            return session.prepareAuthorizedPublicConstructionStep(operation,work.draft().id(),work.nextOrdinal(),after,frame.authority());
        }
        public CompletableFuture<Void> acknowledge(PopulationSession.PreparedConstructionStep prepared,long observedMillis){var intent=prepared.intent();
            return session.acknowledgePublicConstructionStep(intent.operation(),intent.draft(),prepared.permit().world(),intent.after(),observedMillis)
                    .thenAccept(saved->{if(saved.state()!=ConstructionBlockJournal.State.APPLIED)throw new CompletionException(new BuildingRegistry.Declined("Native acknowledgement did not retain exact output"));});
        }
        public CompletableFuture<Void> complete(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame){
            return session.completeCivHome(identity("completion",work.draft().id(),0),work.draft().id(),new CivBuildingCompletion.Evidence(frame.authority(),frame.cells())).thenAccept(ignored->{});
        }
    };}
    private static NativeStep nativeStep(JavaPlugin host,PopulationSession session){var writer=new PaperConstructionWriter(host,session);return new NativeStep(){
        public String after(ConstructionWorkQueue.Work work){
            var draft=work.draft();var nativePlan=PaperConstructionLibrary.prepare(draft.template(),draft.transform());int ordinal=0;
            for(int stage=0;stage<draft.template().stages().size();stage++)for(var local:draft.template().stages().get(stage).blocks().keySet())
                if(ordinal++==work.nextOrdinal())return nativePlan.stages().get(stage).blocks().get(draft.transform().apply(local,draft.template().size()));
            throw new IllegalArgumentException("No native block at saved ordinal");
        }
        public PaperConstructionStep.Outcome attempt(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame,PopulationSession.PreparedConstructionStep prepared){return writer.attempt(work,frame,prepared);}
    };}
    private static UUID identity(String kind,UUID draft,int ordinal){return UUID.nameUUIDFromBytes(("automatic-home-work-v1|"+kind+"|"+draft+"|"+ordinal).getBytes(StandardCharsets.UTF_8));}
    public PaperAutomaticConstruction(Owner owner,AutomaticConstructionSettings settings,NativeStep nativeStep,
            BiFunction<ConstructionWorkQueue.Work,Long,Survey> surveys,LongSupplier clock,Consumer<String> diagnostic,Logger logger){
        this.owner=Objects.requireNonNull(owner);this.settings=Objects.requireNonNull(settings);this.nativeStep=Objects.requireNonNull(nativeStep);
        this.surveys=Objects.requireNonNull(surveys);this.clock=Objects.requireNonNull(clock);this.diagnostic=Objects.requireNonNull(diagnostic);this.logger=Objects.requireNonNull(logger);
        diagnostic.accept(settings.enabled()?"Home construction: waiting for saved projects and real supplies/labor.":"Home construction: disabled; add automatic-construction settings to opt in.");
    }
    @Override public void run(){
        if(closed||!settings.enabled())return;long now=clock.getAsLong();
        try{
            if(purchasing!=null){if(!purchasing.isDone())return;boolean changed=purchasing.join();cycleProgress|=changed;
                explain(changed?"supplies bought; waiting for earned delivery and labor":"supplies already paid; waiting for earned delivery and labor");finish();return;}
            if(acknowledging!=null){if(!acknowledging.isDone())return;acknowledging.join();cycleProgress=true;explain("saved block "+(current.nextOrdinal()+1)+"/"+current.draft().template().blockCount());finish();return;}
            if(completing!=null){if(!completing.isDone())return;completing.join();cycleProgress=true;explain("home completed; actual housing inspection will qualify beds and assign a family");finish();return;}
            if(preparing!=null){
                if(!preparing.isDone()||now<nextNative)return;var prepared=preparing.join();preparing=null;
                var outcome=nativeStep.attempt(current,frame,prepared);nextNative=Math.addExact(now,settings.blockIntervalMillis());
                if(outcome.state()==PaperConstructionStep.State.APPLIED||outcome.state()==PaperConstructionStep.State.ALREADY_PRESENT)
                    acknowledging=owner.acknowledge(prepared,now);
                // Routine owner reads can invalidate a permit between ticks. Renew the same exact intent,
                // at most twice, only within the original evidence lifetime; never renew first-write proof.
                else if(outcome.state()==PaperConstructionStep.State.WAIT&&++nativeAttempts<3&&now>=frame.authority().checkedMillis()
                        &&nextNative-frame.authority().checkedMillis()<=5000)
                    preparing=owner.prepare(current,frame,nativeStep.after(current));
                else{explain("paused: "+outcome.explanation());finish();}
                return;
            }
            if(page!=null){
                if(!page.isDone())return;var result=page.join();page=null;
                if(result.projects().size()>8)throw new IllegalArgumentException("Construction page exceeds shared budget");
                cursor=result.next();waiting.addAll(result.projects());return;
            }
            if(current==null){
                if(waiting.isEmpty()){
                    if(cursor!=null){page=owner.page(cursor);return;}
                    if(nextCycle==0){nextCycle=Math.addExact(now,cycleProgress?settings.blockIntervalMillis():settings.reviewSeconds()*1000L);cycleProgress=false;}
                    if(now<nextCycle)return;nextCycle=0;page=owner.page(null);return;
                }
                current=waiting.removeFirst();
                if(current.draft().template().purpose()!=Purpose.HOME){finish();return;}
                if(current.state()==ConstructionWorkQueue.State.WAIT_LABOR||current.state()==ConstructionWorkQueue.State.PAUSED||current.state()==ConstructionWorkQueue.State.CONFLICT){explain(current.explanation());finish();return;}
                if(ConstructionPreview.requiredCells(current.draft().template(),current.draft().transform(),current.draft().origin()).size()+1>PaperConstructionWriter.CELL_READS_PER_TICK){explain("paused: home exceeds bounded native writer size");finish();return;}
            }
            // Deliberately separate a survey tick from the native-write tick; never combine their read budgets.
            if(now<nextNative)return;
            if(survey==null)survey=surveys.apply(current,now);
            var observed=survey.advance(settings.cellsPerTick(),now);if(observed.isEmpty())return;frame=observed.get();
            if(!frame.authority().siteCleared()||!frame.authority().worldLoaded()){explain("paused: "+frame.explanation());finish();return;}
            switch(current.state()){
                case WAIT_MATERIALS->purchasing=owner.procure(current,frame);
                case READY,RESUME->preparing=owner.prepare(current,frame,nativeStep.after(current));
                case COMPLETE->completing=owner.complete(current,frame);
                default->throw new IllegalArgumentException("Unexpected native work state");
            }
        }catch(RuntimeException failure){
            Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
            boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof ConstructionSupplyOrders.Declined||cause instanceof CancellationException;
            String message=expected?cause.getMessage():"review unavailable; paid assets and saved work retained";
            explain("paused: "+message);
            if(!expected&&!Objects.equals(lastWarning,cause.toString())){lastWarning=cause.toString();logger.log(Level.WARNING,"Automatic home construction paused; retained history unchanged",cause);}
            boolean local=current!=null;finish();
            if(!local){page=null;waiting.clear();cursor=null;nextCycle=Math.addExact(now,settings.reviewSeconds()*1000L);cycleProgress=false;}
        }
    }
    private void explain(String message){diagnostic.accept("Home construction"+(current==null?"": " "+current.draft().id())+": "+message+".");}
    private void finish(){current=null;survey=null;frame=null;nativeAttempts=0;purchasing=null;preparing=null;acknowledging=null;completing=null;}
    @Override public void close(){closed=true;waiting.clear();page=null;finish();} // Accepted SQL work drains; no future native write follows close.
}
