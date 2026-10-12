package com.blueninjas24.civilizations.economy;

import java.util.Objects;

/** Stable commodity keys, independent of Minecraft items and prices. Units remain caller-defined. */
public record Commodity(String id) implements Comparable<Commodity> {
    public static final Commodity FOOD = new Commodity("food");
    public static final Commodity TIMBER = new Commodity("timber");
    public static final Commodity STONE = new Commodity("stone");
    public static final Commodity SAND = new Commodity("sand");
    public static final Commodity IRON = new Commodity("iron");
    public static final Commodity COAL = new Commodity("coal");
    public static final Commodity LIVESTOCK_PRODUCTS = new Commodity("livestock_products");

    public Commodity {
        Objects.requireNonNull(id);
        if (!id.matches("[a-z][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid commodity key");
    }
    @Override public int compareTo(Commodity other) { return id.compareTo(other.id); }
}
