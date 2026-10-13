package com.blueninjas24.civilizations.economy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GrainHarvestTest {
    FarmObservation farm(boolean complete,Long wheat){return new FarmObservation(new UUID(0,1),new UUID(0,2),1,complete,32,null,wheat);}
    GrainHarvest.Plan plan(long output,long food,long grain,Long wheat,GrainHarvestPolicy policy){return GrainHarvest.plan(output,food,0,4,grain,farm(true,wheat),true,policy);}
    @Test void onlyActualFutureWorkSplitsAndBasicMealsRemainProtected() {
        var p=plan(8,12,0,32L,GrainHarvestPolicy.prototype());assertEquals(2,p.diverted());assertEquals(6,p.foodOutput());assertEquals(2,p.grainUnits());assertEquals(12,p.protectedFood());
        assertEquals(p.harvest(),p.foodOutput()+p.diverted());
        var shortage=plan(8,0,0,32L,GrainHarvestPolicy.prototype());assertEquals(0,shortage.grainUnits());assertEquals(8,shortage.foodOutput());
        var barely=plan(8,5,0,32L,GrainHarvestPolicy.prototype());assertEquals(1,barely.diverted());assertEquals(12,barely.availableFood()-barely.diverted());
    }
    @Test void wheatFractionStorageAndYieldBoundWorkWithoutDeletingOldStock() {
        assertEquals(1,plan(8,12,0,16L,GrainHarvestPolicy.prototype()).grainUnits());
        var policy=new GrainHarvestPolicy(true,100,0,5,3);
        var p=plan(8,12,0,32L,policy);assertEquals(1,p.diverted());assertEquals(3,p.grainUnits());
        assertEquals(0,plan(8,12,4,32L,policy).grainUnits());assertEquals(20,plan(8,12,20,32L,policy).closingGrain());
    }
    @Test void unknownPartialUnstaffedDisabledAndUnproductiveFieldsCannotMintGrain() {
        for(Long wheat:new Long[]{null,0L})assertEquals(0,plan(8,12,0,wheat,GrainHarvestPolicy.prototype()).grainUnits());
        assertEquals(0,GrainHarvest.plan(8,12,0,4,0,farm(false,32L),true,GrainHarvestPolicy.prototype()).grainUnits());
        assertEquals(0,GrainHarvest.plan(8,12,0,4,0,farm(true,32L),false,GrainHarvestPolicy.prototype()).grainUnits());
        assertEquals(0,plan(8,12,0,32L,GrainHarvestPolicy.disabled()).grainUnits());assertEquals(0,plan(0,12,0,32L,GrainHarvestPolicy.prototype()).grainUnits());
        assertEquals(0,plan(8,12,0,32L,new GrainHarvestPolicy(true,0,0,256,1)).grainUnits());
    }
    @Test void invalidTermsAndArithmeticOverflowFailInsteadOfWrapping() {
        assertThrows(IllegalArgumentException.class,()->new GrainHarvestPolicy(true,101,2,256,1));assertThrows(IllegalArgumentException.class,()->plan(-1,12,0,32L,GrainHarvestPolicy.prototype()));
        assertThrows(ArithmeticException.class,()->plan(Long.MAX_VALUE,12,0,32L,GrainHarvestPolicy.prototype()));
        assertThrows(ArithmeticException.class,()->GrainHarvest.plan(0,0,0,Long.MAX_VALUE,0,farm(true,32L),true,GrainHarvestPolicy.prototype()));
    }
}
