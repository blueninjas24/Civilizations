package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.economy.FoodPurchases.Terms;
import java.util.Objects;
import java.util.UUID;

/** Immutable inventory recovery state, serialized with the same Minecraft player inventory snapshot. */
public record PendingFoodPurchase(Terms terms, Stage stage) {
    public static final String KEY = "civilizations:pending_food_purchase";
    public enum Stage { PREPARED, EXCHANGED, CANCELLED, REVIEW }
    public PendingFoodPurchase { Objects.requireNonNull(terms); Objects.requireNonNull(stage); }
    public PendingFoodPurchase withStage(Stage next) { return new PendingFoodPurchase(terms, next); }
    public String encode() {
        return "buy1|" + stage + "|" + terms.operation() + "|" + terms.player() + "|" + terms.settlement() + "|" + terms.units()
                + "|" + terms.item() + "|" + terms.count() + "|" + terms.emeralds() + "|" + terms.hundredthsPerEmerald();
    }
    public static PendingFoodPurchase decode(String encoded) {
        if (encoded == null || encoded.length() > 260) throw new IllegalArgumentException("Invalid purchase receipt length");
        var f = encoded.split("\\|", -1);
        if (f.length != 10 || !f[0].equals("buy1")) throw new IllegalArgumentException("Unsupported purchase receipt format");
        var result = new PendingFoodPurchase(new Terms(UUID.fromString(f[2]), UUID.fromString(f[3]), UUID.fromString(f[4]),
                Long.parseLong(f[5]), f[6], Integer.parseInt(f[7]), Integer.parseInt(f[8]), Long.parseLong(f[9])), Stage.valueOf(f[1]));
        if (!result.encode().equals(encoded)) throw new IllegalArgumentException("Noncanonical purchase receipt");
        return result;
    }
}
