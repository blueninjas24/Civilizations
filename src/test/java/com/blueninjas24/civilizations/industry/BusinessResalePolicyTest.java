package com.blueninjas24.civilizations.industry;
import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class BusinessResalePolicyTest {
    @Test void valuesRealCashCargoStockAndSetupWithoutValuingUndiscoveredOre(){
        var p=BusinessResalePolicy.prototype();assertEquals(2000,p.price(2000,300,500,Map.of(Commodity.STONE,2L)));
        assertEquals(2000,p.price(3000,500,0,Map.of()));assertThrows(IllegalArgumentException.class,()->p.price(0,0,0,Map.of(new Commodity("unknown"),1L)));
        assertThrows(ArithmeticException.class,()->p.price(0,Long.MAX_VALUE,0,Map.of()));assertFalse(BusinessResalePolicy.disabled().enabled());
    }
}
