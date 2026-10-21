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
}
