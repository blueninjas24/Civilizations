package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpkeepContributionPolicyTest {
    @Test void originalCapitalReserveEarnedCashAndBothPeriodCapsStayProtected() {
        var p=UpkeepContributionPolicy.prototype();
        assertEquals(4000,p.available(20000,4000,10000,0,0,0));
        assertEquals(0,p.available(12000,4000,10000,0,0,0));
        assertEquals(500,p.available(20000,4000,10000,9500,0,0));
        assertEquals(300,p.available(20000,4000,10000,0,3700,0));
        assertEquals(200,p.available(20000,4000,10000,0,0,3800));
        assertEquals(0,p.available(20000,0,0,0,0,0));
        assertEquals(0,UpkeepContributionPolicy.disabled().available(20000,0,10000,0,0,0));
    }
    @Test void arithmeticBoundariesNeverTurnOverflowIntoSpareCash() {
        var p=UpkeepContributionPolicy.prototype();
        assertEquals(0,p.available(Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,0,0,0));
        assertEquals(4000,p.available(Long.MAX_VALUE,0,Long.MAX_VALUE,0,0,0));
        assertThrows(IllegalArgumentException.class,()->p.available(-1,0,0,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new UpkeepContributionPolicy(true,8000,101,4000,4000));
    }
}
