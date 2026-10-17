package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockLearningTest {
    @TempDir Path directory;LivestockRuntimeTest f;
    @BeforeEach void setup()throws Exception{f=new LivestockRuntimeTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    IndustryRuntime runtime(WorkLearningPolicy learning){
        var policy=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(),new WorkshopSupplyPolicy(true,2,8,Map.of(f.f.grain,25L)));
        var runtime=new IndustryRuntime(f.f.db.getConnection(),policy,BusinessOperationPolicy.prototype(),learning);
        runtime.configureLivestock(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype(),LivestockWorkingArea.prototype(),f.healthy::get);return runtime;
    }
    long skill()throws Exception{return f.f.db.getPopulationRepository().findWorkProfile(f.f.worker).orElseThrow().experience().getOrDefault("farming",0L);}
    long awards()throws Exception{return f.scalar("SELECT COUNT(*) FROM livestock_learning_entries");}
    @Test void actualFeedBackedCareLearnsOnceAndRestartDoesNotRetroactivelyTeach()throws Exception{
        var runtime=runtime(WorkLearningPolicy.prototype());runtime.start(1000);runtime.pulse(2000,Set.of(f.f.world));assertEquals(0,skill());assertEquals(0,awards());
        for(int n=3;n<=6;n++)runtime.pulse(n*1000L,Set.of(f.f.world));assertTrue(skill()>0);assertEquals(skill(),awards());
        assertEquals(0,f.f.db.getPopulationRepository().findWorkProfile(f.f.owner).orElseThrow().experience().getOrDefault("farming",0L),"Ownership alone does not earn animal-care experience");
        long saved=skill();runtime.pulse(6000,Set.of(f.f.world));assertEquals(saved,skill());
        f.f.db.close();f.f.reopen();runtime=runtime(WorkLearningPolicy.prototype());runtime.start(100000);runtime.pulse(100000,Set.of(f.f.world));assertEquals(saved,skill());assertEquals(saved,awards());
    }
    @Test void failureAfterSkillWriteRollsBackFeedGoodsExperienceAndThePeriod()throws Exception{
        f.f.feed(20);var runtime=runtime(WorkLearningPolicy.prototype());runtime.start(1000);
        f.f.sql("CREATE TRIGGER fail_after_herd_learning AFTER INSERT ON industry_runtime_history BEGIN SELECT RAISE(ABORT,'fixture final clock failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,Set.of(f.f.world)));assertEquals(0,skill());assertEquals(0,awards());assertEquals(20,f.f.units(f.f.grain));assertEquals(0,f.period());assertEquals(0,f.f.units(f.f.milk));
        f.f.sql("DROP TRIGGER fail_after_herd_learning");runtime.pulse(2000,Set.of(f.f.world));assertEquals(1,skill());assertEquals(1,awards());
    }
    @Test void disabledOrCappedLearningAndAbsentUntrustedWorkNeverGrantFreeSkills()throws Exception{
        f.f.feed(20);var runtime=runtime(WorkLearningPolicy.disabled());runtime.start(1000);runtime.pulse(2000,Set.of(f.f.world));assertEquals(0,skill());assertEquals(0,awards());
        runtime=runtime(new WorkLearningPolicy(true,5,2,64,Map.of("livestock","farming")));runtime.start(3000);runtime.pulse(4000,Set.of(f.f.world));assertEquals(2,skill());
        runtime.pulse(5000,Set.of(f.f.world));assertEquals(2,skill());runtime.pulse(100000,Set.of());assertEquals(2,skill());f.healthy.set(false);runtime.pulse(200000,Set.of(f.f.world));assertEquals(2,skill());
    }
    @Test void migration88PreservesPopulatedHerdAndFailedUpgradeRollsBack()throws Exception{
        var before=f.f.repo().load(f.f.business.id()).orElseThrow();SchemaFixtures.rewind(f.f.db.getConnection(),88);
        f.f.sql("CREATE VIEW livestock_learning_entries AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.f.db.getConnection()));
        assertEquals(88,f.scalar("PRAGMA user_version"));assertEquals(0,f.scalar("SELECT COUNT(*) FROM sqlite_master WHERE name='livestock_learning_reviews'"));assertEquals(before,f.f.repo().load(f.f.business.id()).orElseThrow());
        f.f.sql("DROP VIEW livestock_learning_entries");new SchemaMigrator().migrate(f.f.db.getConnection());assertEquals(SchemaMigrator.CURRENT_VERSION,f.scalar("PRAGMA user_version"));assertEquals(before,f.f.repo().load(f.f.business.id()).orElseThrow());assertEquals(0,awards());
        try(var s=f.f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void employeeCanEarnTheActualFoundingQualificationThenInheritWithoutAFreeWaiver()throws Exception{
        var people=f.f.db.getPopulationRepository();people.recordSkillExperience(f.f.owner,"farming",1);
        UUID business=new UUID(0,100),worker=new UUID(0,101);people.insertResident(new com.blueninjas24.civilizations.population.Resident(worker,"Fixture caretaker",f.f.town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        var site=new BusinessSite(new UUID(0,102),f.f.town,f.f.world,4,64,0,1,"Explicit succession fixture");
        var firm=new Business(business,f.f.town,"Heritable dairy",f.f.owner,f.f.owner,"livestock",site.id(),1,0);
        f.f.db.getBusinessRepository().found(new BusinessFounding(firm,site,new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of("farming",1L)),new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of()),0,"Real qualification fixture"));
        f.f.db.getBusinessRepository().hire(new UUID(0,103),business,worker,0);f.f.db.getIndustryRepository().openStore(business,64,Map.of(f.f.grain,1L,f.f.milk,1L),0);
        var roster=Map.of(new UUID(0,104),0,new UUID(0,105),0);for(var animal:roster.keySet())new AnimalOwnershipRepository(f.f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(animal,f.f.world,LivestockAdoption.Ownership.VILLAGE,null,f.f.town,1,"Explicit succession fixture animal"));
        new LivestockRepository(f.f.db.getConnection()).open(business,LivestockHerd.Species.COW,f.f.grain,8,roster);
        f.f.sql("UPDATE industry_stock SET units=20 WHERE business_id='"+business+"' AND commodity='grain'");
        var owner=people.findResident(f.f.owner).orElseThrow();people.updateResident(new com.blueninjas24.civilizations.population.Resident(owner.id(),owner.name(),owner.settlementId(),owner.householdId(),owner.ageSteps(),com.blueninjas24.civilizations.population.Resident.Status.DECEASED));
        var succession=new BusinessSuccession(f.f.db.getConnection());assertTrue(succession.candidate(business).isEmpty());
        var runtime=runtime(WorkLearningPolicy.prototype());runtime.start(1000);runtime.pulse(2000,Set.of(f.f.world));
        assertEquals(worker,succession.candidate(business).orElseThrow().resident());
        assertEquals(worker,succession.succeed(new UUID(0,106),business,1).orElseThrow().nextOwner());
    }
}
