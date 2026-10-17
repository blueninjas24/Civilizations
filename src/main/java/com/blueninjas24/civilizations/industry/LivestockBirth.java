package com.blueninjas24.civilizations.industry;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Permanent logical identity for one simulated newborn, independent of a later Paper entity UUID. */
public record LivestockBirth(UUID id,UUID business,long birthPeriod,int ordinal,long maturePeriod) {
    public LivestockBirth {
        Objects.requireNonNull(id);Objects.requireNonNull(business);
        if(birthPeriod<1||ordinal<0||ordinal>=1000||maturePeriod<=birthPeriod||maturePeriod-birthPeriod>1000
                ||!id.equals(identity(business,birthPeriod,ordinal)))throw new IllegalArgumentException("Invalid permanent livestock birth identity");
    }
    private static UUID identity(UUID business,long period,int ordinal) {
        return UUID.nameUUIDFromBytes(("civilizations:livestock-birth-v1:"+business+":"+period+":"+ordinal).getBytes(StandardCharsets.UTF_8));
    }
    public static List<LivestockBirth> batch(UUID business,long period,int count,int maturityPeriods) {
        Objects.requireNonNull(business);
        if(period<1||count<0||count>1000||maturityPeriods<1||maturityPeriods>1000)throw new IllegalArgumentException("Invalid livestock birth batch");
        if(count==0)return List.of();
        long mature=Math.addExact(period,maturityPeriods);var result=new ArrayList<LivestockBirth>(count);
        for(int i=0;i<count;i++)result.add(new LivestockBirth(identity(business,period,i),business,period,i,mature));
        return List.copyOf(result);
    }
}
