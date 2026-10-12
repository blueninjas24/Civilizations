package com.blueninjas24.civilizations.industry;
import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VillageSupplyPolicyTest {
    @Test void budgetsFoodProtectionGoodsAndHaulingAllBoundPurchases() {
        var p=VillageSupplyPolicy.prototype();
        assertEquals(10,p.affordable(Commodity.FOOD,10_000,20,0,100,100,100));
        assertEquals(0,p.affordable(Commodity.STONE,8000,20,0,100,100,100));
        assertEquals(1,p.affordable(Commodity.STONE,8100,20,0,100,100,100));
        assertEquals(2,p.affordable(Commodity.STONE,10_000,20,800,100,100,100));
        assertEquals(1,p.affordable(Commodity.STONE,10_000,20,0,100,100,1));
        assertEquals(0,p.affordable(Commodity.STONE,Long.MAX_VALUE,Long.MAX_VALUE,0,100,100,100));
        assertEquals(0,VillageSupplyPolicy.disabled().affordable(Commodity.FOOD,10_000,1,0,10,10,10));
    }
    @Test void disabledOrMissingPricesNeverAuthorizeFreeGoods() {
        assertEquals(0,VillageSupplyPolicy.prototype().affordable(Commodity.SAND,10_000,1,0,10,10,10));
        assertThrows(IllegalArgumentException.class,()->VillageSupplyPolicy.prototype().affordable(Commodity.FOOD,-1,1,0,1,1,1));
    }
}
