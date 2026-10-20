package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.population.*;
import java.util.*;

/** Deterministic proposed labor split. Actual booking/credit must share the simulation owner's work ledger. */
public record ConstructionLaborPolicy(boolean enabled,int villageWorkers,int businessWorkers,int effortPerWorker,int businessSharePercent) {
    public record Worker(Resident resident,WorkProfile profile,UUID employer,int remainingEffort) {
        public Worker{Objects.requireNonNull(resident);Objects.requireNonNull(profile);if(!resident.id().equals(profile.residentId())||remainingEffort<0||remainingEffort>10_000)throw new IllegalArgumentException("Invalid construction worker evidence");}
    }
    public record Allocation(UUID resident,int constructionEffort,int remainingEffort){}
    public record Plan(List<Allocation> allocations,String explanation) {
        public Plan{allocations=List.copyOf(allocations);}
        public int proposedWork(){return allocations.stream().mapToInt(Allocation::constructionEffort).sum();}
    }
    public ConstructionLaborPolicy {
        if(villageWorkers<1||villageWorkers>64||businessWorkers<1||businessWorkers>64||effortPerWorker<1||effortPerWorker>10_000||businessSharePercent<1||businessSharePercent>100)
            throw new IllegalArgumentException("Invalid construction labor limits");
    }
    /** A null business means village work: employed residents are ineligible. Existing jobs are never changed. */
    public Plan propose(UUID town,UUID business,JobRequirements qualifications,List<Worker> workers,long seed,long period,boolean worldAvailable) {
        Objects.requireNonNull(town);Objects.requireNonNull(qualifications);workers=List.copyOf(workers);
        if(period<0||workers.size()>1024)throw new IllegalArgumentException("Invalid construction labor review");
        var identities=new HashSet<UUID>();for(var worker:workers)if(!identities.add(worker.resident().id())||worker.remainingEffort()>effortPerWorker)throw new IllegalArgumentException("Duplicate worker or effort beyond the configured work budget");
        if(!enabled||!worldAvailable)return new Plan(List.of(),"Construction labor waits for an enabled policy and available world");
        var selected=workers.stream().filter(w->w.resident().settlementId().equals(town)&&Objects.equals(w.employer(),business)
                &&w.remainingEffort()>0&&WorkEligibility.evaluate(w.resident(),w.profile(),qualifications).eligible())
                .sorted((a,b)->{int n=Long.compareUnsigned(rank(a.resident().id(),seed,period),rank(b.resident().id(),seed,period));return n==0?a.resident().id().compareTo(b.resident().id()):n;})
                .limit(business==null?villageWorkers:businessWorkers).toList();
        var allocations=new ArrayList<Allocation>();int cap=business==null?effortPerWorker:effortPerWorker*businessSharePercent/100;
        for(var worker:selected) {
            int effort=Math.min(cap,worker.remainingEffort());if(effort>0)allocations.add(new Allocation(worker.resident().id(),effort,worker.remainingEffort()-effort));
        }
        return new Plan(allocations,allocations.isEmpty()?"No qualified local labor with unspent effort":"Proposed construction effort replaces normal effort; owner booking still required");
    }
    private static long rank(UUID id,long seed,long period){long n=id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),17)^seed^(period*0x9e3779b97f4a7c15L);n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    public static ConstructionLaborPolicy prototype(){return new ConstructionLaborPolicy(true,2,1,100,25);}
}
