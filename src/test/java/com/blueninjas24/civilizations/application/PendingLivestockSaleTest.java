package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PendingLivestockSaleTest {
    Terms terms(Product product, int count, int emeralds, long rate) {
        return new Terms(new UUID(1, 1), new UUID(2, 2), new UUID(3, 3), new UUID(4, 4), new UUID(5, 5), product, count, emeralds, rate);
    }
    @Test void everyStageAndProductRoundTripsExactRecoveryTerms() {
        for (var product : Product.values()) for (var stage : PendingLivestockSale.Stage.values()) {
            var receipt = new PendingLivestockSale(terms(product, 1, 4, 800), stage);
            assertEquals(receipt, PendingLivestockSale.decode(receipt.encode()));
            assertEquals(PendingLivestockSale.Stage.REVIEW, receipt.withStage(PendingLivestockSale.Stage.REVIEW).stage());
            assertEquals(receipt.terms(), receipt.withStage(PendingLivestockSale.Stage.REVIEW).terms());
        }
    }
    @Test void milkAlwaysNeedsOnePlayerBucketAndCannotStackWhileEggsNeedNone() {
        var milk = terms(Product.MILK, 1, 3, 800); assertEquals(1, milk.emptyBuckets()); assertEquals("MILK_BUCKET", milk.product().item());
        assertEquals(2400, milk.cost()); assertEquals(0, terms(Product.EGGS, 64, 3, 800).emptyBuckets());
        assertThrows(IllegalArgumentException.class, () -> terms(Product.MILK, 2, 3, 800));
        assertThrows(IllegalArgumentException.class, () -> terms(Product.EGGS, 65, 3, 800));
    }
    @Test void malformedNoncanonicalOversizedAndUnsupportedRecoveryRecordsAreRefused() {
        String good = new PendingLivestockSale(terms(Product.MILK, 1, 3, 800), PendingLivestockSale.Stage.PREPARED).encode();
        for (var bad : List.of("", "x".repeat(401), good + "|", good.replace("lps1|", "lps2|"), good.replace("PREPARED", "PAID"),
                good.replace("MILK", "DIAMONDS"), good.replace("|1|3|800", "|01|3|800"), good.replace("|1|3|800", "|2|3|800"),
                good.replace("|1|3|800", "|1|0|800"), good.replace("|1|3|800", "|1|3|0"), good.replace("00000000-0000-0001", "0-0-1"))) {
            assertThrows(IllegalArgumentException.class, () -> PendingLivestockSale.decode(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> PendingLivestockSale.decode(null));
    }
    @Test void invalidTermsCannotRepresentFreeNegativeOverflowingOrUnboundedPayment() {
        for (int count : new int[] { -1, 0, 65 }) assertThrows(IllegalArgumentException.class, () -> terms(Product.EGGS, count, 1, 800));
        for (int price : new int[] { -1, 0, 65 }) assertThrows(IllegalArgumentException.class, () -> terms(Product.MILK, 1, price, 800));
        for (long rate : new long[] { -1, 0, 100_000_001, Long.MAX_VALUE }) assertThrows(IllegalArgumentException.class, () -> terms(Product.MILK, 1, 1, rate));
        assertEquals(6_400_000_000L, terms(Product.MILK, 1, 64, 100_000_000).cost());
        assertThrows(NullPointerException.class, () -> terms(null, 1, 1, 800));
    }
}
