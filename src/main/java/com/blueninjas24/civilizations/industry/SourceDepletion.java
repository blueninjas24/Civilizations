package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Equal rounds across positive sources, with deterministic remainder order. No per-unit loop. */
public final class SourceDepletion {
    private SourceDepletion() {}
    public static List<Long> drain(List<Long> balances,long amount) {
        if(amount<0||balances.size()>65536)throw new IllegalArgumentException("Invalid source depletion budget");
        long total=0,high=0;
        for(long balance:balances){if(balance<0)throw new IllegalArgumentException("Negative source");total=Math.addExact(total,balance);high=Math.max(high,balance);}
        if(amount>total)throw new IllegalArgumentException("Extraction exceeds source shares");
        long low=0;
        while(low<high) {
            long mid=low+(high-low)/2+(high-low)%2,cost=0;
            for(long balance:balances)cost=Math.addExact(cost,Math.min(balance,mid));
            if(cost<=amount)low=mid;else high=mid-1;
        }
        var result=new ArrayList<Long>(balances.size());long spent=0;
        for(long balance:balances){long reduction=Math.min(balance,low);spent=Math.addExact(spent,reduction);result.add(balance-reduction);}
        long extra=amount-spent;
        for(int i=0;i<result.size()&&extra>0;i++)if(result.get(i)>0){result.set(i,result.get(i)-1);extra--;}
        if(extra!=0)throw new IllegalStateException("Incomplete source depletion");
        return List.copyOf(result);
    }
}
