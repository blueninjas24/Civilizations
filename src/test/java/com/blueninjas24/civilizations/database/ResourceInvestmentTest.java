package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourceInvestmentTest {
    @TempDir Path directory;AutomaticQuarryFoundingTest fixture;IndustryRuntimePolicy policy;ResourceInvestment investment;
    @BeforeEach void setup()throws Exception {
        fixture=new AutomaticQuarryFoundingTest();fixture.directory=directory;fixture.setup();fixture.prepareResource("stone");
        var employee=new UUID(0,6);fixture.db.getPopulationRepository().insertResident(new Resident(employee,"Fixture smith",fixture.town,null,300,Resident.Status.ALIVE));
        fixture.business(new UUID(2,3),"blacksmith",2,10000,employee,fixture.storageSizes(Commodity.STONE));
        var cut=new ProductionRecipe("cut","craftsman",Map.of(Commodity.STONE,2L),Map.of(new Commodity("cut_material"),1L),1,4);
        var tools=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.COAL,3L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,4);
        var base=fixture.industryPolicy();policy=new IndustryRuntimePolicy(true,1000,8,base.mining(),Map.of("cut",cut,"tools",tools),
                new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.COAL,100L,Commodity.STONE,100L)),new WarehouseSupplyPolicy(true,16,16));
        new MineralOpportunities(fixture.db.getConnection()).observe(fixture.town,List.of(new MineralObservation(fixture.world,2000,true,Commodity.COAL,List.of(new Position(40,40,0),new Position(41,40,0))),
                new MineralObservation(fixture.world,2000,true,Commodity.IRON,List.of())));
        // The extra real customer employee changes food demand; funding must use a fresh assessment.
        var economy=new EconomyRuntimeRepository(fixture.db.getConnection(),fixture.economySettings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216,
                com.blueninjas24.civilizations.economy.VillagePriorities.Policy.prototype(),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype().seed());
        economy.startOnline(2000,Set.of(fixture.world));economy.pulse(3000,false);
        investment=service();
    }
    @AfterEach void close(){if(fixture!=null)fixture.close();}
    ResourceInvestment service(){
        var c=fixture.db.getConnection();var operations=BusinessOperationPolicy.prototype();var grants=StartupGrantPolicy.prototype();
        var mines=new AutomaticMineFounding(c,MineFoundingPolicy.prototype(),policy,operations,grants,216,42,()->fixture.healthy);
        var quarries=new AutomaticQuarryFounding(c,QuarryFoundingPolicy.prototype(),QuarryPolicy.prototype(),policy,operations,grants,216,42,()->fixture.healthy);
        var forests=new AutomaticForestryFounding(c,ForestryFoundingPolicy.disabled(),ForestryPolicy.disabled(),policy,operations,grants,216,42,()->true);
        return new ResourceInvestment(c,BusinessAllocationPolicy.prototype(),policy,mines,quarries,forests,new LocalIndustryDemand(c,policy,operations),
                new VillageSupplyPurchases(c,VillageSupplyPolicy.disabled(),StudyPolicy.disabled(),operations));
    }
    String review(long now)throws Exception{return investment.review(fixture.town,1,now,Set.of(fixture.world));}
    long count(String table)throws Exception{return fixture.scalar("SELECT COUNT(*) FROM "+table);}
    @Test void strongerMineCustomerBeatsFeasibleQuarryAndUsesOnlyOneProtectedGrant()throws Exception {
        String result=review(2000);assertTrue(result.contains("mining, funded demand value 300 from 2 viable"),result);
        assertEquals(1,fixture.firms("mining"));assertEquals(0,fixture.firms("quarry"));
        assertEquals(1,count("resource_investment_reviews"));assertEquals(1,count("startup_grants"));assertEquals(57500,fixture.cash());
        assertEquals(2000,fixture.scalar("SELECT capital_hundredths FROM startup_grants"));
        assertEquals(0,fixture.scalar("SELECT SUM(units) FROM industry_stock WHERE business_id IN(SELECT business_id FROM startup_grants)"));
        assertTrue(review(3000).contains("interval"));assertEquals(1,count("startup_grants"));
    }
    @Test void strongerQuarryCustomerCanWinInsteadOfHardcodedMineOrder()throws Exception {
        fixture.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+new UUID(2,3)+"'");
        String result=review(2000);assertTrue(result.contains("quarry, funded demand value 200"),result);
        assertEquals(1,fixture.firms("quarry"));assertEquals(0,fixture.firms("mining"));assertEquals(1,count("resource_investment_reviews"));
    }
    @Test void lateAuditFailureRollsBackMoneyBusinessSourcesFirstJobAndRotation()throws Exception {
        fixture.sql("CREATE TRIGGER reject_investment BEFORE INSERT ON resource_investment_reviews BEGIN SELECT RAISE(ABORT,'audit failure'); END");
        assertThrows(SQLException.class,()->review(2000));assertEquals(60000,fixture.cash());assertEquals(0,count("startup_grants"));
        assertEquals(0,count("mineral_deposits"));assertEquals(0,count("resource_investment_state"));assertEquals(3,count("business_employment"));
        fixture.sql("DROP TRIGGER reject_investment");assertTrue(review(2000).contains("mining"));assertEquals(1,count("startup_grants"));
    }
    @Test void absentWorldNoSkillNoFundingAndStaleEvidenceCannotInventAnOpportunity()throws Exception {
        assertTrue(investment.review(fixture.town,1,2000,Set.of()).contains("absent"));assertEquals(0,count("startup_grants"));
        fixture.sql("UPDATE resident_skills SET experience=0 WHERE skill='mining'");review(2000);assertEquals(0,count("startup_grants"));
        fixture.sql("UPDATE resident_skills SET experience=10 WHERE skill='mining'");fixture.sql("UPDATE treasuries SET hundredths=2500");
        investment=service();review(2000);assertEquals(0,count("startup_grants"));
        fixture.sql("UPDATE treasuries SET hundredths=60000");investment=service();review(1_000_000);assertEquals(0,count("startup_grants"));
    }
}
