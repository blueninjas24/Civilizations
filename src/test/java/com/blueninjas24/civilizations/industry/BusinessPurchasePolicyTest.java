package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessPurchasePolicyTest {
    @Test void retainedOperatingCashAndFractionalMoneyAreNeverSpent() {
        var p=BusinessPurchasePolicy.prototype();assertEquals(0,p.businessAvailable(900));assertEquals(0,p.businessAvailable(1003));
        assertEquals(1,p.businessAvailable(1004));assertEquals(1250,p.businessAvailable(6000));assertEquals(333,p.estateAvailable(333));
    }
    @Test void extremeBalancesAreConservedWithoutPercentageOverflow() {
        assertEquals(Long.MAX_VALUE,new BusinessPurchasePolicy(0,100,100).businessAvailable(Long.MAX_VALUE));
        long available=BusinessPurchasePolicy.prototype().businessAvailable(Long.MAX_VALUE);
        assertTrue(available>0);assertTrue(available<Long.MAX_VALUE/4);assertEquals(Long.MAX_VALUE,BusinessPurchasePolicy.prototype().estateAvailable(Long.MAX_VALUE));
    }
    @Test void unsafeSettingsAndNegativeBalancesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new BusinessPurchasePolicy(-1,25,100));
        assertThrows(IllegalArgumentException.class,()->new BusinessPurchasePolicy(0,101,100));
        assertThrows(IllegalArgumentException.class,()->BusinessPurchasePolicy.prototype().businessAvailable(-1));
        assertThrows(IllegalArgumentException.class,()->BusinessPurchasePolicy.prototype().estateAvailable(-1));
        assertEquals(0,BusinessPurchasePolicy.disabled().businessAvailable(Long.MAX_VALUE));
    }
}
