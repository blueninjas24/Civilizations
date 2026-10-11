package com.blueninjas24.civilizations.industry;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolUpkeepTest {
    final ToolUpkeepPolicy policy=new ToolUpkeepPolicy(true,1,2,50,400,Set.of("mining"));
    @Test void graceAndWearOnlyAdvanceOnActualProductivePeriods() {
        var first=ToolUpkeep.prepare(ToolUpkeep.State.initial(),2,1,policy);assertTrue(first.grace());
        var idle=ToolUpkeep.finish(first,0,policy);assertEquals(ToolUpkeep.State.initial(),idle.after());assertEquals(0,idle.toolsUsed());
        var grace=ToolUpkeep.finish(first,1,policy);assertEquals(1,grace.after().productivePeriods());assertEquals(0,grace.toolsUsed());
        var used=ToolUpkeep.finish(ToolUpkeep.prepare(grace.after(),2,1,policy),2,policy);assertEquals(1,used.toolsUsed());assertEquals(1,used.after().servicePeriods());
        var stillIdle=ToolUpkeep.finish(ToolUpkeep.prepare(used.after(),2,0,policy),0,policy);assertEquals(used.after(),stillIdle.after());
        var last=ToolUpkeep.finish(ToolUpkeep.prepare(used.after(),2,0,policy),1,policy);assertEquals(0,last.after().servicePeriods());assertEquals(0,last.toolsUsed());
    }
    @Test void smallBusinessesAccumulateSlowedWorkWithoutRoundingToPermanentZero() {
        var state=new ToolUpkeep.State(1,0,0);var first=ToolUpkeep.prepare(state,1,0,policy);assertEquals(0,first.requested());
        state=ToolUpkeep.finish(first,0,policy).after();assertEquals(50,state.remainderPercent());
        var second=ToolUpkeep.prepare(state,1,0,policy);assertEquals(1,second.requested());
        var after=ToolUpkeep.finish(second,1,policy);assertEquals(0,after.after().remainderPercent());assertEquals(2,after.after().productivePeriods());assertEquals(0,after.toolsUsed());
    }
    @Test void noWorkersCannotEarnFractionalWorkOrWearTools() {
        var before=new ToolUpkeep.State(4,0,75);var plan=ToolUpkeep.prepare(before,0,0,policy);
        assertEquals(before,ToolUpkeep.finish(plan,0,policy).after());
        assertThrows(IllegalArgumentException.class,()->ToolUpkeep.finish(plan,1,policy));
    }
    @Test void savedServiceSurvivesRetuningWithoutRefillingOrDiscardingTools() {
        var before=new ToolUpkeep.State(10,3,50);var changed=new ToolUpkeepPolicy(true,0,99,25,800,Set.of("mining"));
        var after=ToolUpkeep.finish(ToolUpkeep.prepare(before,4,0,changed),4,changed);
        assertEquals(2,after.after().servicePeriods());assertEquals(50,after.after().remainderPercent());assertEquals(0,after.toolsUsed());
    }
    @Test void configurationAndStateAreValidated() {
        assertThrows(IllegalArgumentException.class,()->new ToolUpkeepPolicy(true,0,0,50,400,Set.of("mining")));
        assertThrows(IllegalArgumentException.class,()->new ToolUpkeepPolicy(true,0,2,0,400,Set.of("mining")));
        assertThrows(IllegalArgumentException.class,()->new ToolUpkeepPolicy(true,0,2,50,400,Set.of("warehouse")));
        assertThrows(IllegalArgumentException.class,()->new ToolUpkeep.State(0,0,100));
    }
}
