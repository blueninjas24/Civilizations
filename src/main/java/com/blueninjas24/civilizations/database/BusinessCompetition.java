package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.Resident;
import java.sql.*;
import java.util.*;

/** Complete local comparison before one bounded staffing attempt. No poaching, spending or forced closure. */
public final class BusinessCompetition {
    private final Connection c;private final BusinessAllocationPolicy policy;private final BusinessStaffingPolicy staffing;
    private final IndustryRuntimePolicy industry;private final BusinessOperationPolicy operations;
    private final LocalIndustryDemand demand;private final VillageSupplyPurchases publicDemand;
    private final LinkedHashMap<UUID,Long> attempts=new LinkedHashMap<>();
    public BusinessCompetition(Connection c,BusinessAllocationPolicy policy,BusinessStaffingPolicy staffing,
            IndustryRuntimePolicy industry,BusinessOperationPolicy operations,LocalIndustryDemand demand,VillageSupplyPurchases publicDemand){
        this.c=Objects.requireNonNull(c);this.policy=Objects.requireNonNull(policy);this.staffing=Objects.requireNonNull(staffing);
        this.industry=Objects.requireNonNull(industry);this.operations=Objects.requireNonNull(operations);
        this.demand=Objects.requireNonNull(demand);this.publicDemand=Objects.requireNonNull(publicDemand);
    }
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE business_allocation_state (
                    settlement_id TEXT PRIMARY KEY REFERENCES settlements(id),last_business TEXT NOT NULL REFERENCES businesses(id),
                    sequence INTEGER NOT NULL CHECK(typeof(sequence)='integer' AND sequence>0),
                    observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0)
                )
                """);
            s.execute("""
                CREATE TABLE business_allocation_reviews (
                    settlement_id TEXT NOT NULL REFERENCES settlements(id),sequence INTEGER NOT NULL CHECK(sequence>0),
                    business_id TEXT NOT NULL REFERENCES businesses(id),observed_step INTEGER NOT NULL CHECK(observed_step>=0),
                    observed_ms INTEGER NOT NULL CHECK(observed_ms>=0),urgent INTEGER NOT NULL CHECK(urgent IN(0,1)),
                    demand_value INTEGER NOT NULL CHECK(typeof(demand_value)='integer' AND demand_value>=0),
                    policy TEXT NOT NULL,explanation TEXT NOT NULL,PRIMARY KEY(settlement_id,sequence)
                )
                """);
        }
    }
    public boolean enabled(){return policy.enabled()&&staffing.enabled();}
    public String review(UUID town,long step,long now,Set<UUID> worlds,AutomaticBusinessStaffing hiring)throws SQLException {
        if(!c.getAutoCommit())throw new SQLException("Competition requires idle SQL owner");
        if(step<0||now<0)throw new IllegalArgumentException("Invalid allocation time");
        if(!enabled())return "Shared allocation disabled";
        if(worlds==null)return "Shared allocation paused: world availability unknown";
        try(var s=c.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next()||!worlds.contains(UUID.fromString(r.getString(1))))return "Shared allocation paused: world absent";}
        }
        String cursor="";long sequence=0,last=0;
        try(var s=c.prepareStatement("SELECT last_business,sequence,observed_ms FROM business_allocation_state WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next()){cursor=r.getString(1);sequence=r.getLong(2);last=r.getLong(3);}}
        }
        if(sequence>0&&(now<last||now-last<policy.reviewMillis()))return "Shared allocation waiting for review interval";
        var attempted=attempts.get(town);
        if(attempted!=null&&(now<attempted||now-attempted<policy.reviewMillis()))return "Shared allocation waiting for review interval";
        attempts.remove(town);attempts.put(town,now);while(attempts.size()>1024)attempts.remove(attempts.keySet().iterator().next());
        try {
        var workforce=new WorkforceCompetitionReport(c).read(town,operations);
        if(!workforce.complete())return "Shared allocation deferred: "+workforce.explanation();
        var needs=demand.review(town,worlds,32);
        if(!needs.complete())return "Shared allocation deferred: "+needs.explanation();
        var publicNeeds=publicDemand.demand(town,worlds);
        var funded=new TreeMap<>(needs.uncovered());publicNeeds.fundedUnits().forEach((g,q)->funded.merge(g,q,Math::addExact));
        long unmet=0;
        try(var s=c.prepareStatement("SELECT demand-fulfilled FROM needs_periods WHERE settlement_id=? ORDER BY period DESC LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())unmet=r.getLong(1);}
        }
        var firms=new BusinessRepository(c);var people=new PopulationRepository(c);var candidates=new ArrayList<BusinessAllocationPolicy.Candidate>();
        for(var firm:workforce.firms()) {
            if(!firm.available()||firm.vacancies()==0||firm.qualifiedIdleCandidates()==0||!staffing.industries().contains(firm.industry()))continue;
            var saved=firms.findBusiness(firm.id()).orElseThrow();var owner=people.findResident(saved.ownerId()).orElseThrow();
            if(step<saved.foundedStep()||owner.status()!=Resident.Status.ALIVE||!owner.settlementId().equals(town))continue;
            var outputs=outputs(firm.id(),saved.siteId(),firm.industry());if(outputs.isEmpty())continue;
            long value=0;
            for(var good:outputs) {
                long units=funded.getOrDefault(good,0L),price=publicDemand.prices().getOrDefault(good,industry.purchasingSupply().unitHundredths().getOrDefault(good,1L));
                // Compare economic values rather than raw coal/rare-material unit counts; clamp before multiplication.
                long contribution=units>policy.maximumDemandValue()/price?policy.maximumDemandValue():units*price;
                value=Math.min(policy.maximumDemandValue(),value+contribution);
            }
            boolean urgent=unmet>0&&policy.urgentIndustries().contains(firm.industry());
            if(urgent||value>0)candidates.add(new BusinessAllocationPolicy.Candidate(firm.id(),urgent,value));
        }
        var ranked=policy.rank(candidates,cursor);if(ranked.isEmpty())return "Shared allocation: no viable qualified idle-worker match; no jobs changed";
        var chosen=ranked.getFirst();long next=Math.incrementExact(sequence),observed=Math.max(now,last);
        return hiring.review(chosen.business(),step,reason->{
            String explanation="Complete local comparison "+ranked+"; selected "+chosen.business()+"; "+reason+"; no poaching, spending, forced transfers or closures";
            try(var s=c.prepareStatement("INSERT INTO business_allocation_reviews VALUES(?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,town.toString());s.setLong(2,next);s.setString(3,chosen.business().toString());s.setLong(4,step);s.setLong(5,observed);
                s.setInt(6,chosen.urgent()?1:0);s.setLong(7,chosen.fundedDemandValue());s.setString(8,policy.toString());s.setString(9,explanation);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO business_allocation_state VALUES(?,?,?,?) ON CONFLICT(settlement_id) DO UPDATE SET last_business=excluded.last_business,sequence=excluded.sequence,observed_ms=excluded.observed_ms")) {
                s.setString(1,town.toString());s.setString(2,chosen.business().toString());s.setLong(3,next);s.setLong(4,observed);s.executeUpdate();
            }
        })+"; shared priority for village "+town+", selected business "+chosen.business()+": urgent="+chosen.urgent()+", funded unmet value="+chosen.fundedDemandValue()+", compared "+ranked.size()+" viable firms";
        }catch(SQLException|RuntimeException failure){attempts.remove(town);throw failure;}
    }
    private Set<Commodity> outputs(UUID business,UUID site,String kind)throws SQLException {
        var result=new TreeSet<Commodity>();
        if(Set.of("mining","quarry","sandpit").contains(kind)) {
            UUID deposit=null;long remaining=0;
            try(var s=c.prepareStatement("SELECT id,remaining_batches FROM mineral_deposits WHERE site_id=?")) {
                s.setString(1,site.toString());try(var r=s.executeQuery()){if(r.next()){deposit=UUID.fromString(r.getString(1));remaining=r.getLong(2);}}
            }
            if(deposit==null||remaining==0||!new IndustryRepository(c).sourceHistoryTrusted(deposit))return Set.of();
            try(var s=c.prepareStatement("SELECT commodity FROM mineral_yields WHERE deposit_id=?")){s.setString(1,deposit.toString());try(var r=s.executeQuery()){while(r.next())result.add(new Commodity(r.getString(1)));}}
        }else if(kind.equals("logging")) {
            if(new ForestryRepository(c).find(site).filter(f->f.stock().capacity()>0).isPresent()&&new IndustryRepository(c).forestryHistoryTrusted(site))result.add(Commodity.TIMBER);
        }else if(kind.equals("fishing")) {
            if(new FisheryRepository(c).find(site).filter(f->f.stock().capacity()>0).isPresent()&&!FisheryWorkplaces.paused(c,site))result.add(Commodity.FOOD);
        }else if(kind.equals("warehouse"))result.addAll(industry.mining().storageSpacePerUnit().keySet());
        else if(kind.equals("livestock")) {
            var herd=new LivestockRepository(c).load(business);if(herd.isPresent())result.add(new Commodity(herd.orElseThrow().species()==LivestockHerd.Species.COW?"milk":"eggs"));
        }else industry.recipes().values().stream().filter(r->r.industry().equals(kind)).forEach(r->result.addAll(r.outputs().keySet()));
        return Set.copyOf(result);
    }
}
