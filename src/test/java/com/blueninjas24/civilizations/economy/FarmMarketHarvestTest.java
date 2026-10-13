package com.blueninjas24.civilizations.economy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmMarketHarvestTest {
    FarmObservation field(Long wheat){return new FarmObservation(new UUID(0,1),new UUID(0,2),1000,true,32,null,wheat);}
    FarmMarketHarvest.Plan plan(long food,long room,Long wheat){return FarmMarketHarvest.plan(8,food,0,4,0,room,field(wheat),GrainHarvestPolicy.prototype(),FarmMarketPolicy.prototype());}
    @Test void contributionIsCappedEvenWhenVillageDemandExceedsAllOutput() {
        var p=plan(0,256,32L);assertEquals(4,p.publicFood());assertEquals(4,p.businessFood());assertEquals(0,p.grainUnits());assertEquals(8,p.actualWork());
        assertTrue(p.publicFood()<=p.businessFood());
    }
    @Test void adequatePublicReservesLeaveNewHarvestEntirelyOwnedWithoutConvertingOldFood() {
        var p=plan(24,256,32L);assertEquals(0,p.publicFood());assertEquals(6,p.businessFood());assertEquals(2,p.grainUnits());assertEquals(8,p.actualWork());
        var needed=plan(8,256,32L);assertEquals(3,needed.publicFood());assertEquals(3,needed.businessFood());assertEquals(2,needed.grainUnits());
    }
    @Test void fullStoragePausesUnacceptedWorkRatherThanMakingContributionUnlimited() {
        var p=plan(0,0,32L);assertEquals(0,p.actualWork());assertEquals(0,p.publicFood());
        var small=plan(0,1,null);assertEquals(2,small.actualWork());assertEquals(1,small.publicFood());assertEquals(1,small.businessFood());
    }
    @Test void unknownWheatAndRealIncomingFoodHaveDistinctConservativeEffects() {
        var unknown=plan(24,256,null);assertEquals(8,unknown.businessFood());assertEquals(0,unknown.grainUnits());
        var delivered=FarmMarketHarvest.plan(8,0,12,4,0,256,field(32L),GrainHarvestPolicy.prototype(),FarmMarketPolicy.prototype());assertEquals(0,delivered.publicFood());assertEquals(6,delivered.businessFood());
    }
    @Test void exhaustiveSmallCasesConserveWorkEnforceShareAndNeverOverfill() {
        for(int percent:new int[]{0,25,50,100})for(int room=0;room<17;room++)for(int food=0;food<17;food++) {
            var p=FarmMarketHarvest.plan(8,food,0,4,0,room,field(32L),GrainHarvestPolicy.prototype(),new FarmMarketPolicy(true,percent,2,256));
            assertEquals(p.actualWork(),p.publicFood()+p.businessFood()+p.grainWork());assertTrue(p.actualWork()<=8);
            assertTrue(p.publicFood()*100<=(p.publicFood()+p.businessFood())*percent);assertTrue(p.grainUnits()+p.businessFood()<=room);
        }
    }
}
