package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopInputPlanTest {
    final Commodity tools=new Commodity("tools");
    final ProductionRecipe recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,4);
    final Map<Commodity,Long> empty=Map.of(Commodity.IRON,0L,Commodity.COAL,0L,tools,0L),sizes=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,tools,1L);
    final WorkshopSupplyPolicy policy=new WorkshopSupplyPolicy(true,2,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L));
    final WorkshopInputPlan.Warehouse warehouse=new WorkshopInputPlan.Warehouse(new UUID(0,1),Map.of(Commodity.IRON,100L,Commodity.COAL,100L));
    WorkshopInputPlan.Plan plan(long funds,long space,long haul,int slots,Map<Commodity,Long> held,Map<Commodity,Long> pending,List<WorkshopInputPlan.Warehouse> sellers) {
        return WorkshopInputPlan.choose(recipe,2,funds,space,haul,slots,held,pending,sizes,sellers,policy);
    }
    @Test void buysOnlyLargestAffordableCompleteBundle() {
        var p=plan(1500,100,100,128,empty,Map.of(),List.of(warehouse));
        assertEquals(3,p.batches());assertEquals(1500,p.price());assertEquals(2,p.orders().size());
        assertEquals(6,p.orders().stream().filter(o->o.commodity().equals(Commodity.IRON)).findFirst().orElseThrow().units());
        assertEquals(0,plan(499,100,100,128,empty,Map.of(),List.of(warehouse)).batches());
    }
    @Test void missingOneMaterialDoesNotBuyOtherMaterialSpeculatively() {
        var coalOnly=new WorkshopInputPlan.Warehouse(new UUID(0,2),Map.of(Commodity.COAL,100L));
        var p=plan(10000,100,100,128,empty,Map.of(),List.of(coalOnly));assertEquals(0,p.batches());assertTrue(p.orders().isEmpty());
    }
    @Test void heldAndPendingInputsPreventDuplicateOrdersButPlannerDoesNotProduce() {
        var held=Map.of(Commodity.COAL,2L,Commodity.IRON,0L,tools,0L);
        var p=plan(0,100,100,128,held,Map.of(Commodity.IRON,4L),List.of());
        assertEquals(2,p.batches());assertEquals(0,p.price());assertTrue(p.orders().isEmpty());assertEquals(0,held.get(tools));
    }
    @Test void transportStorageAndOrderReviewEachBoundActualPurchases() {
        assertEquals(1,plan(10000,100,5,128,empty,Map.of(),List.of(warehouse)).batches());
        assertEquals(1,plan(10000,5,100,128,empty,Map.of(),List.of(warehouse)).batches());
        assertEquals(0,plan(10000,100,100,1,empty,Map.of(),List.of(warehouse)).batches());
        var expands=new ProductionRecipe("expands","blacksmith",Map.of(Commodity.COAL,1L),Map.of(tools,5L),1,4);
        assertEquals(1,WorkshopInputPlan.choose(expands,2,10000,9,100,128,empty,Map.of(),sizes,List.of(warehouse),policy).batches());
    }
    @Test void noLaborDisabledOrUnknownPriceNeverManufacturesPurchasingPower() {
        assertEquals(0,WorkshopInputPlan.choose(recipe,0,10000,100,100,128,empty,Map.of(),sizes,List.of(warehouse),policy).batches());
        assertEquals(0,WorkshopInputPlan.choose(recipe,2,10000,100,100,128,empty,Map.of(),sizes,List.of(warehouse),WorkshopSupplyPolicy.disabled()).batches());
        assertEquals(0,WorkshopInputPlan.choose(recipe,2,10000,100,100,128,empty,Map.of(),sizes,List.of(warehouse),new WorkshopSupplyPolicy(true,2,16,Map.of())).batches());
    }
    @Test void stableSellerOrderingAndCopiedSnapshotsSurviveCallerMutation() {
        var mutable=new HashMap<>(warehouse.stock());var first=new WorkshopInputPlan.Warehouse(warehouse.id(),mutable);mutable.clear();
        var second=new WorkshopInputPlan.Warehouse(new UUID(0,2),warehouse.stock());
        var a=plan(10000,100,100,128,empty,Map.of(),List.of(second,first));
        assertEquals(a,plan(10000,100,100,128,empty,Map.of(),List.of(first,second)));
        assertTrue(a.orders().stream().allMatch(o->o.seller().equals(first.id())));
        assertThrows(IllegalArgumentException.class,()->plan(10000,100,100,128,empty,Map.of(),List.of(first,first)));
    }
    @Test void largeCatalogWeightsCannotOverflowIntoAffordableStorage() {
        var heavy=new HashMap<>(sizes);heavy.put(tools,Long.MAX_VALUE);
        var expanding=new ProductionRecipe("heavy","blacksmith",Map.of(Commodity.COAL,1L),Map.of(tools,2L),1,4);
        assertEquals(0,WorkshopInputPlan.choose(expanding,1,Long.MAX_VALUE,Long.MAX_VALUE,100,128,empty,Map.of(),heavy,List.of(warehouse),policy).batches());
    }
}
