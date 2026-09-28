package com.blueninjas24.civilizations.settlement;

import java.util.UUID;

public class Settlement {

    private final UUID id;
    private final String name;
    private final String world;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final long discoveredAt;

    public Settlement(
            UUID id,
            String name,
            String world,
            int centerX,
            int centerY,
            int centerZ,
            long discoveredAt
    ) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.discoveredAt = discoveredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public long getDiscoveredAt() {
        return discoveredAt;
    }
}