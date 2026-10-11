package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmToolBudgetPolicyTest {
    @Test void foodProtectionBudgetOverflowAndDisabledPolicyAreConservative() {
        var p=FarmToolBudgetPolicy.prototype();
        assertTrue(p.review(6500,2,0).eligible());assertEquals(2500,p.review(6500,2,0).protectedHundredths());
        assertFalse(p.review(6499,2,0).eligible());assertFalse(p.review(20000,2,1).eligible());
        assertEquals(8000,p.review(20000,20,0).protectedHundredths());assertFalse(p.review(11999,20,0).eligible());
        assertFalse(p.review(Long.MAX_VALUE,Long.MAX_VALUE,0).eligible());assertFalse(FarmToolBudgetPolicy.disabled().review(20000,2,0).eligible());
        assertThrows(IllegalArgumentException.class,()->p.review(0,-1,0));
    }
}
