package com.blueninjas24.civilizations.economy;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodBartersTest {
    @Test void wholeGoodsFloorToValueAndDisposableStockAndNeverMintRemainders() {
        var goods=new FoodBarters.Goods("OAK_LOG",2,3);
        assertEquals(3,FoodBarters.maximumPayment(2500,400,100,goods,64));
        assertEquals(2,FoodBarters.maximumPayment(2500,400,8,goods,64));
        assertEquals(0,FoodBarters.maximumPayment(2500,400,3,goods,64));
        assertEquals(1,FoodBarters.maximumPayment(2500,400,100,goods,1));
    }
    @Test void unsupportedItemsCurrenciesAndCommodityConversionsFail() {
        assertThrows(IllegalArgumentException.class,()->new FoodBarters.Goods("DIAMOND",1,0));
        assertThrows(IllegalArgumentException.class,()->new FoodBarters.Policy(true,3,64,Map.of(Commodity.FOOD,new FoodBarters.Goods("OAK_LOG",1,0))));
        assertThrows(IllegalArgumentException.class,()->new FoodBarters.Policy(true,3,64,Map.of(Commodity.TIMBER,new FoodBarters.Goods("COBBLESTONE",1,0))));
        assertThrows(IllegalArgumentException.class,()->new FoodBarters.Goods("OAK_LOG",0,0));
    }
    @Test void exactAcceptedTermsAreImmutableAndBounded() {
        var terms=new FoodBarters.Terms(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),8,"WHEAT",8,Commodity.TIMBER,"OAK_LOG",6,2);
        assertEquals(12,terms.paymentUnits());
        assertThrows(IllegalArgumentException.class,()->new FoodBarters.Terms(terms.operation(),terms.player(),terms.settlement(),8,"WHEAT",8,Commodity.TIMBER,"OAK_LOG",65,1));
        assertThrows(UnsupportedOperationException.class,()->FoodBarters.Policy.prototype().goods().clear());
        assertThrows(ArithmeticException.class,()->FoodBarters.maximumPayment(100,Long.MAX_VALUE,10,new FoodBarters.Goods("OAK_LOG",2,0),64));
    }
}
