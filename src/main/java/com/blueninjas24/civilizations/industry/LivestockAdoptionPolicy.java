package com.blueninjas24.civilizations.industry;

/** Editable conservative local recognition and paid founding; no implicit rights over large pastures. */
public record LivestockAdoptionPolicy(boolean enabled,int villageRadius,int clearRadius,int minimumAdults,int maximumAnimals,
        long observationMillis,int intervalTicks,int chunksPerTick,int cellsPerTick,long startupHundredths,long capitalHundredths,
        int herdCapacity,long founderFarming,long workerFarming,int maximumPerSpecies) {
    public LivestockAdoptionPolicy {
        if(villageRadius<8||villageRadius>128||clearRadius<2||clearRadius>16||minimumAdults<2||maximumAnimals<minimumAdults||maximumAnimals>32
                ||observationMillis<1000||observationMillis>3_600_000||intervalTicks<20||intervalTicks>1200||chunksPerTick<1||chunksPerTick>4||cellsPerTick<1||cellsPerTick>4096
                ||startupHundredths<0||startupHundredths>1_000_000||capitalHundredths<1||capitalHundredths>1_000_000
                ||herdCapacity<maximumAnimals||herdCapacity>256||founderFarming<0||founderFarming>1000||workerFarming<0||workerFarming>1000||maximumPerSpecies<1||maximumPerSpecies>16)
            throw new IllegalArgumentException("Invalid livestock adoption/founding limits");
    }
    public static LivestockAdoptionPolicy prototype(){return new LivestockAdoptionPolicy(true,64,8,2,8,30000,100,1,512,500,2000,16,1,0,1);}
    public static LivestockAdoptionPolicy disabled(){var p=prototype();return new LivestockAdoptionPolicy(false,p.villageRadius,p.clearRadius,p.minimumAdults,p.maximumAnimals,p.observationMillis,p.intervalTicks,p.chunksPerTick,p.cellsPerTick,p.startupHundredths,p.capitalHundredths,p.herdCapacity,p.founderFarming,p.workerFarming,p.maximumPerSpecies);}
}
