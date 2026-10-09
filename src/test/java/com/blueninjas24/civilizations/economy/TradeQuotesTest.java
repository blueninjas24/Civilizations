package com.blueninjas24.civilizations.economy;

import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeQuotesTest {
    @Test void unknownBudgetRemainsDifferentFromARealEmptyAccount() {
        var unknown=TradeQuotes.quote(new BigDecimal("2.00"),3,4,OptionalLong.empty());
        assertEquals(TradeQuotes.Coverage.UNKNOWN_BUDGET,unknown.coverage());assertTrue(unknown.affordableItems().isEmpty());
        var zero=TradeQuotes.quote(new BigDecimal("2.00"),3,4,OptionalLong.of(0));
        assertEquals(TradeQuotes.Coverage.NO_AFFORDABLE_ITEMS,zero.coverage());assertEquals(0,zero.affordableItems().orElseThrow());
        assertEquals(0,zero.affordableCostHundredths().orElseThrow());
    }
    @Test void exactBudgetCoversWholeItemsAndPartialBudgetNeverRoundsUp() {
        var full=TradeQuotes.quote(new BigDecimal("2.00"),3,4,OptionalLong.of(2400));
        assertEquals(600,full.itemPriceHundredths());assertEquals(2400,full.offeredCostHundredths());assertEquals(TradeQuotes.Coverage.FULL,full.coverage());
        var partial=TradeQuotes.quote(new BigDecimal("2.00"),3,4,OptionalLong.of(2399));
        assertEquals(3,partial.affordableItems().orElseThrow());assertEquals(1800,partial.affordableCostHundredths().orElseThrow());assertEquals(TradeQuotes.Coverage.PARTIAL,partial.coverage());
        assertEquals(0,TradeQuotes.quote(new BigDecimal("2.00"),3,4,OptionalLong.of(599)).affordableItems().orElseThrow());
    }
    @Test void priceIsExactAndDoesNotRoundAgainPerItemOrStack() {
        var quote=TradeQuotes.quote(new BigDecimal("0.01"),3,64,OptionalLong.of(192));
        assertEquals(3,quote.itemPriceHundredths());assertEquals(192,quote.offeredCostHundredths());
        assertEquals(64,quote.affordableItems().orElseThrow());assertTrue(quote.explanation().contains("no currency payout"));
        assertThrows(IllegalArgumentException.class,()->TradeQuotes.quote(new BigDecimal("0.005"),3,4,OptionalLong.of(100)));
    }
    @Test void invalidInputAndOverflowCannotCreateAnOffer() {
        for(var price:List.of(BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("999999999999999999999")))
            assertThrows(IllegalArgumentException.class,()->TradeQuotes.quote(price,1,1,OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class,()->TradeQuotes.quote(BigDecimal.ONE,0,1,OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class,()->TradeQuotes.quote(BigDecimal.ONE,1,65,OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class,()->TradeQuotes.quote(BigDecimal.ONE,1,0,OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class,()->TradeQuotes.quote(BigDecimal.ONE,1,1,OptionalLong.of(-1)));
        assertThrows(ArithmeticException.class,()->TradeQuotes.quote(BigDecimal.ONE,Long.MAX_VALUE,1,OptionalLong.empty()));
        assertThrows(ArithmeticException.class,()->new TradeQuotes.Quote(Long.MAX_VALUE/2,1,3,null));
    }
    @Test void longBoundaryIsAffordableWithoutFloatingPointDrift() {
        var quote=new TradeQuotes.Quote(Long.MAX_VALUE,1,1,Long.MAX_VALUE);
        assertEquals(1,quote.affordableItems().orElseThrow());assertEquals(Long.MAX_VALUE,quote.offeredCostHundredths());
        assertEquals(0,new TradeQuotes.Quote(Long.MAX_VALUE,1,1,Long.MAX_VALUE-1).affordableItems().orElseThrow());
    }
    @Test void seededBudgetsNeverOverspendAndBuyTheMaximumAffordableWholeQuantity() {
        var random=new Random(814);
        for(int iteration=0;iteration<10000;iteration++) {
            long price=1+random.nextInt(100000),units=1+random.nextInt(1000),budget=random.nextLong(1000000000);int items=1+random.nextInt(64);
            var quote=new TradeQuotes.Quote(price,units,items,budget);int accepted=quote.affordableItems().orElseThrow();long cost=quote.affordableCostHundredths().orElseThrow();
            assertTrue(accepted>=0&&accepted<=items);assertTrue(cost<=budget);
            if(accepted<items)assertTrue(Math.addExact(cost,quote.itemPriceHundredths())>budget);
        }
    }
    @Test void existingScarcityPricesAndFoodUnitsFeedQuotesWithoutASecondRoundingRule() {
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for(var band:CommodityAccounting.Condition.values())bands.put(band,85);
        var settings=new EconomySettings(60,120,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,new BigDecimal("3.33")),bands);
        var price=settings.price(Commodity.FOOD,CommodityAccounting.Condition.SURPLUS);
        assertEquals(new BigDecimal("2.83"),price);
        long breadUnits=FoodAidPreview.Policy.prototype().units("BREAD",1);
        var quote=TradeQuotes.quote(price,breadUnits,4,OptionalLong.of(3396));
        assertEquals(849,quote.itemPriceHundredths());assertEquals(3396,quote.offeredCostHundredths());assertEquals(TradeQuotes.Coverage.FULL,quote.coverage());
    }

}
