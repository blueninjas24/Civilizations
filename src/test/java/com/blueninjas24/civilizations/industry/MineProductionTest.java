package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MineProductionTest {
    static final Map<Commodity,Long> BUNDLE=Map.of(Commodity.COAL,1L,Commodity.STONE,3L);
    static final Map<Commodity,Long> SPACE=Map.of(Commodity.COAL,1L,Commodity.STONE,1L);
    @Test void oneWorkBudgetProducesMineralAndRockTogetherWithinEveryLimit() {
        var plan=MineProduction.calculate(1200,100,3,8,32,1024,BUNDLE,SPACE);
        assertEquals(24,plan.batches());assertEquals(24L,plan.outputs().get(Commodity.COAL));
        assertEquals(72L,plan.outputs().get(Commodity.STONE));assertEquals(96,plan.storageSpace());
        assertEquals(2,MineProduction.calculate(1200,100,3,8,32,9,BUNDLE,SPACE).batches());
        assertEquals(5,MineProduction.calculate(5,100,3,8,32,1024,BUNDLE,SPACE).batches());
        assertEquals(0,MineProduction.calculate(1200,100,0,8,32,1024,BUNDLE,SPACE).batches());
        assertEquals(0,MineProduction.calculate(0,100,3,8,32,1024,BUNDLE,SPACE).batches());
    }
    @Test void seededLimitsConserveGeologicalBundleOverTenThousandCases() {
        var random=new Random(413);for(int i=0;i<10000;i++) {
            long remaining=random.nextInt(3000),requested=random.nextInt(2000),space=random.nextInt(5000);
            int workers=random.nextInt(100);long capacity=random.nextInt(300)+1;
            var plan=MineProduction.calculate(remaining,requested,workers,8,capacity,space,BUNDLE,SPACE);
            assertTrue(plan.batches()<=remaining && plan.batches()<=requested && plan.batches()<=workers*8L && plan.batches()<=capacity);
            assertTrue(plan.storageSpace()<=space);assertEquals(plan.batches()*4,plan.storageSpace());
            assertEquals(plan.batches()*3,plan.outputs().get(Commodity.STONE));
        }
    }
    @Test void unsafeSettingsMissingStorageAndOverflowFail() {
        assertThrows(IllegalArgumentException.class,()->MineProduction.calculate(1,1,1,1,1,1,BUNDLE,Map.of()));
        assertThrows(ArithmeticException.class,()->MineProduction.calculate(1,1,2,Long.MAX_VALUE,1,1,BUNDLE,SPACE));
        assertThrows(IllegalArgumentException.class,()->new IndustryPolicy(0,1,1,1,1,1,BUNDLE,SPACE));
        assertEquals(1200,IndustryPolicy.prototype().initialDepositBatches(8));
        assertThrows(IllegalArgumentException.class,()->IndustryPolicy.prototype().initialDepositBatches(0));
    }
}
