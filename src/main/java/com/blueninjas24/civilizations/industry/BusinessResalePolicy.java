package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.math.BigInteger;
import java.util.*;

/** Editable sale terms and computational budgets, with no private ownership cap. */
public record BusinessResalePolicy(boolean enabled,long minimumSiteHundredths,int startupRecoveryPercent,
                                   long deadlineMillis,int buyersPerReview,Map<Commodity,Long> stockValues) {
    public BusinessResalePolicy {
        stockValues=Collections.unmodifiableMap(new TreeMap<>(stockValues));
        if(minimumSiteHundredths<1||minimumSiteHundredths>1_000_000_000L||startupRecoveryPercent<0||startupRecoveryPercent>100
                ||deadlineMillis<1000||deadlineMillis>86400000||buyersPerReview<1||buyersPerReview>128||stockValues.size()>32
                ||stockValues.values().stream().anyMatch(v->v<1||v>1_000_000_000L))throw new IllegalArgumentException("Invalid autonomous resale settings");
    }
    public long price(long startup,long cash,long ownedCargo,Map<Commodity,Long> stock) {
        if(startup<0||cash<0||ownedCargo<0)throw new IllegalArgumentException("Negative sale asset value");
        long setup=Math.max(minimumSiteHundredths,BigInteger.valueOf(startup).multiply(BigInteger.valueOf(startupRecoveryPercent)).divide(BigInteger.valueOf(100)).longValueExact());
        long price=Math.addExact(setup,Math.addExact(cash,ownedCargo));
        for(var e:stock.entrySet()) {
            if(e.getValue()<0)throw new IllegalArgumentException("Negative sale stock");
            if(e.getValue()==0)continue;
            var value=stockValues.get(e.getKey());if(value==null)throw new IllegalArgumentException("No resale value configured for "+e.getKey().id());
            price=Math.addExact(price,Math.multiplyExact(value,e.getValue()));
        }
        return price;
    }
    public static BusinessResalePolicy prototype(){return new BusinessResalePolicy(true,1000,50,600000,32,
            Map.of(Commodity.FOOD,200L,Commodity.TIMBER,150L,Commodity.STONE,100L,Commodity.IRON,500L,Commodity.COAL,200L,
                   Commodity.LIVESTOCK_PRODUCTS,300L,new Commodity("tools"),800L,new Commodity("storage_fittings"),2000L));}
    public static BusinessResalePolicy disabled(){return new BusinessResalePolicy(false,1000,50,600000,32,Map.of());}
}
