package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LivestockBirthTest {
    final UUID business=new UUID(1,2);
    @Test void birthIdentitySurvivesReplayAndDifferentMaturitySettingsWithoutCollidingWithOtherBirths() {
        var first=LivestockBirth.batch(business,7,1000,12);assertEquals(first,LivestockBirth.batch(business,7,1000,12));
        var ids=new HashSet<UUID>();first.forEach(b->{assertTrue(ids.add(b.id()));assertEquals(19,b.maturePeriod());});
        LivestockBirth.batch(business,8,1000,12).forEach(b->assertTrue(ids.add(b.id())));
        LivestockBirth.batch(new UUID(3,4),7,1000,12).forEach(b->assertTrue(ids.add(b.id())));
        assertEquals(first.getFirst().id(),LivestockBirth.batch(business,7,1,3).getFirst().id());
        assertThrows(UnsupportedOperationException.class,()->first.clear());
    }
    @Test void zeroBirthsCreateNoIdentityOrUnnecessaryMaturityOverflow() {
        assertTrue(LivestockBirth.batch(business,Long.MAX_VALUE,0,1000).isEmpty());
        assertThrows(ArithmeticException.class,()->LivestockBirth.batch(business,Long.MAX_VALUE,1,1));
        assertEquals(Long.MAX_VALUE,LivestockBirth.batch(business,Long.MAX_VALUE-1,1,1).getFirst().maturePeriod());
    }
    @Test void invalidCountsPeriodsMaturityAndForgedIdentityCannotBecomeARecordedAnimal() {
        assertThrows(IllegalArgumentException.class,()->LivestockBirth.batch(business,0,1,12));
        assertThrows(IllegalArgumentException.class,()->LivestockBirth.batch(business,1,1001,12));
        assertThrows(IllegalArgumentException.class,()->LivestockBirth.batch(business,1,-1,12));
        assertThrows(IllegalArgumentException.class,()->LivestockBirth.batch(business,1,1,0));
        assertThrows(IllegalArgumentException.class,()->LivestockBirth.batch(business,1,1,1001));
        assertThrows(IllegalArgumentException.class,()->new LivestockBirth(UUID.randomUUID(),business,1,0,13));
    }
}
