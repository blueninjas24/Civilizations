package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FisheryInvestmentTest {
    @TempDir Path directory;FisheryOpportunitiesTest fixture;
    FisheryFoundingPolicy policy=FisheryFoundingPolicy.prototype();
    @BeforeEach void setup()throws Exception{fixture=new FisheryOpportunitiesTest();fixture.directory=directory;fixture.setup();}
    @AfterEach void close(){fixture.close();}
    BusinessStartupGrantsTest data(){return fixture.fixture;}
    void qualified()throws Exception{data().db.getPopulationRepository().recordSkillExperience(data().person,"fishing",10);}
    FisheryInvestment repo(long adulthood,long seed){return new FisheryInvestment(data().db.getConnection(),fixture.repo(),policy,adulthood,seed);}
    FisheryInvestment.Preparation prepare()throws Exception{return repo(216,42).prepare(data().town,2,2000,Set.of(data().world));}
    @Test void actualQualifiedOwnerProposalIsReadOnlySeededAndSurvivesRestart()throws Exception {
        qualified();var before=data().snapshot();var result=prepare();var proposal=result.proposal().orElseThrow();assertEquals(data().person,proposal.founder());
        assertEquals(2,proposal.foundedStep());assertEquals(10,proposal.ownerRequirements().minimumExperience().get("fishing"));assertEquals(1,proposal.workerRequirements().minimumExperience().get("fishing"));assertEquals(2,proposal.workerCapacity());
        for(int i=0;i<12;i++)assertEquals(result,prepare());assertEquals(before,data().snapshot());data().db.close();data().open();assertEquals(result,prepare());
    }
    @Test void observationOrWrongSkillCannotGrantFishingExperience()throws Exception {
        assertTrue(prepare().proposal().isEmpty());data().db.getPopulationRepository().recordSkillExperience(data().person,"mining",100);
        assertTrue(prepare().proposal().isEmpty());data().db.getPopulationRepository().recordSkillExperience(data().person,"fishing",9);assertTrue(prepare().proposal().isEmpty());
        assertEquals(9,data().db.getPopulationRepository().findWorkProfile(data().person).orElseThrow().experience().get("fishing"));assertEquals(0,data().count("businesses"));
    }
    @Test void actualAdulthoodAndLivingLocalIdentityRemainRequired()throws Exception {
        qualified();assertTrue(repo(400,42).prepare(data().town,2,2000,Set.of(data().world)).proposal().isEmpty());
        data().sql("UPDATE residents SET age_steps=100");assertTrue(prepare().proposal().isEmpty());data().sql("UPDATE residents SET age_steps=300,status='DECEASED'");assertTrue(prepare().proposal().isEmpty());
    }
    @Test void existingJobBlocksPreparationButOwnershipHasNoArtificialLimit()throws Exception {
        qualified();data().grants().startWorkshop(data().producer("quarry",data().person,20,1000),StartupGrantPolicy.prototype());assertTrue(prepare().proposal().isEmpty());
        data().sql("UPDATE business_employment SET ended_step=1,end_reason='Explicit fixture job ended'");assertEquals(data().person,prepare().proposal().orElseThrow().founder());
        assertEquals(1,data().count("businesses"));assertEquals(1,data().count("startup_grants"));
    }
    @Test void activeStudyBlocksPreparationWithoutDiscardingItsCourse()throws Exception {
        qualified();data().sql("INSERT INTO economy_stocks VALUES('"+data().town+"','timber',0,0)");
        var course=UUID.randomUUID();var connection=data().db.getConnection();connection.setAutoCommit(false);
        try{new ResidentStudy(connection,StudyPolicy.prototype(),216).enrollRows(course,data().person,data().town,"commerce",1);connection.commit();}
        catch(Exception failure){connection.rollback();throw failure;}finally{connection.setAutoCommit(true);}
        var before=data().snapshot();assertTrue(prepare().proposal().isEmpty());assertEquals(before,data().snapshot());assertEquals(1,data().count("resident_courses"));
    }
    @Test void founderPageAndSeedNeverSelectAnIneligibleResident()throws Exception {
        qualified();var other=UUID.randomUUID();data().db.getPopulationRepository().insertResident(new Resident(other,"Explicit other founder",data().town,null,300,Resident.Status.ALIVE));
        data().db.getPopulationRepository().recordSkillExperience(other,"fishing",10);data().sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'");data().economy.pulse(3000,false);data().sql("UPDATE economy_stocks SET units=1 WHERE commodity='food'");
        policy=new FisheryFoundingPolicy(true,1,216,10,1,2);var expected=java.util.stream.Stream.of(data().person,other).min(Comparator.comparing(UUID::toString)).orElseThrow();
        for(int seed=0;seed<12;seed++)assertEquals(expected,repo(216,seed).prepare(data().town,2,3000,Set.of(data().world)).proposal().orElseThrow().founder());
    }
    @Test void serializedPreparationRespectsPauseAndNeverCreatesAWorkplace()throws Exception {
        qualified();data().db.close();var time=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var owner=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),java.util.logging.Logger.getLogger("FisheryOwnerTest"),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),time::get)) {
            owner.configureWater(WaterObserverPolicy.prototype());owner.configureFishing(FishingPolicy.prototype());owner.configureFisheryOpportunities(fixture.terms);owner.configureFisheryFounding(policy);
            owner.configureIndustryRuntime(fixture.industry);owner.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());owner.configureVillageSupplies(VillageSupplyPolicy.prototype());
            owner.start(false,Set.of(data().world)).get(5,TimeUnit.SECONDS);assertEquals(data().person,owner.prepareFisheryInvestment(data().town).get(5,TimeUnit.SECONDS).proposal().orElseThrow().founder());
            owner.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);assertTrue(owner.prepareFisheryInvestment(data().town).get(5,TimeUnit.SECONDS).proposal().isEmpty());
        }
        data().open();assertEquals(10000,data().cash());assertEquals(0,data().count("business_sites"));assertEquals(0,data().count("fishery_resources"));
    }
}
