package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BusinessAllocationPolicyTest {
    @Test void urgencyOverridesDemandWhileEqualPriorityRotatesAndLargeValuesStayBounded() {
        var a=new BusinessAllocationPolicy.Candidate(new UUID(0,1),false,Long.MAX_VALUE);
        var b=new BusinessAllocationPolicy.Candidate(new UUID(0,2),true,1);
        assertEquals(b,BusinessAllocationPolicy.prototype().rank(List.of(a,b),"").getFirst());
        var equal=new BusinessAllocationPolicy.Candidate(new UUID(0,3),true,1);
        assertEquals(equal,BusinessAllocationPolicy.prototype().rank(List.of(b,equal),b.business().toString()).getFirst());
        var policy=new BusinessAllocationPolicy(true,1000,false,Set.of(),1_000_000_000);
        assertEquals(a,policy.rank(List.of(a,b),"").getFirst());
        assertThrows(IllegalArgumentException.class,()->policy.rank(List.of(a,a),""));
    }
}
