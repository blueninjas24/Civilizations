package com.blueninjas24.civilizations.population;

/** Prototype chances per pressured household / per receiving settlement each month. */
public record MigrationPolicy(double moveChance, double externalArrivalChance) {
    public MigrationPolicy {
        if (!Double.isFinite(moveChance) || moveChance < 0 || moveChance > 1
                || !Double.isFinite(externalArrivalChance) || externalArrivalChance < 0 || externalArrivalChance > 1) {
            throw new IllegalArgumentException("Migration chances must be finite values in [0, 1]");
        }
    }

    public static MigrationPolicy disabled() { return new MigrationPolicy(0, 0); }
    public static MigrationPolicy prototype() { return new MigrationPolicy(0.02, 0.005); }
    public String replayKey() {
        return "migration-v1:" + Double.toHexString(moveChance) + ":" + Double.toHexString(externalArrivalChance);
    }
}
