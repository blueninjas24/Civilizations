package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionPublicWorkTest {
    @TempDir Path directory;ConstructionSupplyOrdersTest f;UUID worker,order;
    @BeforeEach void setup()throws Exception {
        f=new ConstructionSupplyOrdersTest();f.directory=directory;f.setup();worker=UUID.randomUUID();
        f.f.db.getPopulationRepository().insertResident(new Resident(worker,"Fixture unemployed builder",f.f.town,null,300,Resident.Status.ALIVE));
        f.f.economy.pulse(4000,false);
    }
    @AfterEach void close(){f.close();}
    void materials(int units)throws Exception {
        var request=new ConstructionSupplyOrders.Request(UUID.randomUUID(),f.publicDraft.id(),f.seller,Source.VILLAGE,null,Commodity.STONE,units,3);
        order=request.operation();f.orders().purchase(request,f.proof(f.publicDraft),4001,Set.of(f.f.world));
        f.f.economy.pulse(5000,false);assertEquals(1,ConstructionSupplyOrders.deliveryOnly(f.c()).deliverReady(Set.of(f.f.world),16));
    }
    EconomyRuntimeRepository runtime(boolean farmJobs)throws Exception {
        var r=new EconomyRuntimeRepository(f.c(),f.f.settings,FarmObservation.Policy.prototype(),216);
        if(farmJobs)r.configureFarmBusinesses(FarmBusinessPolicy.prototype());
        r.configureConstruction(new ConstructionLaborPolicy(true,2,1,10,25));r.startOnline(5000,Set.of(f.f.world));return r;
    }
    long count(String table)throws Exception {try(var s=f.c().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    long work()throws Exception{return new ConstructionPublicWork(f.c()).work(f.publicDraft.id(),0);}
    @Test void realFoodPeriodsEarnBoundedUnemployedWorkAndEscrowMaterialsOnceThenWaitForPhysicalStage()throws Exception {
        materials(12);var runtime=runtime(true);runtime.pulse(6000,false);
        assertEquals(10,work());assertTrue(f.orders().stock(f.publicDraft.id()).isEmpty(),"All stage stone is escrowed once, never spendable twice");
        assertEquals(8,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));
        runtime.pulse(6000,false);assertEquals(10,work());assertEquals(1,count("construction_public_work"));
        for(long now=7000;now<=9000;now+=1000)runtime.pulse(now,false);
        assertEquals(36,work());assertEquals(4,count("construction_public_work"));
        runtime.pulse(10000,false);assertEquals(36,work());assertEquals(0,new ConstructionPublicWork(f.c()).work(f.publicDraft.id(),1));
        assertEquals(0,count("construction_block_journal"),"Earned data work grants no native write or completed home");
        f.f.db.close();f.f.open();assertEquals(36,work());assertTrue(f.orders().stock(f.publicDraft.id()).isEmpty());
        try(var s=f.c().createStatement();var r=s.executeQuery("SELECT DISTINCT resident_id FROM construction_public_work")){assertTrue(r.next());assertEquals(worker.toString(),r.getString(1));assertFalse(r.next());}
    }
    @Test void economicJournalPreparationRequiresEarnedStageWorkEscrowFreshSiteAndEarlierAcknowledgements()throws Exception {
        materials(12);var runtime=runtime(true);runtime.pulse(6000,false);
        var journal=new ConstructionBlockJournal(f.c());var op=UUID.randomUUID();
        var proof=new ConstructionMaterials.Authority(f.publicDraft.id(),f.f.world,9000,true,true);
        assertThrows(SQLException.class,()->journal.preparePublic(op,f.publicDraft.id(),0,"minecraft:cobblestone",proof,9001,Set.of(f.f.world)));
        for(long now=7000;now<=9000;now+=1000)runtime.pulse(now,false);
        assertThrows(SQLException.class,()->journal.preparePublic(op,f.publicDraft.id(),0,"minecraft:cobblestone",proof,9001,Set.of()));
        var first=journal.preparePublic(op,f.publicDraft.id(),0,"minecraft:cobblestone",proof,9001,Set.of(f.f.world));
        assertEquals(ConstructionBlockJournal.State.PREPARED,first.state());
        assertEquals(first,journal.preparePublic(op,f.publicDraft.id(),0,"minecraft:cobblestone",proof,9001,Set.of(f.f.world)));
        assertThrows(SQLException.class,()->journal.preparePublic(UUID.randomUUID(),f.publicDraft.id(),1,"minecraft:cobblestone",proof,9001,Set.of(f.f.world)));
        journal.acknowledge(op,"minecraft:cobblestone");
        journal.preparePublic(UUID.randomUUID(),f.publicDraft.id(),1,"minecraft:cobblestone",proof,9001,Set.of(f.f.world));
        assertEquals(2,count("construction_block_journal"));assertEquals(36,work());assertTrue(f.orders().stock(f.publicDraft.id()).isEmpty());
    }
    @Test void incompleteMaterialsAndAbsentWorldsCannotEarnWorkOrSpendStock()throws Exception {
        materials(11);var runtime=runtime(true);runtime.pulse(6000,false);assertEquals(0,work());assertEquals(Map.of(Commodity.STONE,11L),f.orders().stock(f.publicDraft.id()));
        runtime.updateLoadedWorlds(6000,Set.of());runtime.pulse(106000,false);assertEquals(0,work());
        assertEquals(5,runtime.views().get(f.f.town).account().state().completedPeriods());
    }
    @Test void legacyAllAdultFoodLaborAndActiveStudentsAreNotExtraBuilders()throws Exception {
        materials(12);var legacy=runtime(false);legacy.pulse(6000,false);assertEquals(0,work());
        f.f.sql("INSERT INTO resident_courses VALUES('"+UUID.randomUUID()+"','"+worker+"','"+f.f.town+"','mining',10,1,99,'ACTIVE')");
        var dedicated=new EconomyRuntimeRepository(f.c(),f.f.settings,FarmObservation.Policy.prototype(),216);
        dedicated.configureFarmBusinesses(FarmBusinessPolicy.prototype());dedicated.configureConstruction(ConstructionLaborPolicy.prototype());
        dedicated.startOnline(6000,Set.of(f.f.world));dedicated.pulse(7000,false);
        assertEquals(0,work());assertEquals(Map.of(Commodity.STONE,12L),f.orders().stock(f.publicDraft.id()));
    }
    @Test void lateWorkReceiptFailureRollsBackFoodCheckpointStageEscrowAndLaborTogether()throws Exception {
        materials(12);var runtime=runtime(true);
        f.f.sql("CREATE TRIGGER reject_builder AFTER INSERT ON construction_public_work BEGIN SELECT RAISE(ABORT,'fixture work failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(6000,false));
        assertEquals(0,work());assertEquals(0,count("construction_stage_work"));assertEquals(4,runtime.views().get(f.f.town).account().state().completedPeriods());
        assertEquals(Map.of(Commodity.STONE,12L),f.orders().stock(f.publicDraft.id()));
        f.f.sql("DROP TRIGGER reject_builder");runtime.pulse(6000,false);assertEquals(10,work());assertEquals(1,count("construction_public_work"));
    }
    @Test void version106UpgradeKeepsPaidCargoAndBalancesThroughFailureAndRetry()throws Exception {
        materials(12);long money=f.money();var saved=f.orders().find(order).orElseThrow();
        SchemaFixtures.rewind(f.c(),106);f.f.sql("CREATE TABLE construction_stage_work(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.c()));assertEquals(money,f.money());assertEquals(saved,f.orders().find(order).orElseThrow());
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(106,r.getInt(1));}
        f.f.sql("DROP TABLE construction_stage_work");new SchemaMigrator().migrate(f.c());
        assertEquals(Map.of(Commodity.STONE,12L),f.orders().stock(f.publicDraft.id()));assertEquals(0,count("construction_public_work"));
        try(var s=f.c().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
