package com.blueninjas24.civilizations.industry;

import java.util.Objects;

/** Editable prototype birth/care terms. Parsing does not adopt animals or start businesses. */
public record LivestockPolicy(boolean enabled,int maturityPeriods,int breedingDelayPeriods,
                              long feedPerBirth,long workPerBirth,long maximumBirthsPerPeriod,
                              LivestockWork.Terms cow,LivestockWork.Terms chicken,LivestockWork.Terms sheep) {
    public LivestockPolicy {
        Objects.requireNonNull(cow);Objects.requireNonNull(chicken);Objects.requireNonNull(sheep);
        if(maturityPeriods<1||maturityPeriods>1000||breedingDelayPeriods<1||breedingDelayPeriods>1000
                ||feedPerBirth<1||feedPerBirth>1_000_000||workPerBirth<1||workPerBirth>1_000_000
                ||maximumBirthsPerPeriod<0||maximumBirthsPerPeriod>1000)throw new IllegalArgumentException("Invalid livestock birth terms");
    }
    /** Preserve existing source callers and the historical cow/chicken operation fingerprint. */
    public LivestockPolicy(boolean enabled,int maturityPeriods,int breedingDelayPeriods,long feedPerBirth,long workPerBirth,long maximumBirthsPerPeriod,LivestockWork.Terms cow,LivestockWork.Terms chicken) {
        this(enabled,maturityPeriods,breedingDelayPeriods,feedPerBirth,workPerBirth,maximumBirthsPerPeriod,cow,chicken,sheepDefaults());
    }
    public static LivestockWork.Terms sheepDefaults(){return new LivestockWork.Terms(2,1,1,3,1);}
    public LivestockWork.Terms terms(LivestockHerd.Species species){return switch(species){case COW->cow;case CHICKEN->chicken;case SHEEP->sheep;};}
    @Override public String toString(){
        String legacy="LivestockPolicy[enabled="+enabled+", maturityPeriods="+maturityPeriods+", breedingDelayPeriods="+breedingDelayPeriods+", feedPerBirth="+feedPerBirth+", workPerBirth="+workPerBirth+", maximumBirthsPerPeriod="+maximumBirthsPerPeriod+", cow="+cow+", chicken="+chicken+"]";
        return sheep.equals(sheepDefaults())?legacy:legacy+"|sheep="+sheep;
    }
    public static LivestockPolicy prototype(){return new LivestockPolicy(true,12,6,4,2,1,
            new LivestockWork.Terms(2,1,1,4,1),new LivestockWork.Terms(2,1,1,2,1));}
    public static LivestockPolicy disabled(){var p=prototype();return new LivestockPolicy(false,p.maturityPeriods,p.breedingDelayPeriods,p.feedPerBirth,p.workPerBirth,p.maximumBirthsPerPeriod,p.cow,p.chicken,p.sheep);}
}
