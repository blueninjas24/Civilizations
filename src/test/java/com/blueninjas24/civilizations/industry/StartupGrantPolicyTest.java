package com.blueninjas24.civilizations.industry;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StartupGrantPolicyTest {
    @Test void broaderProducersShareUnchangedCapsAndFoodProtection() {
        var p=StartupGrantPolicy.prototype();
        for(var industry:Set.of("logging","fishing","quarry","sandpit","bakery","craftsman","livestock")) {
            assertTrue(p.review(industry,6000,10,2000,0,0).eligible(),industry);
            assertFalse(p.review(industry,5999,10,2000,0,0).eligible(),industry);
            assertFalse(p.review(industry,10000,2,1500,4000,4000).eligible(),industry);
            assertFalse(p.review(industry,10000,2,1500,0,19000).eligible(),industry);
        }
        assertEquals(5000,p.maximumGrant());assertEquals(5000,p.perPeriodLimit());
        assertEquals(20000,p.lifetimeLimit());assertEquals(2500,p.minimumTreasury());
    }
    @Test void protectsFoodDemandBudgetEvenWhenItExceedsFixedTreasuryFloor() {
        var p=StartupGrantPolicy.prototype();assertEquals(4000,p.review("mining",6000,10,2000,0,0).protectedHundredths());
        assertTrue(p.review("mining",6000,10,2000,0,0).eligible());assertFalse(p.review("mining",5999,10,2000,0,0).eligible());
    }
    @Test void unsupportedDisabledCapsAndOverflowAreSafeAndExplainable() {
        var p=StartupGrantPolicy.prototype();assertFalse(p.review("unapproved_industry",10000,2,1000,0,0).eligible());
        assertFalse(StartupGrantPolicy.disabled().review("mining",10000,2,1000,0,0).eligible());
        assertTrue(p.review("mining",Long.MAX_VALUE,Long.MAX_VALUE,1,0,0).reason().contains("exceeds"));
        assertFalse(p.review("mining",10000,2,5001,0,0).eligible());assertFalse(p.review("mining",10000,2,1,5000,0).eligible());
        assertFalse(p.review("mining",10000,2,1,0,20000).eligible());
    }
    @Test void invalidSettingsOrEvidenceFailAndIndustryListIsImmutable() {
        assertThrows(IllegalArgumentException.class,()->new StartupGrantPolicy(true,1,1,1,0,0,1,Set.of("mining")));
        assertThrows(IllegalArgumentException.class,()->StartupGrantPolicy.prototype().review("mining",-1,1,1,0,0));
        assertThrows(UnsupportedOperationException.class,()->StartupGrantPolicy.prototype().industries().clear());
    }
}
