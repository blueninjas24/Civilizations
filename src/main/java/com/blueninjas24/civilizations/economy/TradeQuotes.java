package com.blueninjas24.civilizations.economy;

import java.math.*;
import java.util.*;

/** Exact abstract Trade Value quotes; no currency conversion, funds reservation or inventory mutation. */
public final class TradeQuotes {
    private TradeQuotes() {}
    public enum Coverage { UNKNOWN_BUDGET, NO_AFFORDABLE_ITEMS, PARTIAL, FULL }
    public record Quote(long pricePerUnitHundredths, long unitsPerItem, int offeredItems, Long budgetHundredths) {
        public Quote {
            if(pricePerUnitHundredths<1 || unitsPerItem<1 || offeredItems<1 || offeredItems>64
                    || budgetHundredths!=null && budgetHundredths<0) throw new IllegalArgumentException("Invalid trade quote");
            // Refuse unrepresentable offers before callers can create a transaction or misleading display.
            Math.multiplyExact(Math.multiplyExact(pricePerUnitHundredths,unitsPerItem),offeredItems);
        }
        public long itemPriceHundredths() { return Math.multiplyExact(pricePerUnitHundredths,unitsPerItem); }
        public long offeredCostHundredths() { return Math.multiplyExact(itemPriceHundredths(),offeredItems); }
        public OptionalInt affordableItems() {
            return budgetHundredths==null ? OptionalInt.empty() : OptionalInt.of((int)Math.min(offeredItems,budgetHundredths/itemPriceHundredths()));
        }
        public OptionalLong affordableCostHundredths() {
            var items=affordableItems();return items.isEmpty()?OptionalLong.empty():OptionalLong.of(Math.multiplyExact(itemPriceHundredths(),items.getAsInt()));
        }
        public Coverage coverage() {
            var items=affordableItems();return items.isEmpty()?Coverage.UNKNOWN_BUDGET:items.getAsInt()==0?Coverage.NO_AFFORDABLE_ITEMS:items.getAsInt()==offeredItems?Coverage.FULL:Coverage.PARTIAL;
        }
        public String explanation() {
            return "Indicative quote: " + offeredItems + " items, " + unitsPerItem + " commodity units each; "
                    + BigDecimal.valueOf(itemPriceHundredths(),2).toPlainString() + " Trade Value/item; total "
                    + BigDecimal.valueOf(offeredCostHundredths(),2).toPlainString() + ". "
                    + (budgetHundredths==null?"Village budget unknown.":"Village can currently afford " + affordableItems().orElseThrow() + " whole items.")
                    + " Calculation only; no currency payout, goods accepted or funds reserved.";
        }
    }
    /** Existing scarcity pricing rounds once per commodity unit; this method does not round again per item or stack. */
    public static Quote quote(BigDecimal pricePerUnit,long unitsPerItem,int items,OptionalLong budget) {
        Objects.requireNonNull(pricePerUnit);Objects.requireNonNull(budget);
        final long hundredths;
        try { hundredths=pricePerUnit.setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
        catch(ArithmeticException invalid) { throw new IllegalArgumentException("Quote price needs exact representable hundredths",invalid); }
        return new Quote(hundredths,unitsPerItem,items,budget.isPresent()?budget.getAsLong():null);
    }
}
