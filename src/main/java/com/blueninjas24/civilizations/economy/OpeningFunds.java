package com.blueninjas24.civilizations.economy;

/** Editable initial conditions, never recurring income or personal wealth. */
public record OpeningFunds(boolean enabled, long hundredthsPerResident) {
    public OpeningFunds {
        if (hundredthsPerResident < 0 || hundredthsPerResident > 100_000_000)
            throw new IllegalArgumentException("finance.opening-trade-value-per-resident must be between 0 and 1000000");
    }
    public static OpeningFunds prototype() { return new OpeningFunds(true, 2400); }
    public static OpeningFunds disabled() { return new OpeningFunds(false, 2400); }
    public long amount(long livingResidents) {
        if (livingResidents < 0) throw new IllegalArgumentException("Negative living population");
        return Math.multiplyExact(livingResidents, hundredthsPerResident);
    }
}
