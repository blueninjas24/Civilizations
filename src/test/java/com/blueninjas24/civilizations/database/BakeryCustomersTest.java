package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BakeryCustomersTest {
    @TempDir Path directory;BakeryInvestmentTest f;
    @BeforeEach void setup()throws Exception{f=new BakeryInvestmentTest();f.directory=directory;f.setup();f.opportunity();}
    @AfterEach void close(){f.close();}
    BakeryOpportunities review(BakeryMarketPolicy market) {
        return new BakeryOpportunities(f.db.getConnection(),WorkshopOpportunityPolicy.prototype(),f.runtime(),StartupGrantPolicy.prototype(),f.starter,
                VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.prototype(),market);
    }
    UUID paidFood()throws Exception {
        var seller=f.privateSource("farming");f.sql("UPDATE industry_stock SET units=10 WHERE business_id='"+seller+"' AND commodity='food'");
        f.sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");
        new VillageSupplyPurchases(f.db.getConnection(),VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),BusinessOperationPolicy.prototype())
                .purchase(new VillageSupplyPurchases.Request(UUID.randomUUID(),f.town,seller,Commodity.FOOD,4,1),Set.of(f.world));
        // Explicit test-owned reserve refill, not production or forecast behavior.
        f.sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'");return seller;
    }
    @Test void wellFedVillageNeedsRealPaidHistoryAndForecastNeverCreatesAnOrderOrSpends()throws Exception {
        f.sql("UPDATE economy_stocks SET units=100 WHERE commodity='food'");
        assertTrue(review(BakeryMarketPolicy.prototype()).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
        paidFood();var before=f.snapshot();
        assertTrue(review(BakeryMarketPolicy.disabled()).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
        var market=review(BakeryMarketPolicy.prototype()).review(f.town,1000,Set.of(f.world));assertEquals(4,market.candidates().getFirst().fundedFoodUnits());
        assertEquals(before,f.snapshot());f.db.close();f.open();assertEquals(market,review(BakeryMarketPolicy.prototype()).review(f.town,1000,Set.of(f.world)));
    }
    @Test void editableShareCashWorldAndCurrentPopulationLimitCompetition()throws Exception {
        paidFood();var before=f.snapshot();
        assertTrue(review(new BakeryMarketPolicy(true,12,5000,32)).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
        assertTrue(review(new BakeryMarketPolicy(true,12,10000,2)).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
        assertTrue(review(BakeryMarketPolicy.prototype()).review(f.town,1000,Set.of()).candidates().isEmpty());assertEquals(before,f.snapshot());
        f.sql("UPDATE treasuries SET hundredths=7000");assertTrue(review(BakeryMarketPolicy.prototype()).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
        f.sql("UPDATE treasuries SET hundredths=10000");f.sql("UPDATE needs_periods SET demand=2,fulfilled=2");
        assertTrue(review(BakeryMarketPolicy.prototype()).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
    }
    @Test void paidHistoryAgesOutAndIsAveragedAcrossQuietPeriods()throws Exception {
        paidFood();f.sql("UPDATE economy_accounts SET completed_periods=2");f.sql("UPDATE economy_runtime SET target_periods=2");
        f.sql("INSERT INTO needs_periods(settlement_id,period,food_condition,shortage_periods,fulfilled,demand,unassigned_residents) SELECT settlement_id,2,food_condition,shortage_periods,fulfilled,demand,unassigned_residents FROM needs_periods WHERE period=1");
        assertTrue(review(BakeryMarketPolicy.prototype()).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
        assertTrue(review(new BakeryMarketPolicy(true,1,10000,32)).review(f.town,1000,Set.of(f.world)).candidates().isEmpty());
    }
}
