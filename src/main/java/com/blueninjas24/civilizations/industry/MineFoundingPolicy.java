package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Editable prototype startup checks, not permission to excavate or construct. */
public record MineFoundingPolicy(boolean enabled,int settlementsPerPulse,int patchesPerReview,int foundersPerReview,
        long surveyLifetimeMillis,int horizontalRange,int verticalRange,long minimumAgeSteps,long founderExperience,
        int workerCapacity,long startupHundredths,long capitalHundredths,Map<Commodity,Long> mineralYield) {
    public MineFoundingPolicy {
        mineralYield=Map.copyOf(mineralYield);
        if(settlementsPerPulse<1||settlementsPerPulse>16||patchesPerReview<1||patchesPerReview>64||foundersPerReview<1||foundersPerReview>512
                ||surveyLifetimeMillis<1000||surveyLifetimeMillis>3_600_000||horizontalRange<1||horizontalRange>256
                ||verticalRange<1||verticalRange>128||minimumAgeSteps<1||minimumAgeSteps>1_000_000
                ||founderExperience<1||founderExperience>1_000_000_000||workerCapacity<1||workerCapacity>32
                ||startupHundredths<0||capitalHundredths<0||startupHundredths>1_000_000_000L||capitalHundredths>1_000_000_000L
                ||startupHundredths+capitalHundredths==0||mineralYield.isEmpty()||mineralYield.size()>2
                ||mineralYield.entrySet().stream().anyMatch(e->!Set.of(Commodity.COAL,Commodity.IRON).contains(e.getKey())||e.getValue()<1||e.getValue()>1_000_000))
            throw new IllegalArgumentException("Invalid automatic mine founding settings");
    }
    public static MineFoundingPolicy prototype(){return new MineFoundingPolicy(true,2,8,64,60000,96,48,216,10,2,500,2000,Map.of(Commodity.COAL,2L,Commodity.IRON,1L));}
    public static MineFoundingPolicy disabled(){var p=prototype();return new MineFoundingPolicy(false,p.settlementsPerPulse,p.patchesPerReview,p.foundersPerReview,p.surveyLifetimeMillis,p.horizontalRange,p.verticalRange,p.minimumAgeSteps,p.founderExperience,p.workerCapacity,p.startupHundredths,p.capitalHundredths,p.mineralYield);}
    public Map<Commodity,Long> bundle(Commodity mineral,IndustryPolicy industry) {
        if(!mineralYield.containsKey(mineral))throw new IllegalArgumentException("Mineral outside automatic founding policy");
        var result=new TreeMap<Commodity,Long>();industry.miningBundle().forEach((good,units)->{if(!Set.of(Commodity.COAL,Commodity.IRON).contains(good))result.put(good,units);});
        result.put(mineral,mineralYield.get(mineral));return Map.copyOf(result);
    }
}
