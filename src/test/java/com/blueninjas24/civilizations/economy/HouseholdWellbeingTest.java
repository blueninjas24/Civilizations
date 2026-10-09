package com.blueninjas24.civilizations.economy;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HouseholdWellbeingTest {
    private final UUID home = UUID.randomUUID();
    private final HouseholdWellbeing.Policy policy = HouseholdWellbeing.Policy.prototype();
    private HouseholdNeeds.Home needs(Integer capacity, Long supplied, Long streak) {
        return new HouseholdNeeds.Home(home,2,0,4,capacity,supplied,streak);
    }
    @Test void suppliedKnownHomeStartsModerateAndRecoversWithoutCreatingMoney() {
        var result=HouseholdWellbeing.assess(needs(2,4L,0L),null,policy,3);
        assertEquals(50,result.before());assertEquals(51,result.after());assertEquals(HouseholdWellbeing.Condition.SECURE,result.condition());
        assertTrue(result.explanation().contains("not money"));
    }
    @Test void foodPressureEscalatesOnlyAfterThatHouseholdsOwnStreak() {
        var first=HouseholdWellbeing.assess(needs(2,2L,1L),50,policy,3);
        assertEquals(47,first.after());assertEquals(HouseholdWellbeing.Condition.PRESSURED,first.condition());
        var crisis=HouseholdWellbeing.assess(needs(2,2L,3L),47,policy,3);
        assertEquals(42,crisis.after());assertEquals(HouseholdWellbeing.Condition.FOOD_CRISIS,crisis.condition());
    }
    @Test void unknownCapacityDoesNotPretendCrowdingOrComfortAndCrowdingAddsPressure() {
        var unknown=HouseholdWellbeing.assess(needs(null,4L,0L),50,policy,3);
        assertEquals(50,unknown.after());assertEquals(HouseholdWellbeing.Condition.UNKNOWN,unknown.condition());
        var crowded=HouseholdWellbeing.assess(needs(1,4L,0L),50,policy,3);assertEquals(49,crowded.after());
        assertEquals(44,HouseholdWellbeing.assess(needs(1,0L,3L),50,policy,3).after());
        assertEquals(50,HouseholdWellbeing.assess(needs(2,null,null),50,policy,3).after());
    }
    @Test void scoresClampAndBadPoliciesOrPriorScoresFail() {
        assertEquals(100,HouseholdWellbeing.assess(needs(2,4L,0L),100,policy,3).after());
        assertEquals(0,HouseholdWellbeing.assess(needs(1,0L,4L),0,policy,3).after());
        assertThrows(IllegalArgumentException.class,()->HouseholdWellbeing.assess(needs(2,4L,0L),-1,policy,3));
        assertThrows(IllegalArgumentException.class,()->new HouseholdWellbeing.Policy(true,50,1,5,3,1));
        assertThrows(IllegalArgumentException.class,()->HouseholdWellbeing.assess(needs(2,4L,0L),50,HouseholdWellbeing.Policy.disabled(),3));
    }
    @Test void retunedStartingAmountDoesNotResetAnExistingScore() {
        var changed=new HouseholdWellbeing.Policy(true,99,2,4,8,3);
        assertEquals(42,HouseholdWellbeing.assess(needs(2,4L,0L),40,changed,3).after());
        assertEquals(100,HouseholdWellbeing.assess(needs(2,4L,0L),null,changed,3).after());
    }
    @Test void seededLongRunFamineRecoveryAndUnknownCyclesRemainDeterministicAndBounded() {
        for (long seed : new long[]{0,1,872613,Long.MAX_VALUE}) {
            assertEquals(run(seed),run(seed));
        }
    }
    private List<Integer> run(long seed) {
        var random=new SplittableRandom(seed);int score=50;long streak=0;var checkpoints=new ArrayList<Integer>();
        for(int period=1;period<=10000;period++) {
            long supplied=period<=2000 ? 0 : period<=4000 ? 4 : random.nextLong(5);
            streak=supplied==4 ? 0 : streak+1;
            var result=HouseholdWellbeing.assess(needs(period<=4000 ? 2 : random.nextInt(1,4),supplied,streak),score,policy,3);
            score=result.after();assertTrue(score>=0&&score<=100);
            if(period==2000)assertEquals(0,score);
            if(period==4000)assertEquals(100,score);
            if(period%100==0)checkpoints.add(score);
        }
        return checkpoints;
    }
}
