package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Only buy existing producer goods against uncovered customer needs. No speculative reserves. */
public final class WarehouseInputPlan {
    private WarehouseInputPlan() {}
    public static List<WorkshopInputPlan.Order> choose(Map<Commodity,Long> demand,Map<Commodity,Long> pending,
            long funds,long freeSpace,long haul,int slots,Map<Commodity,Long> sizes,
            List<WorkshopInputPlan.Warehouse> producers,Map<Commodity,Long> prices) {
        if(funds<0 || freeSpace<0 || haul<0 || slots<0 || slots>128
                || demand.values().stream().anyMatch(n->n<0) || pending.values().stream().anyMatch(n->n<0)
                || sizes.values().stream().anyMatch(n->n<1) || prices.values().stream().anyMatch(n->n<1)
                || producers.size()>64 || producers.stream().map(WorkshopInputPlan.Warehouse::id).distinct().count()!=producers.size())
            throw new IllegalArgumentException("Invalid bounded warehouse purchasing resources");
        var result=new ArrayList<WorkshopInputPlan.Order>();
        var sources=producers.stream().sorted(Comparator.comparing(p->p.id().toString())).toList();
        for(var e:new TreeMap<>(demand).entrySet()) {
            Long size=sizes.get(e.getKey()),price=prices.get(e.getKey());
            if(size==null || price==null)continue;
            long missing=Math.max(0,e.getValue()-pending.getOrDefault(e.getKey(),0L));
            for(var source:sources) {
                if(result.size()==slots)return List.copyOf(result);
                long units=Math.min(missing,source.stock().getOrDefault(e.getKey(),0L));
                units=Math.min(units,Math.min(haul,Math.min(funds/price,freeSpace/size)));
                if(units==0)continue;
                long cost=Math.multiplyExact(units,price);
                result.add(new WorkshopInputPlan.Order(source.id(),e.getKey(),units,cost));
                funds-=cost;freeSpace-=Math.multiplyExact(units,size);haul-=units;missing-=units;
                if(missing==0)break;
            }
        }
        return List.copyOf(result);
    }
}
