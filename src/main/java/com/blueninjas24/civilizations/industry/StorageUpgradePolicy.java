package com.blueninjas24.civilizations.industry;

import java.math.BigInteger;
import java.util.*;

/** Limited fittings within an existing workplace, not permission for physical expansion. */
public record StorageUpgradePolicy(boolean enabled,int occupancyPercent,long capacityPerUpgrade,int maximumUpgrades,
        int maximumGrowthPercent,long maximumCapacity,long minimumCash,long unitsPerUpgrade,long unitPrice,
        int deliveryPeriods,int sellersPerReview,Set<String> industries) {
    public StorageUpgradePolicy {
        industries=Collections.unmodifiableSet(new TreeSet<>(industries));
        if(occupancyPercent<1||occupancyPercent>100||capacityPerUpgrade<1||capacityPerUpgrade>1_000_000
                ||maximumUpgrades<1||maximumUpgrades>32||maximumGrowthPercent<1||maximumGrowthPercent>400
                ||maximumCapacity<1||maximumCapacity>1_000_000_000L||minimumCash<0||minimumCash>100_000_000_000L
                ||unitsPerUpgrade<1||unitsPerUpgrade>64||unitPrice<1||unitPrice>100_000_000L
                ||deliveryPeriods<1||deliveryPeriods>100||sellersPerReview<1||sellersPerReview>64
                ||industries.size()>32||industries.stream().anyMatch(k->!k.matches("[a-z][a-z0-9_]{0,63}")||Set.of("farming","livestock").contains(k)))
            throw new IllegalArgumentException("Invalid storage upgrade limits; farm/herd adapters are separate");
    }
    public long invoice(){return Math.multiplyExact(unitsPerUpgrade,unitPrice);}
    public long nextCapacity(long base,long current,int previous,long occupied,long cash) {
        if(base<1||current<base||previous<0||occupied<0||occupied>current||cash<0)throw new IllegalArgumentException("Invalid storage evidence");
        if(!enabled||previous>=maximumUpgrades||cash<invoice()||cash-invoice()<minimumCash
                ||BigInteger.valueOf(occupied).multiply(BigInteger.valueOf(100)).compareTo(BigInteger.valueOf(current).multiply(BigInteger.valueOf(occupancyPercent)))<0)return current;
        long limit=BigInteger.valueOf(base).multiply(BigInteger.valueOf(100L+maximumGrowthPercent)).divide(BigInteger.valueOf(100))
                .min(BigInteger.valueOf(maximumCapacity)).longValueExact();
        if(limit<=current)return current;
        return current+Math.min(capacityPerUpgrade,limit-current);
    }
    public static StorageUpgradePolicy prototype(){return new StorageUpgradePolicy(true,80,256,4,100,8192,2500,1,2000,1,8,
            Set.of("mining","blacksmith","warehouse","quarry","sandpit","fishing","bakery","craftsman"));}
    public static StorageUpgradePolicy disabled(){var p=prototype();return new StorageUpgradePolicy(false,p.occupancyPercent,p.capacityPerUpgrade,p.maximumUpgrades,p.maximumGrowthPercent,p.maximumCapacity,p.minimumCash,p.unitsPerUpgrade,p.unitPrice,p.deliveryPeriods,p.sellersPerReview,p.industries);}
}
