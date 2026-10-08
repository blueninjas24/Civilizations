package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EconomySettingsTest {
    private EconomySettings settings(int output, long price) {
        var bands = new EnumMap<Condition, Integer>(Condition.class);
        for (var band : Condition.values()) bands.put(band, 100);
        return new EconomySettings(60, 120, 2, 2, 1, 4, output, new Policy(5000, 12500, 20000, 3),
                Map.of(Commodity.FOOD, 12L), Map.of(Commodity.FOOD, BigDecimal.valueOf(price)), bands);
    }
    @Test void outputCannotExceedEitherAvailableWorkersOrEvidencedFacilitySlots() {
        var settings = settings(100, 2);
        assertEquals(0, settings.foodProduction(10, OptionalLong.empty())); assertEquals(0, settings.foodProduction(10, OptionalLong.of(0)));
        assertEquals(0, settings.foodProduction(0, OptionalLong.of(10))); assertEquals(8, settings.foodProduction(10, OptionalLong.of(2)));
        assertEquals(8, settings.foodProduction(2, OptionalLong.of(10))); assertEquals(0, settings(0, 2).foodProduction(10, OptionalLong.of(10)));
    }
    @Test void negativesAndOverflowDoNotInventGoodsAndScalingHandlesWideIntermediateProduct() {
        var settings = settings(100, 2);
        assertThrows(IllegalArgumentException.class, () -> settings.foodDemand(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> settings.foodProduction(1, OptionalLong.of(-1)));
        assertThrows(ArithmeticException.class, () -> settings.foodDemand(Long.MAX_VALUE, 0));
        assertThrows(ArithmeticException.class, () -> settings.startingReserves(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE / 25, settings(1, 2).foodProduction(Long.MAX_VALUE, OptionalLong.of(Long.MAX_VALUE)));
    }
    @Test void snapshotsAndDerivedInitialReservesCannotBeMutated() {
        var settings = settings(100, 2);
        assertThrows(UnsupportedOperationException.class, () -> settings.startingPerResident().clear());
        assertThrows(UnsupportedOperationException.class, () -> settings.baseTradeValues().clear());
        assertThrows(UnsupportedOperationException.class, () -> settings.pricePercent().clear());
        assertThrows(UnsupportedOperationException.class, () -> settings.startingReserves(1).clear());
        assertEquals(0, settings.startingReserves(0).get(Commodity.FOOD));
    }
    @Test void quoteRequiresKnownCommodityAndIsIndependentOfEmeraldsAndInventory() {
        var settings = settings(100, 2);
        assertEquals(new BigDecimal("2.00"), settings.price(Commodity.FOOD, Condition.BALANCED));
        assertThrows(IllegalArgumentException.class, () -> settings.price(Commodity.IRON, Condition.BALANCED));
    }
}
