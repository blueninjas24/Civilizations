package com.blueninjas24.civilizations.economy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodBargainingTest {
    private final FoodBargaining.Policy policy=FoodBargaining.Policy.prototype();
    @Test void fairAskingPriceIsAcceptedAndHigherPricesGetCurrentCounter() {
        assertEquals(2,FoodBargaining.discuss(2,3,1000,null,policy).offer());
        assertEquals(FoodBargaining.Status.AGREED,FoodBargaining.discuss(2,3,1000,null,policy).status());
        var counter=FoodBargaining.discuss(5,3,1000,null,policy);
        assertEquals(3,counter.offer());assertEquals(1,counter.failedAttempts());
        assertEquals(2,FoodBargaining.discuss(5,2,2000,counter,policy).offer());
    }
    @Test void failedAlignmentsCloseDealAndClockRollbackCannotShortenCooldown() {
        var first=FoodBargaining.discuss(5,3,1000,null,policy);
        var second=FoodBargaining.discuss(5,3,2000,first,policy);
        var closed=FoodBargaining.discuss(5,3,3000,second,policy);
        assertEquals(FoodBargaining.Status.CLOSED,closed.status());assertEquals(63000,closed.cooldownUntil());
        assertEquals(closed,FoodBargaining.discuss(1,3,0,closed,policy));
        assertEquals(FoodBargaining.Status.AGREED,FoodBargaining.discuss(1,3,63000,closed,policy).status());
    }
    @Test void countersDoNotKeepExtendingAnUnfinishedConversation() {
        var first=FoodBargaining.discuss(5,3,1000,null,policy);
        var second=FoodBargaining.discuss(5,3,2000,first,policy);
        assertEquals(first.expiresMillis(),second.expiresMillis());
        assertEquals(1,FoodBargaining.discuss(5,3,46000,second,policy).failedAttempts());
    }
    @Test void configurablePatienceAndCooldownAreExactAndInvalidInputsFail() {
        var strict=new FoodBargaining.Policy(true,1,2,3);
        var closed=FoodBargaining.discuss(2,1,1000,null,strict);
        assertEquals(3000,closed.cooldownUntil());assertEquals(4000,closed.expiresMillis());
        assertThrows(IllegalArgumentException.class,()->new FoodBargaining.Policy(true,0,60,45));
        assertThrows(IllegalArgumentException.class,()->FoodBargaining.discuss(1,0,1000,null,policy));
        assertThrows(ArithmeticException.class,()->FoodBargaining.discuss(2,1,Long.MAX_VALUE,null,policy));
    }
}
