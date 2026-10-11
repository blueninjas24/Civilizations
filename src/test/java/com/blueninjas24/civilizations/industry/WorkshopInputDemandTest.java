package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopInputDemandTest {
    final Commodity tools=new Commodity("tools");
    final ProductionRecipe recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,8);
    final Map<Commodity,Long> empty=Map.of(Commodity.IRON,0L,Commodity.COAL,0L,tools,0L);
    final Map<Commodity,Long> sizes=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,tools,1L);
    final WorkshopSupplyPolicy policy=new WorkshopSupplyPolicy(true,2,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L));
    WorkshopInputDemand.Review review(long workers,long funds,long space,Map<Commodity,Long> held,Map<Commodity,Long> pending) {
        return WorkshopInputDemand.review(recipe,workers,funds,space,held,pending,sizes,policy);
    }
    @Test void bufferRequiresMoneyForCompleteInputsInsteadOfUnfundedSingleCommodityPressure() {
        assertEquals(0,review(1,499,100,empty,Map.of()).batches());
        var r=review(1,500,100,empty,Map.of());assertEquals(1,r.batches());assertEquals(500,r.estimatedCost());
        assertEquals(Map.of(Commodity.IRON,2L,Commodity.COAL,1L),r.missing());
        assertThrows(UnsupportedOperationException.class,()->r.missing().clear());
    }
    @Test void ownedAndPaidPendingInputsReduceGapButNeverBecomeGoodsProduced() {
        var held=Map.of(Commodity.IRON,2L,Commodity.COAL,0L,tools,0L);
        var r=review(1,0,100,held,Map.of(Commodity.COAL,1L));assertEquals(1,r.batches());assertTrue(r.missing().isEmpty());
        assertEquals(0,r.estimatedCost());assertEquals(0,held.get(tools));
        assertEquals(Map.of(Commodity.COAL,1L),review(1,100,100,held,Map.of()).missing());
    }
    @Test void laborSiteBufferAndStorageBoundPressure() {
        assertTrue(review(0,10000,100,empty,Map.of()).missing().isEmpty());
        assertEquals(2,review(1,10000,100,empty,Map.of()).batches());
        assertEquals(16,review(100,10000,100,empty,Map.of()).batches());
        assertEquals(0,review(1,10000,2,empty,Map.of()).batches());
        assertEquals(1,review(1,10000,3,empty,Map.of()).batches());
    }
    @Test void futureOutputGrowthMustFitAlongsideNewInputs() {
        var growing=new ProductionRecipe("growth","blacksmith",Map.of(Commodity.IRON,1L),Map.of(tools,3L),1,1);
        assertEquals(0,WorkshopInputDemand.review(growing,1,1000,2,empty,Map.of(),sizes,policy).batches());
        assertEquals(1,WorkshopInputDemand.review(growing,1,1000,3,empty,Map.of(),sizes,policy).batches());
    }
    @Test void disabledMissingPricesAndInvalidResourcesNeverInventAffordableDemand() {
        assertTrue(WorkshopInputDemand.review(recipe,1,1000,100,empty,Map.of(),sizes,WorkshopSupplyPolicy.disabled()).missing().isEmpty());
        var noPrices=new WorkshopSupplyPolicy(true,2,16,Map.of());
        assertEquals(0,WorkshopInputDemand.review(recipe,1,1000,100,empty,Map.of(),sizes,noPrices).batches());
        assertThrows(IllegalArgumentException.class,()->review(-1,100,100,empty,Map.of()));
        assertThrows(IllegalArgumentException.class,()->review(1,100,-1,empty,Map.of()));
        assertThrows(IllegalArgumentException.class,()->review(1,100,100,Map.of(),Map.of()));
    }
    @Test void fundedBufferMatchesExistingSupplyPlannerWhenRealSuppliersHaveEnoughStock() {
        var warehouse=new WorkshopInputPlan.Warehouse(UUID.randomUUID(),Map.of(Commodity.IRON,100L,Commodity.COAL,100L));
        var supply=WorkshopInputPlan.choose(recipe,2,1300,100,128,128,empty,Map.of(),sizes,List.of(warehouse),policy);
        var demand=review(2,1300,100,empty,Map.of());assertEquals(supply.batches(),demand.batches());assertEquals(supply.price(),demand.estimatedCost());
    }
}
