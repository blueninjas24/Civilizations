package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperAutomaticConstructionTest {
    private ConstructionDraft draft;private long now=1000;private int pages,surveys,advances,buys,prepares,writes,acks,completions;
    private boolean safe=true;private final List<UUID> visited=new ArrayList<>();private final List<String> diagnostics=new ArrayList<>();
    private CompletableFuture<ConstructionWorkQueue.Page> page=new CompletableFuture<>();
    private CompletableFuture<Boolean> buying=new CompletableFuture<>();private CompletableFuture<Void> ack=new CompletableFuture<>(),complete=new CompletableFuture<>();
    private CompletableFuture<PopulationSession.PreparedConstructionStep> preparing=new CompletableFuture<>();
    private PaperConstructionStep.State nativeResult=PaperConstructionStep.State.APPLIED;
    @BeforeEach void setup()throws Exception{
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        draft=new ConstructionDraft(new UUID(0,1),new UUID(0,2),new UUID(0,3),new Position(0,64,0),Transform.identity(),template,1000);
    }
    private ConstructionWorkQueue.Work work(ConstructionWorkQueue.State state){return new ConstructionWorkQueue.Work(draft,state,state==ConstructionWorkQueue.State.COMPLETE?120:0,Optional.empty(),"Saved "+state);}
    private PaperAutomaticConstruction runtime(boolean enabled){
        return new PaperAutomaticConstruction(new PaperAutomaticConstruction.Owner(){
            public CompletableFuture<ConstructionWorkQueue.Page> page(UUID after){pages++;return page;}
            public CompletableFuture<Boolean> procure(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame){buys++;return buying;}
            public CompletableFuture<PopulationSession.PreparedConstructionStep> prepare(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame,String after){prepares++;return preparing;}
            public CompletableFuture<Void> acknowledge(PopulationSession.PreparedConstructionStep prepared,long time){acks++;assertEquals(now,time);return ack;}
            public CompletableFuture<Void> complete(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame){completions++;return complete;}
        },new AutomaticConstructionSettings(enabled,128,10,500),new PaperAutomaticConstruction.NativeStep(){
            public String after(ConstructionWorkQueue.Work work){return "minecraft:cobblestone";}
            public PaperConstructionStep.Outcome attempt(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame,PopulationSession.PreparedConstructionStep prepared){writes++;return new PaperConstructionStep.Outcome(nativeResult,"Fixture native result");}
        },(work,start)->{surveys++;visited.add(work.draft().id());return (budget,time)->{advances++;assertEquals(128,budget);return Optional.of(new PaperConstructionWorkSurvey.Frame(new ConstructionMaterials.Authority(work.draft().id(),work.draft().world(),time,safe,safe),"minecraft:air",safe?"verified":"unavailable",Map.of()));};},()->now,diagnostics::add,Logger.getLogger("AutomaticConstructionTest"));
    }
    private PopulationSession.PreparedConstructionStep prepared(){var operation=UUID.randomUUID();return new PopulationSession.PreparedConstructionStep(new ConstructionBlockJournal.Intent(operation,draft.id(),0,draft.origin(),"minecraft:air","minecraft:cobblestone",ConstructionBlockJournal.State.PREPARED),new NativeConstructionGate.Permit(UUID.randomUUID(),1,operation,draft.id(),draft.world(),now));}
    private void start(PaperAutomaticConstruction r,ConstructionWorkQueue.Work work){r.run();now+=10_000;r.run();page.complete(new ConstructionWorkQueue.Page(List.of(work),null));r.run();r.run();}
    @Test void asynchronousPurchasesHaveOneBoundedSurveyAndCannotSpendAgainWhilePending(){
        var r=runtime(true);start(r,work(ConstructionWorkQueue.State.WAIT_MATERIALS));assertEquals(1,buys);assertEquals(1,advances);
        r.run();r.run();assertEquals(1,buys);assertEquals(1,surveys);buying.complete(true);r.run();assertTrue(diagnostics.getLast().contains("supplies bought"));
        assertEquals(0,writes);assertEquals(0,acks);r.close();assertFalse(buying.isCancelled());
    }
    @Test void aNativeWriteWaitsForDurablePreparationThenAcknowledgesOnceBeforeAnyOtherWork(){
        var r=runtime(true);start(r,work(ConstructionWorkQueue.State.READY));assertEquals(1,prepares);assertEquals(0,writes);
        r.run();assertEquals(0,writes);preparing.complete(prepared());r.run();assertEquals(1,writes);assertEquals(1,acks);
        r.run();r.run();assertEquals(1,writes);assertEquals(1,surveys);ack.complete(null);r.run();assertTrue(diagnostics.getLast().contains("saved block 1/120"));
    }
    @Test void closingAfterIntentPreparationNeverWritesOrCancelsAcceptedSqlWork(){
        var r=runtime(true);start(r,work(ConstructionWorkQueue.State.READY));r.close();assertFalse(preparing.isCancelled());preparing.complete(prepared());r.run();assertEquals(0,writes);assertEquals(0,acks);
    }
    @Test void changedWorldAndDisabledOrLaborWaitingProjectsDoNotSpendOrWrite(){
        var disabled=runtime(false);disabled.run();assertEquals(0,pages);
        var r=runtime(true);safe=false;start(r,work(ConstructionWorkQueue.State.WAIT_MATERIALS));assertEquals(0,buys);assertEquals(0,prepares);assertTrue(diagnostics.getLast().contains("unavailable"));r.close();
        page=new CompletableFuture<>();var labor=runtime(true);start(labor,work(ConstructionWorkQueue.State.WAIT_LABOR));assertEquals(1,surveys);assertEquals(0,buys);assertEquals(0,writes);
    }
    @Test void refusalOrAmbiguousNativeWaitRetainsQueueAndNeverAcknowledgesFabricatedOutput(){
        var r=runtime(true);nativeResult=PaperConstructionStep.State.WAIT;start(r,work(ConstructionWorkQueue.State.RESUME));preparing.complete(prepared());r.run();assertEquals(1,writes);assertEquals(0,acks);r.run();assertEquals(1,writes);assertEquals(0,completions);
    }
    @Test void transientOwnerContentionRenewsTheSameIntentWithPacingAndStrictRetryBounds(){
        var r=runtime(true);nativeResult=PaperConstructionStep.State.WAIT;start(r,work(ConstructionWorkQueue.State.RESUME));preparing.complete(prepared());
        r.run();assertEquals(1,writes);r.run();assertEquals(1,writes);now+=500;r.run();assertEquals(2,writes);now+=500;r.run();assertEquals(3,writes);assertEquals(0,acks);assertEquals(3,prepares);
        r.run();assertEquals(3,writes);assertTrue(diagnostics.getLast().contains("paused"));
    }
    @Test void oneBlockedVillageDoesNotStarveTheNextAndPagingRemainsBounded(){
        var r=runtime(true);var first=work(ConstructionWorkQueue.State.CONFLICT);var second=new ConstructionDraft(new UUID(0,9),draft.settlement(),draft.world(),draft.origin(),draft.transform(),draft.template(),1000);
        r.run();now+=10_000;r.run();page.complete(new ConstructionWorkQueue.Page(List.of(first,new ConstructionWorkQueue.Work(second,ConstructionWorkQueue.State.WAIT_MATERIALS,0,Optional.empty(),"ready inputs")),null));
        r.run();r.run();r.run();assertEquals(1,buys);assertEquals(List.of(second.id()),visited);assertEquals(1,pages);
    }
    @Test void failedPurchaseDoesNotDiscardOtherVillagesInTheSamePage(){
        var r=runtime(true);buying=CompletableFuture.failedFuture(new BuildingRegistry.Declined("Funds protected"));var a=work(ConstructionWorkQueue.State.WAIT_MATERIALS);
        r.run();now+=10_000;r.run();page.complete(new ConstructionWorkQueue.Page(List.of(a,a),null));r.run();r.run();r.run();assertTrue(diagnostics.getLast().contains("Funds protected"));r.run();assertEquals(2,buys);assertEquals(1,pages);
    }
    @Test void onlyCompletePaidNativeWorkCanRegisterHomeAndNoRuntimeCreatesCapacity(){
        var r=runtime(true);start(r,work(ConstructionWorkQueue.State.COMPLETE));assertEquals(1,completions);assertEquals(0,writes);assertEquals(0,buys);r.run();assertEquals(1,completions);complete.complete(null);r.run();assertTrue(diagnostics.getLast().contains("housing inspection"));
    }
}
