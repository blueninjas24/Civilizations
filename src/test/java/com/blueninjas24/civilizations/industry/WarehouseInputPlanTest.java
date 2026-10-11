package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarehouseInputPlanTest {
    final UUID first=new UUID(0,1),second=new UUID(0,2);
    final Map<Commodity,Long> prices=Map.of(Commodity.COAL,100L,Commodity.IRON,200L);
    final Map<Commodity,Long> sizes=Map.of(Commodity.COAL,1L,Commodity.IRON,2L);
    List<WorkshopInputPlan.Warehouse> sources(){return List.of(new WorkshopInputPlan.Warehouse(second,Map.of(Commodity.IRON,10L)),new WorkshopInputPlan.Warehouse(first,Map.of(Commodity.COAL,10L,Commodity.IRON,2L)));}
    @Test void purchasesAreDeterministicAndRespectSharedPendingDemand() {
        var orders=WarehouseInputPlan.choose(Map.of(Commodity.COAL,3L,Commodity.IRON,4L),Map.of(Commodity.COAL,2L,Commodity.IRON,1L),10000,100,100,128,sizes,sources(),prices);
        assertEquals(List.of(new WorkshopInputPlan.Order(first,Commodity.COAL,1,100),new WorkshopInputPlan.Order(first,Commodity.IRON,2,400),new WorkshopInputPlan.Order(second,Commodity.IRON,1,200)),orders);
        assertThrows(UnsupportedOperationException.class,()->orders.clear());
    }
    @Test void fundsStorageHaulingAndReceiptSlotsEachBoundActualGoods() {
        var demand=Map.of(Commodity.IRON,100L);
        assertEquals(1,WarehouseInputPlan.choose(demand,Map.of(),300,100,100,128,sizes,sources(),prices).getFirst().units());
        assertEquals(1,WarehouseInputPlan.choose(demand,Map.of(),10000,3,100,128,sizes,sources(),prices).getFirst().units());
        assertEquals(1,WarehouseInputPlan.choose(demand,Map.of(),10000,100,1,128,sizes,sources(),prices).getFirst().units());
        assertEquals(1,WarehouseInputPlan.choose(demand,Map.of(),10000,100,100,1,sizes,sources(),prices).size());
        assertTrue(WarehouseInputPlan.choose(demand,Map.of(),10000,100,100,0,sizes,sources(),prices).isEmpty());
    }
    @Test void noNeedNoMoneyMissingPricesOrUnsupportedStorageNeverCreatesOrders() {
        assertTrue(WarehouseInputPlan.choose(Map.of(),Map.of(),100,100,100,128,sizes,sources(),prices).isEmpty());
        assertTrue(WarehouseInputPlan.choose(Map.of(Commodity.IRON,2L),Map.of(Commodity.IRON,4L),100,100,100,128,sizes,sources(),prices).isEmpty());
        assertTrue(WarehouseInputPlan.choose(Map.of(Commodity.IRON,2L),Map.of(),0,100,100,128,sizes,sources(),prices).isEmpty());
        assertTrue(WarehouseInputPlan.choose(Map.of(Commodity.IRON,2L),Map.of(),1000,100,100,128,Map.of(),sources(),prices).isEmpty());
        assertTrue(WarehouseInputPlan.choose(Map.of(Commodity.IRON,2L),Map.of(),1000,100,100,128,sizes,sources(),Map.of()).isEmpty());
    }
    @Test void invalidResourcesAndDuplicateSourcesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->WarehouseInputPlan.choose(Map.of(Commodity.IRON,-1L),Map.of(),1000,100,100,128,sizes,sources(),prices));
        assertThrows(IllegalArgumentException.class,()->WarehouseInputPlan.choose(Map.of(),Map.of(),1000,100,100,129,sizes,sources(),prices));
        var duplicate=List.of(sources().getFirst(),sources().getFirst());
        assertThrows(IllegalArgumentException.class,()->WarehouseInputPlan.choose(Map.of(),Map.of(),1000,100,100,128,sizes,duplicate,prices));
    }
}
