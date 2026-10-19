package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FisherySelectionTest {
    @TempDir Path directory;FisheryInvestmentTest f;FisheryInvestment investment;ResourceInvestment selection;
    BusinessStartupGrantsTest data(){return f.data();}
    @BeforeEach void setup()throws Exception {
        f=new FisheryInvestmentTest();f.directory=directory;f.setup();f.qualified();
        var c=data().db.getConnection();var runtime=f.fixture.industry;var ops=BusinessOperationPolicy.prototype();var grants=StartupGrantPolicy.prototype();
        investment=new FisheryInvestment(c,f.fixture.repo(),f.policy,f.fixture.terms,runtime,WaterObserverPolicy.prototype(),FisheryShorelinePolicy.prototype(),grants,216,42);
        selection=new ResourceInvestment(c,BusinessAllocationPolicy.prototype(),runtime,
                new AutomaticMineFounding(c,MineFoundingPolicy.disabled(),runtime,ops,grants,216,42,()->true),
                new AutomaticQuarryFounding(c,QuarryFoundingPolicy.disabled(),QuarryPolicy.disabled(),runtime,ops,grants,216,42,()->true),
                new AutomaticForestryFounding(c,ForestryFoundingPolicy.disabled(),ForestryPolicy.disabled(),runtime,ops,grants,216,42,()->true),
                new LocalIndustryDemand(c,runtime,ops),new VillageSupplyPurchases(c,VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),ops))
                .withFisheryCandidate((town,step,now,worlds)->investment.prepare(town,step,now,worlds).proposal().map(p->
                        new ResourceInvestment.Proposal(FisheryInvestment.selectionId(p.opportunity()),"fishing",Set.of(Commodity.FOOD),1500,"Qualified funded water")));
    }
    @AfterEach void close(){if(f!=null)f.close();}
    FisheryInvestment.Proposal proposal()throws Exception{return investment.prepare(data().town,2,2000,Set.of(data().world)).proposal().orElseThrow();}
    ShorelineEvidence bank(FisheryInvestment.Proposal p) {
        var scan=new ShorelineScan(point->point.equals(new ShorelineScan.Position(-1,64,0))?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR,p.opportunity().water(),2);
        while(!scan.complete())scan.advance(4096);
        return new ShorelineEvidence(data().world,new ShorelineScan.Position(-1,65,0),2001,scan.assessments());
    }
    @Test void foodStartupWorksWithPrivateInputBuyingDisabledAndWaitsForActualBankProof()throws Exception {
        assertFalse(f.fixture.industry.supply().enabled());var before=data().snapshot();
        String reason=selection.review(data().town,2,2000,Set.of(data().world));assertTrue(reason.contains("shoreline/water confirmation"),reason);assertEquals(before,data().snapshot());
        var p=proposal();var frame=bank(p);var key=FisheryInvestment.selectionId(p.opportunity());
        var started=investment.accept(p,frame,2001,Set.of(data().world),FishingPolicy.prototype(),selection.confirmationApproval(data().town,key,2,2001,Set.of(data().world)));
        assertNotEquals(key,started.startup().founding().business().id());
        assertEquals(1,data().count("resource_investment_reviews"));assertEquals(1,data().count("startup_grants"));assertEquals(8500,data().cash());
        assertEquals(128,started.resource().stock().capacity());assertEquals(0,started.startup().store().units().get(Commodity.FOOD));
        assertEquals(started,investment.accept(p,frame,2001,Set.of(data().world),FishingPolicy.prototype()));assertEquals(1,data().count("resource_investment_reviews"));
    }
    @Test void failedSelectionSaveRollsBackWaterBankJobAndGrantThenRetries()throws Exception {
        var p=proposal();var frame=bank(p);var key=FisheryInvestment.selectionId(p.opportunity());
        data().sql("CREATE TRIGGER reject_fishing_choice BEFORE INSERT ON resource_investment_reviews BEGIN SELECT RAISE(ABORT,'fixture selection failure'); END");
        assertThrows(SQLException.class,()->investment.accept(p,frame,2001,Set.of(data().world),FishingPolicy.prototype(),selection.confirmationApproval(data().town,key,2,2001,Set.of(data().world))));
        assertEquals(10000,data().cash());assertEquals(0,data().count("startup_grants"));assertEquals(0,data().count("fishery_resources"));assertEquals(0,data().count("fishery_workplaces"));assertEquals(0,data().count("business_employment"));assertEquals(0,data().count("resource_investment_state"));
        data().sql("DROP TRIGGER reject_fishing_choice");investment.accept(p,frame,2001,Set.of(data().world),FishingPolicy.prototype(),selection.confirmationApproval(data().town,key,2,2001,Set.of(data().world)));
        assertEquals(1,data().count("resource_investment_reviews"));
    }
    @Test void actualPublicFoodCustomerCanAlsoJustifyFisheryHiringWithPrivateBuyingOff()throws Exception {
        var p=proposal();var frame=bank(p);
        var opened=investment.accept(p,frame,2001,Set.of(data().world),FishingPolicy.prototype());
        var employee=UUID.randomUUID();data().db.getPopulationRepository().insertResident(new com.blueninjas24.civilizations.population.Resident(employee,"Fixture fisher",data().town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        data().db.getPopulationRepository().recordSkillExperience(employee,"fishing",1);
        data().sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'");data().economy.pulse(3000,false);
        data().sql("UPDATE economy_stocks SET units=1 WHERE commodity='food'");
        var c=data().db.getConnection();var ops=BusinessOperationPolicy.prototype();var staffing=new BusinessStaffingPolicy(true,64,1,Set.of("fishing"));
        var competition=new BusinessCompetition(c,BusinessAllocationPolicy.prototype(),staffing,f.fixture.industry,ops,new LocalIndustryDemand(c,f.fixture.industry,ops),
                new VillageSupplyPurchases(c,VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),ops));
        long cash=data().cash();String reason=competition.review(data().town,3,3000,Set.of(data().world),new AutomaticBusinessStaffing(c,staffing));
        assertTrue(reason.contains("Vacancy hiring: 1"),reason);assertEquals(2,data().db.getBusinessRepository().currentWorkers(opened.startup().founding().business().id()).size());assertEquals(cash,data().cash());
    }
}
