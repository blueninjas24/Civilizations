package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionSessionHandoffTest {
    @TempDir Path directory;ConstructionPublicWorkTest f;PopulationSession session;ConstructionDraft draft;
    final AtomicLong clock=new AtomicLong(9001);
    @BeforeEach void setup()throws Exception{
        f=new ConstructionPublicWorkTest();f.directory=directory;f.setup();f.materials(12);var runtime=f.runtime(true);
        for(long now=6000;now<=9000;now+=1000)runtime.pulse(now,false);draft=f.f.publicDraft;f.f.f.db.close();
    }
    @AfterEach void close(){if(session!=null)session.close();f.close();}
    void start(boolean enabled)throws Exception{
        var p=PopulationSession.Settings.prototype();var settings=new PopulationSession.Settings(0,p.millisPerMonth(),p.maximumOfflineMonths(),p.monthsPerBatch(),p.population(),p.migration(),p.recognition(),p.enrollment());
        session=new PopulationSession(directory.toFile(),Logger.getLogger("ConstructionHandoffTest"),settings,clock::get,f.f.f.settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype());
        if(enabled)session.configureConstruction(ConstructionBudgetPolicy.prototype(),new ConstructionLaborPolicy(true,2,1,10,25));
        session.start(false,Set.of(draft.world())).get(10,TimeUnit.SECONDS);
    }
    CompletableFuture<ConstructionBlockJournal.Intent> prepare(UUID operation,int ordinal){return session.preparePublicConstructionStep(operation,draft.id(),ordinal,"minecraft:cobblestone",new ConstructionMaterials.Authority(draft.id(),draft.world(),9000,true,true));}
    @Test void ownerPreparesDurablePaidStepAndAcceptsOnlyFreshExactWorldProjectObservations()throws Exception{
        start(true);UUID operation=UUID.randomUUID();var intent=prepare(operation,0).get(10,TimeUnit.SECONDS);
        assertEquals(ConstructionBlockJournal.State.PREPARED,intent.state());assertEquals(intent,prepare(operation,0).get(10,TimeUnit.SECONDS));
        assertThrows(ExecutionException.class,()->session.acknowledgePublicConstructionStep(operation,UUID.randomUUID(),draft.world(),intent.after(),9000).get(10,TimeUnit.SECONDS));
        assertThrows(ExecutionException.class,()->session.acknowledgePublicConstructionStep(operation,draft.id(),UUID.randomUUID(),intent.after(),9000).get(10,TimeUnit.SECONDS));
        assertThrows(ExecutionException.class,()->session.acknowledgePublicConstructionStep(operation,draft.id(),draft.world(),intent.after(),1000).get(10,TimeUnit.SECONDS));
        assertEquals(ConstructionWorkQueue.State.RESUME,session.constructionWorkPage(null,8).get(10,TimeUnit.SECONDS).projects().getFirst().state());
        var saved=session.acknowledgePublicConstructionStep(operation,draft.id(),draft.world(),intent.after(),9000).get(10,TimeUnit.SECONDS);
        assertEquals(ConstructionBlockJournal.State.APPLIED,saved.state());assertEquals(1,session.constructionWorkPage(null,8).get(10,TimeUnit.SECONDS).projects().getFirst().nextOrdinal());
    }
    @Test void disabledRestartRefusesNewNativeIntentButAllowsExistingHistoryAcknowledgement()throws Exception{
        start(true);UUID operation=UUID.randomUUID();var intent=prepare(operation,0).get(10,TimeUnit.SECONDS);session.close();session=null;
        start(false);assertThrows(ExecutionException.class,()->prepare(UUID.randomUUID(),1).get(10,TimeUnit.SECONDS));
        assertEquals(intent,session.constructionWorkPage(null,8).get(10,TimeUnit.SECONDS).projects().getFirst().pending().orElseThrow());
        assertEquals(ConstructionBlockJournal.State.APPLIED,session.acknowledgePublicConstructionStep(operation,draft.id(),draft.world(),intent.after(),9000).get(10,TimeUnit.SECONDS).state());
    }
    @Test void nativePermitIsNonblockingAndInvalidatedByInterveningOwnerWork()throws Exception{
        start(true);UUID operation=UUID.randomUUID();var authority=new ConstructionMaterials.Authority(draft.id(),draft.world(),9000,true,true);
        var prepared=session.prepareAuthorizedPublicConstructionStep(operation,draft.id(),0,"minecraft:cobblestone",authority).get(10,TimeUnit.SECONDS);
        var callbacks=new java.util.concurrent.atomic.AtomicInteger();
        assertEquals(1,session.tryNativeConstructionStep(prepared,()->{
            assertTrue(session.constructionWorkPage(null,8).isCompletedExceptionally(),"Native callback cannot queue/wait for SQL and deadlock the owner");return callbacks.incrementAndGet();
        }).orElseThrow());
        session.constructionWorkPage(null,8).get(10,TimeUnit.SECONDS);
        assertTrue(session.tryNativeConstructionStep(prepared,callbacks::incrementAndGet).isEmpty());assertEquals(1,callbacks.get());
        var refreshed=session.prepareAuthorizedPublicConstructionStep(operation,draft.id(),0,"minecraft:cobblestone",authority).get(10,TimeUnit.SECONDS);
        assertEquals(prepared.intent(),refreshed.intent());assertEquals(2,session.tryNativeConstructionStep(refreshed,callbacks::incrementAndGet).orElseThrow());
    }
    @Test void ordinaryFundingHoldDoesNotPoisonPopulationFailureStatus()throws Exception{
        f.f.f.open();f.f.f.db.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(UUID.randomUUID(),"Unassessed new fixture resident",draft.settlement(),null,240,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));f.f.f.db.close();
        start(true);var failure=assertThrows(ExecutionException.class,()->session.constructionVillageFunding(draft.id(),draft.world(),ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype()).get(10,TimeUnit.SECONDS));
        assertInstanceOf(ConstructionSupplyOrders.Declined.class,failure.getCause());assertNull(session.status().failure(),"Expected construction funding holds are not population/database failures");
    }
}
