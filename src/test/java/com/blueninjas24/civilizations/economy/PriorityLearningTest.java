package com.blueninjas24.civilizations.economy;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import com.blueninjas24.civilizations.economy.VillagePriorities.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import static org.junit.jupiter.api.Assertions.*;

class PriorityLearningTest {
    private final UUID town=UUID.fromString("00000000-0000-0000-0000-000000000051");
    private final Policy policy=Policy.prototype().withLearning(12,25);
    private Candidate food(Decision decision) {return decision.candidates().stream().filter(c->c.action()==Action.SEEK_FOOD).findFirst().orElseThrow();}
    @Test void verifiedImmediateReliefAddsOnlyABoundedFoodBonus() {
        var facts=new Facts(town,20,Condition.SHORTAGE,5,1);
        var decision=VillagePriorities.review(1,facts,policy,new Evidence(5,5));
        assertEquals(1250,food(decision).weight());assertTrue(food(decision).explanation().contains("25%"));
        assertEquals(1000,food(VillagePriorities.review(1,facts,policy,new Evidence(5,0))).weight());
        assertEquals(1000,food(VillagePriorities.review(1,facts,policy,Evidence.none())).weight());
    }
    @Test void partialReliefUsesExactRatiosWithoutOverflow() {
        assertEquals(8,new Evidence(12,4).bonus(policy));
        assertEquals(25,new Evidence(Long.MAX_VALUE,Long.MAX_VALUE).bonus(policy));
        assertEquals(0,new Evidence(Long.MAX_VALUE,1).bonus(policy));
    }
    @Test void learningCannotEnableBlockedResponsesAcrossSeeds() {
        var facts=new Facts(town,20,Condition.BALANCED,0,0);
        for(int seed=0;seed<1000;seed++) {
            var decision=VillagePriorities.review(seed,facts,policy,new Evidence(10,10));
            assertEquals(Action.MONITOR,decision.selected());assertFalse(food(decision).feasible());assertFalse(food(decision).eligible());
        }
    }
    @Test void learnedChoicesAreRepeatableAndCanDifferFromUnlearnedChoices() {
        var facts=new Facts(town,20,Condition.SHORTAGE,5,1);boolean changed=false;
        for(int seed=0;seed<1000;seed++) {
            var learned=VillagePriorities.review(seed,facts,policy,new Evidence(5,5));
            assertEquals(learned,VillagePriorities.review(seed,facts,policy,new Evidence(5,5)));
            if(learned.selected()!=VillagePriorities.review(seed,facts,policy,Evidence.none()).selected())changed=true;
        }
        assertTrue(changed);
    }
    @Test void disabledPolicyPreservesLegacyChoicesAndExplanations() {
        var facts=new Facts(town,20,Condition.SHORTAGE,5,1);
        for(int seed=0;seed<100;seed++) {
            var original=VillagePriorities.review(seed,facts,Policy.prototype());
            var supplied=VillagePriorities.review(seed,facts,Policy.prototype(),new Evidence(5,5));
            assertEquals(original.candidates(),supplied.candidates());assertEquals(original.selected(),supplied.selected());
        }
    }
    @Test void invalidEvidenceAndUnsafePoliciesFail() {
        assertThrows(IllegalArgumentException.class,()->new Evidence(5,6));
        assertThrows(IllegalArgumentException.class,()->new Evidence(-1,0));
        assertThrows(IllegalArgumentException.class,()->policy.withLearning(0,25));
        assertThrows(IllegalArgumentException.class,()->policy.withLearning(101,25));
        assertThrows(IllegalArgumentException.class,()->policy.withLearning(12,101));
        assertEquals(0,policy.withLearning(0,0).learningPeriods());
    }
}
