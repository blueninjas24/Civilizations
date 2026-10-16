package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.util.Objects;
import java.util.UUID;

/** Future inventory adapter must save this with its player inventory and verify disk proof before SQL effects. */
public record PendingLivestockSale(Terms terms, Stage stage) {
    public static final String KEY = "civilizations:pending_livestock_sale";
    public enum Stage { PREPARED, EXCHANGED, CANCELLED, REVIEW }
    public PendingLivestockSale { Objects.requireNonNull(terms); Objects.requireNonNull(stage); }
    public PendingLivestockSale withStage(Stage next) { return new PendingLivestockSale(terms, next); }
    public String encode() {
        return "lps1|" + stage + "|" + terms.operation() + "|" + terms.player() + "|" + terms.settlement()
                + "|" + terms.world() + "|" + terms.seller() + "|" + terms.product() + "|" + terms.count()
                + "|" + terms.emeralds() + "|" + terms.hundredthsPerEmerald();
    }
    public static PendingLivestockSale decode(String encoded) {
        if (encoded == null || encoded.length() > 400) throw new IllegalArgumentException("Invalid livestock receipt length");
        var f = encoded.split("\\|", -1);
        if (f.length != 11 || !f[0].equals("lps1")) throw new IllegalArgumentException("Unsupported livestock receipt format");
        var result = new PendingLivestockSale(new Terms(UUID.fromString(f[2]), UUID.fromString(f[3]), UUID.fromString(f[4]),
                UUID.fromString(f[5]), UUID.fromString(f[6]), Product.valueOf(f[7]), Integer.parseInt(f[8]),
                Integer.parseInt(f[9]), Long.parseLong(f[10])), Stage.valueOf(f[1]));
        if (!result.encode().equals(encoded)) throw new IllegalArgumentException("Noncanonical livestock receipt");
        return result;
    }
}
