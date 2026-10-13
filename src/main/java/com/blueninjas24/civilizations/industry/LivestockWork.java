package com.blueninjas24.civilizations.industry;

/** Pure next-period livestock work. Caller supplies real feed, labor and unreserved stock space.
 * Only already eligible adults participate; juvenile cohorts/maturation are separate persisted work.
 * Outputs are abstract commodity units, not automatic physical Minecraft item/entity mutations. */
public final class LivestockWork {
    private LivestockWork() {}
    public record Terms(long breedingReserve, long feedPerAdult, long renewableUnitsPerAdult,
                        long meatUnitsPerAnimal, long maximumSlaughterPerPeriod) {
        public Terms {
            if (breedingReserve < 2 || breedingReserve > 1_000_000 || feedPerAdult < 1 || feedPerAdult > 1_000_000
                    || renewableUnitsPerAdult < 1 || renewableUnitsPerAdult > 1_000_000
                    || meatUnitsPerAnimal < 1 || meatUnitsPerAnimal > 1_000_000
                    || maximumSlaughterPerPeriod < 0 || maximumSlaughterPerPeriod > 1_000_000)
                throw new IllegalArgumentException("Invalid livestock work terms");
        }
    }
    public record Plan(long retainedAdults, long fedAdults, long feedUsed, long workUsed,
                       long renewableUnits, long slaughtered, long meatUnits) {}
    /** Feed-backed renewable work precedes requested slaughter. Shortages never authorize slaughter
     * of the protected breeding reserve. No RNG, invented consumer, spoilage or automatic disposal. */
    public static Plan plan(long adults, long feed, long work, long freeSpace, long requestedSlaughter, Terms terms) {
        if (adults < 0 || feed < 0 || work < 0 || freeSpace < 0 || requestedSlaughter < 0)
            throw new IllegalArgumentException("Negative livestock input");
        java.util.Objects.requireNonNull(terms);
        long fed = Math.min(Math.min(adults, feed / terms.feedPerAdult()),
                Math.min(work, freeSpace / terms.renewableUnitsPerAdult()));
        long renewable = Math.multiplyExact(fed, terms.renewableUnitsPerAdult());
        long slaughtered = Math.min(Math.min(requestedSlaughter, terms.maximumSlaughterPerPeriod()),
                Math.min(Math.max(0, adults - terms.breedingReserve()),
                        Math.min(work - fed, (freeSpace - renewable) / terms.meatUnitsPerAnimal())));
        return new Plan(adults - slaughtered, fed, Math.multiplyExact(fed, terms.feedPerAdult()),
                Math.addExact(fed, slaughtered), renewable, slaughtered,
                Math.multiplyExact(slaughtered, terms.meatUnitsPerAnimal()));
    }
}
