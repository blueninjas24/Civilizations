package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Bounded deterministic candidate footprints. Input must come from complete, unclaimed current evidence. */
public final class WaterPatch {
    public record Sample(FisheryObservation.Column position,int surfaceY,long capturedMillis) {
        public Sample {Objects.requireNonNull(position);if(capturedMillis<0)throw new IllegalArgumentException("Invalid water capture time");}
    }
    public record Patch(FisheryObservation evidence,int surfaceY) {}
    private WaterPatch() {}

    /** Cardinally connected water at one surface level; returned evidence still requires live revalidation before funding. */
    public static List<Patch> choose(UUID world,List<Sample> samples,int homeX,int homeZ,long now,
            long maximumAgeMillis,int minimumColumns,int maximumColumns,int maximumSites) {
        Objects.requireNonNull(world);samples=List.copyOf(samples);
        if(now<0||maximumAgeMillis<0||minimumColumns<1||maximumColumns<minimumColumns||maximumColumns>4096
                ||maximumSites<1||maximumSites>64||samples.size()>4096)
            throw new IllegalArgumentException("Invalid water patch bounds");
        var all=new HashMap<FisheryObservation.Column,Sample>();
        for(var sample:samples)if(all.put(sample.position(),sample)!=null)throw new IllegalArgumentException("Duplicate water coordinate");
        long earliest=Math.max(0,now-maximumAgeMillis);
        all.values().removeIf(s->s.capturedMillis()<earliest||s.capturedMillis()>now);
        var order=Comparator.comparingLong((FisheryObservation.Column p)->Math.abs((long)p.x()-homeX)+Math.abs((long)p.z()-homeZ))
                .thenComparingInt(FisheryObservation.Column::x).thenComparingInt(FisheryObservation.Column::z);
        var remaining=new TreeSet<FisheryObservation.Column>(order);remaining.addAll(all.keySet());
        var result=new ArrayList<Patch>();
        while(!remaining.isEmpty()&&result.size()<maximumSites) {
            var origin=remaining.first();int surface=all.get(origin).surfaceY();
            var queue=new ArrayDeque<FisheryObservation.Column>();var component=new HashSet<FisheryObservation.Column>();
            queue.add(origin);remaining.remove(origin);component.add(origin);
            while(!queue.isEmpty())for(var next:neighbors(queue.removeFirst(),order))
                if(remaining.contains(next)&&all.get(next).surfaceY()==surface){remaining.remove(next);component.add(next);queue.addLast(next);}
            if(component.size()<minimumColumns)continue;
            // Choose one bounded connected working patch per observed component, rather than claiming a whole ocean.
            var selected=new ArrayList<FisheryObservation.Column>();var visited=new HashSet<FisheryObservation.Column>();
            queue.add(origin);visited.add(origin);long oldest=now;
            while(!queue.isEmpty()&&selected.size()<maximumColumns) {
                var position=queue.removeFirst();selected.add(position);oldest=Math.min(oldest,all.get(position).capturedMillis());
                for(var next:neighbors(position,order))if(component.contains(next)&&visited.add(next))queue.addLast(next);
            }
            result.add(new Patch(new FisheryObservation(world,oldest,true,selected),surface));
        }
        return List.copyOf(result);
    }
    private static List<FisheryObservation.Column> neighbors(FisheryObservation.Column p,Comparator<FisheryObservation.Column> order) {
        var result=new ArrayList<FisheryObservation.Column>(4);
        if(p.x()>Integer.MIN_VALUE)result.add(new FisheryObservation.Column(p.x()-1,p.z()));
        if(p.x()<Integer.MAX_VALUE)result.add(new FisheryObservation.Column(p.x()+1,p.z()));
        if(p.z()>Integer.MIN_VALUE)result.add(new FisheryObservation.Column(p.x(),p.z()-1));
        if(p.z()<Integer.MAX_VALUE)result.add(new FisheryObservation.Column(p.x(),p.z()+1));
        result.sort(order);return result;
    }
}
