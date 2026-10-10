package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.math.BigInteger;
import java.util.*;

/** Deterministic affordable complete-bundle planning. In-transit inputs prevent duplicate purchases, not production. */
public final class WorkshopInputPlan {
    public record Warehouse(UUID id,Map<Commodity,Long> stock) {
        public Warehouse {Objects.requireNonNull(id);stock=Map.copyOf(stock);if(stock.values().stream().anyMatch(n->n<0))throw new IllegalArgumentException("Negative warehouse stock");}
    }
    public record Order(UUID seller,Commodity commodity,long units,long price) {}
    public record Plan(long batches,List<Order> orders,long price,String explanation) {
        public Plan {orders=List.copyOf(orders);}
    }
    private WorkshopInputPlan() {}
    public static Plan choose(ProductionRecipe recipe,long workers,long funds,long freeSpace,long haulUnits,int orderSlots,
                              Map<Commodity,Long> held,Map<Commodity,Long> pending,Map<Commodity,Long> sizes,
                              List<Warehouse> warehouses,WorkshopSupplyPolicy policy) {
        if(workers<0 || funds<0 || freeSpace<0 || haulUnits<0 || orderSlots<0 || orderSlots>128 || held.values().stream().anyMatch(n->n<0) || pending.values().stream().anyMatch(n->n<0))
            throw new IllegalArgumentException("Negative purchasing resources");
        var goods=new HashSet<>(recipe.inputs().keySet());goods.addAll(recipe.outputs().keySet());
        if(goods.stream().anyMatch(g->!held.containsKey(g) || !sizes.containsKey(g) || sizes.get(g)<1))
            throw new IllegalArgumentException("Store must support every recipe input/output");
        if(warehouses.size()>policy.warehousesPerReview() || warehouses.stream().map(Warehouse::id).distinct().count()!=warehouses.size())
            throw new IllegalArgumentException("Bounded distinct warehouse snapshots required");
        warehouses=warehouses.stream().sorted(Comparator.comparing(w->w.id().toString())).toList();
        if(!policy.enabled())return new Plan(0,List.of(),0,"Automatic workshop supplies disabled");
        long maximum=Math.multiplyExact(Math.min(recipe.siteCapacity(),Math.multiplyExact(workers,recipe.batchesPerWorker())),policy.inputPeriods());
        long lo=0,hi=maximum;
        while(lo<hi) {
            long mid=lo+(hi-lo+1)/2;
            if(tryPlan(recipe,mid,funds,freeSpace,haulUnits,orderSlots,held,pending,sizes,warehouses,policy)!=null)lo=mid;else hi=mid-1;
        }
        var orders=tryPlan(recipe,lo,funds,freeSpace,haulUnits,orderSlots,held,pending,sizes,warehouses,policy);
        long price=0;for(var order:orders)price=Math.addExact(price,order.price());
        return new Plan(lo,orders,price,lo==0?"Supply deferred: no complete affordable input bundle, qualified labor or storage"
                :"Recipe supplies: covers "+lo+" work batches; "+orders.size()+" purchases; cost "+price+" hundredths; in-transit inputs counted only for ordering");
    }
    private static List<Order> tryPlan(ProductionRecipe recipe,long batches,long funds,long freeSpace,long haulUnits,int orderSlots,
                                       Map<Commodity,Long> held,Map<Commodity,Long> pending,Map<Commodity,Long> sizes,
                                       List<Warehouse> warehouses,WorkshopSupplyPolicy policy) {
        var orders=new ArrayList<Order>();var cost=BigInteger.ZERO;var incoming=BigInteger.ZERO;var hauled=BigInteger.ZERO;
        for(var e:new TreeMap<>(recipe.inputs()).entrySet()) {
            long wanted=Math.multiplyExact(e.getValue(),batches);
            long covered=Math.addExact(held.get(e.getKey()),pending.getOrDefault(e.getKey(),0L));
            long missing=Math.max(0,wanted-covered);
            if(missing==0)continue;
            hauled=hauled.add(BigInteger.valueOf(missing));if(hauled.compareTo(BigInteger.valueOf(haulUnits))>0)return null;
            Long unit=policy.unitHundredths().get(e.getKey());if(unit==null)return null;
            incoming=incoming.add(BigInteger.valueOf(missing).multiply(BigInteger.valueOf(sizes.get(e.getKey()))));
            for(var warehouse:warehouses) {
                long quantity=Math.min(missing,warehouse.stock().getOrDefault(e.getKey(),0L));if(quantity==0)continue;
                var price=BigInteger.valueOf(quantity).multiply(BigInteger.valueOf(unit));cost=cost.add(price);
                if(cost.compareTo(BigInteger.valueOf(funds))>0)return null;
                orders.add(new Order(warehouse.id(),e.getKey(),quantity,price.longValueExact()));
                if(orders.size()>orderSlots)return null;
                missing-=quantity;if(missing==0)break;
            }
            if(missing>0)return null;
        }
        var net=weight(recipe.outputs(),sizes).subtract(weight(recipe.inputs(),sizes)).max(BigInteger.ZERO);
        if(incoming.add(net.multiply(BigInteger.valueOf(batches))).compareTo(BigInteger.valueOf(freeSpace))>0)return null;
        return List.copyOf(orders);
    }
    private static BigInteger weight(Map<Commodity,Long> goods,Map<Commodity,Long> sizes) {
        var value=BigInteger.ZERO;for(var e:goods.entrySet())value=value.add(BigInteger.valueOf(e.getValue()).multiply(BigInteger.valueOf(sizes.get(e.getKey()))));return value;
    }
}
