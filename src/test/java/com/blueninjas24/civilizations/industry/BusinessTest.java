package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.population.JobRequirements;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BusinessTest {
    @Test void mismatchedIdentityAndUnsafeFoundingTermsFail() {
        UUID town=UUID.randomUUID(),person=UUID.randomUUID();
        var site=new BusinessSite(UUID.randomUUID(),town,UUID.randomUUID(),1,-32,2,10,"Observed field");
        var b=new Business(UUID.randomUUID(),town,"Farm",person,person,"farming",site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        assertThrows(IllegalArgumentException.class,()->new BusinessFounding(b,site,requirements,requirements,-1,"Opening"));
        var wrong=new BusinessSite(site.id(),UUID.randomUUID(),site.worldId(),1,-32,2,10,"Observed field");
        assertThrows(IllegalArgumentException.class,()->new BusinessFounding(b,wrong,requirements,requirements,0,"Opening"));
        assertThrows(IllegalArgumentException.class,()->new Business(b.id(),town,"Farm",person,person,"farming",site.id(),0,0));
        assertThrows(IllegalArgumentException.class,()->new BusinessSite(site.id(),town,site.worldId(),1,2,3,-1,"Evidence"));
    }

    @Test void historicalIntervalsRequireAnOrderedDateAndReason() {
        UUID id=UUID.randomUUID(),business=UUID.randomUUID(),resident=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new Employment(id,business,resident,10,9L,"Left"));
        assertThrows(IllegalArgumentException.class,()->new Employment(id,business,resident,10,null,"Left"));
        assertThrows(IllegalArgumentException.class,()->new Employment(id,business,resident,10,10L,null));
        assertEquals(10L,new Employment(id,business,resident,10,10L,"Left").endedStep());
    }
}
