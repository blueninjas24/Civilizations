package com.blueninjas24.civilizations.industry;

import java.util.Objects;

/** Editable prototype birth/care terms. Parsing does not adopt animals or start businesses. */
public record LivestockPolicy(boolean enabled,int maturityPeriods,int breedingDelayPeriods,
                              long feedPerBirth,long workPerBirth,long maximumBirthsPerPeriod,
                              LivestockWork.Terms cow,LivestockWork.Terms chicken) {
    public LivestockPolicy {
        Objects.requireNonNull(cow);Objects.requireNonNull(chicken);
        if(maturityPeriods<1||maturityPeriods>1000||breedingDelayPeriods<1||breedingDelayPeriods>1000
                ||feedPerBirth<1||feedPerBirth>1_000_000||workPerBirth<1||workPerBirth>1_000_000
                ||maximumBirthsPerPeriod<0||maximumBirthsPerPeriod>1000)throw new IllegalArgumentException("Invalid livestock birth terms");
    }
    public static LivestockPolicy prototype(){return new LivestockPolicy(true,12,6,4,2,1,
            new LivestockWork.Terms(2,1,1,4,1),new LivestockWork.Terms(2,1,1,2,1));}
    public static LivestockPolicy disabled(){var p=prototype();return new LivestockPolicy(false,p.maturityPeriods,p.breedingDelayPeriods,p.feedPerBirth,p.workPerBirth,p.maximumBirthsPerPeriod,p.cow,p.chicken);}
}
