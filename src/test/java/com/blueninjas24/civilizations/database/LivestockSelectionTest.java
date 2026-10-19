package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockSelectionTest {
    @TempDir Path directory;LivestockInvestmentTest f;ResourceInvestment selection;
    @BeforeEach void setup()throws Exception {
        f=new LivestockInvestmentTest();f.directory=directory;f.setup();
        var c=f.f.db.getConnection();var runtime=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of());
        var ops=BusinessOperationPolicy.prototype();var grants=StartupGrantPolicy.prototype();
        selection=new ResourceInvestment(c,BusinessAllocationPolicy.prototype(),runtime,
                new AutomaticMineFounding(c,MineFoundingPolicy.disabled(),runtime,ops,grants,216,42,()->true),
                new AutomaticQuarryFounding(c,QuarryFoundingPolicy.disabled(),QuarryPolicy.disabled(),runtime,ops,grants,216,42,()->true),
                new AutomaticForestryFounding(c,ForestryFoundingPolicy.disabled(),ForestryPolicy.disabled(),runtime,ops,grants,216,42,()->true),
                new LocalIndustryDemand(c,runtime,ops),new VillageSupplyPurchases(c,VillageSupplyPolicy.prototype(),StudyPolicy.disabled(),ops));
    }
    @AfterEach void close(){if(f!=null)f.close();}
    ResourceInvestment.Proposal proposal()throws Exception {
        var prepared=f.investment().prepare(f.proof(),0,40001,Set.of(f.f.world),()->true);
        assertTrue(prepared.request().isPresent(),prepared.explanation());var request=prepared.request().orElseThrow();
        return new ResourceInvestment.Proposal(request.founding().business().id(),"livestock",Set.of(new Commodity("milk")),request.founding().startupHundredths()+request.capital(),prepared.explanation());
    }
    LivestockInvestment.Result accept(ResourceInvestment.Proposal p)throws Exception {
        return f.investment().accept(f.proof(),0,40001,Set.of(f.f.world),()->true,selection.livestockApproval(f.f.town,p,0,40001,Set.of(f.f.world)));
    }
    @Test void qualifiedHerdPreparationClaimsNothingAndExistingZeroDemandEligibilityIsPreserved()throws Exception {
        var before=f.f.snapshot();var p=proposal();assertEquals(before,f.f.snapshot());assertEquals(0,f.count("animal_ownership"));
        var result=accept(p);assertTrue(result.opened(),result.explanation());assertEquals(p.id(),result.business());
        assertEquals(7500,f.f.cash());assertEquals(3,f.count("animal_ownership"));assertEquals(1,f.count("resource_investment_reviews"));
        try(var s=f.f.db.getConnection().createStatement();var r=s.executeQuery("SELECT demand_value,sequence FROM resource_investment_reviews")){r.next();assertEquals(0,r.getLong(1));assertEquals(1,r.getLong(2));}
    }
    @Test void strongerFundedOpportunityDefersHerdWithoutTakingAnimalsOrMoney()throws Exception {
        var p=proposal();f.f.sql("UPDATE economy_stocks SET units=1 WHERE commodity='food'");
        // Trusted water-adapter contract fixture; actual qualified water/bank opening is tested separately.
        selection.withFisheryCandidate((town,step,now,worlds)->Optional.of(new ResourceInvestment.Proposal(new UUID(9,9),"fishing",Set.of(Commodity.FOOD),1500,"Prepared funded water adapter fixture")));
        assertThrows(BusinessStartupGrants.Declined.class,()->selection.livestockApproval(f.f.town,p,0,40001,Set.of(f.f.world)));
        assertEquals(10000,f.f.cash());assertEquals(0,f.count("animal_ownership"));assertEquals(0,f.count("startup_grants"));
        selection.withFisheryCandidate((town,step,now,worlds)->Optional.empty());assertTrue(accept(p).opened());
    }
    @Test void failedSelectionAuditRestoresEveryOwnershipGrantJobAndHerdRowThenRetries()throws Exception {
        var p=proposal();f.f.sql("CREATE TRIGGER reject_herd_choice BEFORE INSERT ON resource_investment_reviews BEGIN SELECT RAISE(ABORT,'fixture audit failure'); END");
        var before=f.f.snapshot();assertThrows(SQLException.class,()->accept(p));assertEquals(before,f.f.snapshot());
        assertEquals(0,f.count("animal_ownership"));assertEquals(0,f.count("livestock_herds"));assertEquals(0,f.count("business_employment"));assertEquals(0,f.count("resource_investment_state"));
        f.f.sql("DROP TRIGGER reject_herd_choice");assertTrue(accept(p).opened());assertEquals(1,f.count("resource_investment_reviews"));
    }
}
