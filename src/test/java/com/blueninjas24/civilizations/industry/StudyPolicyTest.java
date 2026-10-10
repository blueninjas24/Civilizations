package com.blueninjas24.civilizations.industry;
import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class StudyPolicyTest {
    @Test void prototypeHasEditableBoundedNonfoodCostsAndDisabledLegacyPolicy(){
        assertEquals(10,StudyPolicy.prototype().skills().get("mining").targetExperience());assertEquals(2,StudyPolicy.prototype().studentsPerPeriod());assertFalse(StudyPolicy.disabled().enabled());
        assertThrows(IllegalArgumentException.class,()->new StudyPolicy.Course(1,Map.of(Commodity.FOOD,1L)));
        assertThrows(IllegalArgumentException.class,()->new StudyPolicy.Course(1,Map.of()));
        assertThrows(IllegalArgumentException.class,()->new StudyPolicy(true,1,1,2,8,1,2,StudyPolicy.prototype().skills()));
        assertThrows(IllegalArgumentException.class,()->new StudyPolicy(true,1,100,9,8,1,2,Map.of()));
    }
}
