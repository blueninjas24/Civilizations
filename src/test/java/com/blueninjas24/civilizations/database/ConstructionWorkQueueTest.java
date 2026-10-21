package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.economy.Commodity;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionWorkQueueTest {
    @TempDir Path directory;ConstructionPublicWorkTest f;
    @BeforeEach void setup()throws Exception{f=new ConstructionPublicWorkTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    ConstructionWorkQueue queue(){return new ConstructionWorkQueue(f.f.c());}
    ConstructionWorkQueue.Work work()throws Exception{return queue().page(null,8,Set.of(f.f.f.world)).projects().getFirst();}
    void ready()throws Exception{f.materials(12);var runtime=f.runtime(true);for(long now=6000;now<=9000;now+=1000)runtime.pulse(now,false);}
    ConstructionBlockJournal.Intent prepare()throws Exception{
        var d=f.f.publicDraft;return new ConstructionBlockJournal(f.f.c()).preparePublic(UUID.randomUUID(),d.id(),0,"minecraft:cobblestone",new ConstructionMaterials.Authority(d.id(),d.world(),9000,true,true),9001,Set.of(d.world()));
    }
    @Test void actualDeliveryAndLaborAdvanceReadOnlyHandoffWithoutGrantingNativePermission()throws Exception{
        assertTrue(queue().page(null,8,Set.of(f.f.f.world)).projects().isEmpty());f.materials(2);
        assertEquals(ConstructionWorkQueue.State.WAIT_MATERIALS,work().state());
        var d=f.f.publicDraft;var q=new ConstructionSupplyOrders.Request(UUID.randomUUID(),d.id(),f.f.seller,Source.VILLAGE,null,Commodity.STONE,10,4);
        f.f.orders().purchase(q,new ConstructionMaterials.Authority(d.id(),d.world(),5000,true,true),5001,Set.of(d.world()));
        f.f.f.economy.pulse(6000,false);ConstructionSupplyOrders.deliveryOnly(f.f.c()).deliverReady(Set.of(d.world()),8);
        assertEquals(ConstructionWorkQueue.State.WAIT_LABOR,work().state());var runtime=f.runtime(true);
        for(long now=7000;now<=10000;now+=1000)runtime.pulse(now,false);
        long before;try(var s=f.f.c().createStatement();var r=s.executeQuery("SELECT total_changes()")){before=r.getLong(1);}
        var next=work();assertEquals(ConstructionWorkQueue.State.READY,next.state());assertEquals(0,next.nextOrdinal());assertTrue(next.pending().isEmpty());
        try(var s=f.f.c().createStatement();var r=s.executeQuery("SELECT total_changes()")){assertEquals(before,r.getLong(1));}
        assertEquals(0,f.count("construction_block_journal"));
    }
    @Test void restartResumesExactSavedIntentThenConflictNeverBecomesNewWork()throws Exception{
        ready();var intent=prepare();assertEquals(ConstructionWorkQueue.State.RESUME,work().state());assertEquals(intent,work().pending().orElseThrow());
        f.f.f.db.close();f.f.f.open();assertEquals(intent,work().pending().orElseThrow());
        new ConstructionBlockJournal(f.f.c()).acknowledge(intent.operation(),"minecraft:stone");assertEquals(ConstructionWorkQueue.State.CONFLICT,work().state());assertEquals(intent.operation(),work().pending().orElseThrow().operation());
    }
    @Test void acknowledgementAdvancesOnlyOneOrdinalAndAbsentWorldsNeverEnterQueue()throws Exception{
        ready();var intent=prepare();new ConstructionBlockJournal(f.f.c()).acknowledge(intent.operation(),intent.after());
        assertEquals(ConstructionWorkQueue.State.READY,work().state());assertEquals(1,work().nextOrdinal());
        assertTrue(queue().page(null,8,Set.of()).projects().isEmpty());assertTrue(queue().page(null,8,Set.of(UUID.randomUUID())).projects().isEmpty());
    }
    @Test void paginationDoesNotLoseSecondPaidProjectOrExposePrivateDraft()throws Exception{
        f.materials(2);var other=f.f.draft("starter_home",180);var q=new ConstructionSupplyOrders.Request(UUID.randomUUID(),other.id(),f.f.seller,Source.VILLAGE,null,Commodity.STONE,1,4);
        f.f.orders().purchase(q,new ConstructionMaterials.Authority(other.id(),other.world(),5000,true,true),5001,Set.of(other.world()));
        var first=queue().page(null,1,Set.of(other.world()));assertNotNull(first.next());var second=queue().page(first.next(),1,Set.of(other.world()));assertNull(second.next());
        assertEquals(Set.of(f.f.publicDraft.id(),other.id()),Set.of(first.projects().getFirst().draft().id(),second.projects().getFirst().draft().id()));
        assertFalse(first.projects().getFirst().draft().id().equals(f.f.privateDraft.id()));
    }
    @Test void automaticQueueRequiresFormalFamilyBindingAndNeverAdoptsArbitraryPaidDrafts()throws Exception{
        f.materials(2);var d=f.f.publicDraft;assertFalse(queue().page(null,8,Set.of(d.world())).projects().isEmpty());
        assertTrue(queue().homePage(null,8,Set.of(d.world())).projects().isEmpty());
        UUID family=UUID.randomUUID();f.f.f.db.getPopulationRepository().insertHousehold(new com.blueninjas24.civilizations.population.Household(family,d.settlement(),"Saved proposal family"));
        f.f.f.sql("UPDATE residents SET household_id='"+family+"' WHERE id='"+f.worker+"'");f.f.f.sql("INSERT INTO household_capacity VALUES('"+family+"',0)");
        try(var s=f.f.c().prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,1200,NULL,NULL)")){
            s.setString(1,d.id().toString());s.setString(2,family.toString());s.setString(3,d.settlement().toString());s.setString(4,d.world().toString());s.setString(5,d.template().fingerprint());s.executeUpdate();
        }
        assertEquals(List.of(d.id()),queue().homePage(null,8,Set.of(d.world())).projects().stream().map(w->w.draft().id()).toList());
        assertTrue(queue().homePage(null,8,Set.of()).projects().isEmpty());
    }
}
