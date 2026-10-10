package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WorkLearningPolicyTest {
    @Test void gradualGainCapsWithoutReducingExistingExperienceOrOverflowing() {
        var policy=new WorkLearningPolicy(true,3,10,64,Map.of("mining","mining"));
        assertEquals(3,policy.nextExperience(0));assertEquals(10,policy.nextExperience(9));
        assertEquals(11,policy.nextExperience(11));assertEquals(Long.MAX_VALUE,policy.nextExperience(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,()->policy.nextExperience(-1));
    }
    @Test void limitsAndSkillKeysAreStrictAndCatalogIsImmutable() {
        assertThrows(IllegalArgumentException.class,()->new WorkLearningPolicy(true,0,100,64,Map.of()));
        assertThrows(IllegalArgumentException.class,()->new WorkLearningPolicy(true,1,0,64,Map.of()));
        assertThrows(IllegalArgumentException.class,()->new WorkLearningPolicy(true,1,100,0,Map.of()));
        assertThrows(IllegalArgumentException.class,()->new WorkLearningPolicy(true,1,100,64,Map.of("mining","")));
        assertThrows(UnsupportedOperationException.class,()->WorkLearningPolicy.prototype().industrySkills().put("mining","magic"));
    }
}
