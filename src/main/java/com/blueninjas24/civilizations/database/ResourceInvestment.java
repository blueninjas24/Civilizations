package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Shared choice between current viable resource startups; every winning grant still revalidates normally. */
public final class ResourceInvestment {
    @FunctionalInterface interface StartupAudit {void save()throws SQLException;}
    private record Proposal(UUID id,String kind,Commodity product,long debit,String evidence) {}
    private final Connection c;private final BusinessAllocationPolicy policy;private final IndustryRuntimePolicy industry;
    private final AutomaticMineFounding mines;private final AutomaticQuarryFounding quarries;private final AutomaticForestryFounding forestry;
    private final LocalIndustryDemand demand;private final VillageSupplyPurchases publicDemand;
    private final LinkedHashMap<UUID,Long> attempts=new LinkedHashMap<>();private String townCursor="";
    public ResourceInvestment(Connection c,BusinessAllocationPolicy policy,IndustryRuntimePolicy industry,AutomaticMineFounding mines,
            AutomaticQuarryFounding quarries,AutomaticForestryFounding forestry,LocalIndustryDemand demand,VillageSupplyPurchases publicDemand){
        this.c=Objects.requireNonNull(c);this.policy=Objects.requireNonNull(policy);this.industry=Objects.requireNonNull(industry);
        this.mines=Objects.requireNonNull(mines);this.quarries=Objects.requireNonNull(quarries);this.forestry=Objects.requireNonNull(forestry);
        this.demand=Objects.requireNonNull(demand);this.publicDemand=Objects.requireNonNull(publicDemand);
    }
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE resource_investment_state (
                    settlement_id TEXT PRIMARY KEY REFERENCES settlements(id),last_proposal TEXT NOT NULL,
                    sequence INTEGER NOT NULL CHECK(sequence>0),observed_ms INTEGER NOT NULL CHECK(observed_ms>=0)
                )
                """);
            s.execute("""
                CREATE TABLE resource_investment_reviews (
                    business_id TEXT PRIMARY KEY REFERENCES startup_grants(business_id),settlement_id TEXT NOT NULL REFERENCES settlements(id),
                    sequence INTEGER NOT NULL CHECK(sequence>0),proposal TEXT NOT NULL,industry TEXT NOT NULL,
                    observed_step INTEGER NOT NULL CHECK(observed_step>=0),observed_ms INTEGER NOT NULL CHECK(observed_ms>=0),
                    demand_value INTEGER NOT NULL CHECK(demand_value>=0),policy TEXT NOT NULL,explanation TEXT NOT NULL,
                    UNIQUE(settlement_id,sequence)
                )
                """);
        }
    }
    public String pulse(long step,long now,Set<UUID> worlds)throws SQLException {
        if(!policy.enabled())return "Shared resource investment disabled";
        UUID town=nextTown();if(town==null)return "No initialized villages for resource investment";
        townCursor=town.toString();return review(town,step,now,worlds);
    }
    private UUID nextTown()throws SQLException {
        try(var s=c.prepareStatement("SELECT settlement_id FROM economy_accounts WHERE settlement_id>? ORDER BY settlement_id LIMIT 1")) {
            s.setString(1,townCursor);try(var r=s.executeQuery()){if(r.next())return UUID.fromString(r.getString(1));}
        }
        if(!townCursor.isEmpty()){townCursor="";return nextTown();}return null;
    }
    public String review(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        if(!c.getAutoCommit())throw new SQLException("Resource investment requires idle owner");
        if(step<0||now<0)throw new IllegalArgumentException("Invalid resource investment time");
        if(!policy.enabled()||worlds==null)return "Shared resource investment disabled or world availability unknown";
        try(var s=c.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")){
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next()||!worlds.contains(UUID.fromString(r.getString(1))))return "Resource investment paused with absent world";}
        }
        String cursor="";long sequence=0,last=0;
        try(var s=c.prepareStatement("SELECT last_proposal,sequence,observed_ms FROM resource_investment_state WHERE settlement_id=?")){
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next()){cursor=r.getString(1);sequence=r.getLong(2);last=r.getLong(3);}}
        }
        long previous=Math.max(last,attempts.getOrDefault(town,Long.MIN_VALUE));
        if((sequence>0||attempts.containsKey(town))&&(now<previous||now-previous<policy.reviewMillis()))return "Shared resource investment waiting for review interval";
        attempts.remove(town);attempts.put(town,now);while(attempts.size()>1024)attempts.remove(attempts.keySet().iterator().next());
        try {
            var pressure=demand.review(town,worlds,32);if(!pressure.complete())return "Resource investment deferred: "+pressure.explanation();
            var proposals=new ArrayList<Proposal>();
            var notes=new ArrayList<String>();
            // These existing selectors validate observation age/coverage, protections, existing capacity,
            // workplace range, real protected grants and funded customers; no source is invented here.
            c.setAutoCommit(false);
            try {
                var m=mines.opportunityRows(town,now,worlds,null);
                notes.add("mining: "+m.explanation()+(m.opportunity()!=null&&mines.founder(town)==null?"; no qualified idle founder":""));
                if(m.opportunity()!=null&&mines.founder(town)!=null)proposals.add(proposal("mining",m.opportunity().key(),m.opportunity().patch().mineral(),mines.investmentCost(),m.explanation()));
                var q=quarries.opportunityRows(town,now,worlds,null);
                notes.add("quarry/sand: "+q.explanation()+(q.opportunity()!=null&&quarries.founder(town)==null?"; no qualified idle founder":""));
                if(q.opportunity()!=null&&quarries.founder(town)!=null)proposals.add(proposal(q.opportunity().patch().mineral().equals(Commodity.STONE)?"quarry":"sandpit",q.opportunity().key(),q.opportunity().patch().mineral(),quarries.investmentCost(),q.explanation()));
                var f=forestry.opportunityRows(town,now,worlds,null);
                notes.add("logging: "+f.explanation()+(f.opportunity()!=null&&forestry.founder(town)==null?"; no qualified idle founder":""));
                if(f.opportunity()!=null&&forestry.founder(town)!=null)proposals.add(proposal("logging",f.opportunity().key(),Commodity.TIMBER,forestry.investmentCost(),f.explanation()));
                c.rollback();c.setAutoCommit(true); // Read-only comparison; startup revalidates before creating anything.
            }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
            var candidates=new ArrayList<BusinessAllocationPolicy.Candidate>();
            for(var p:proposals){
                long units=Math.addExact(pressure.uncovered().getOrDefault(p.product(),0L),publicDemand.demandAfterSpending(town,worlds,p.debit()).fundedUnits().getOrDefault(p.product(),0L));
                long price=publicDemand.prices().getOrDefault(p.product(),industry.purchasingSupply().unitHundredths().getOrDefault(p.product(),1L));
                long value=units>policy.maximumDemandValue()/price?policy.maximumDemandValue():units*price;
                if(value>0)candidates.add(new BusinessAllocationPolicy.Candidate(p.id(),false,value));
            }
            var ranked=policy.rank(candidates,cursor);if(ranked.isEmpty())return "No currently viable qualified/funded resource startup: "+String.join("; ",notes)+"; funded shortages "+pressure.uncovered();
            var selected=ranked.getFirst();var proposal=proposals.stream().filter(p->p.id().equals(selected.business())).findFirst().orElseThrow();
            long before;
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COALESCE(MAX(rowid),0) FROM startup_grants")){r.next();before=r.getLong(1);}
            long next=Math.incrementExact(sequence),observed=Math.max(now,last);
            StartupAudit audit=()->saveRows(town,proposal,selected,ranked,before,next,step,observed);
            String result=switch(proposal.kind()){
                case "mining"->mines.review(town,step,now,worlds,audit);
                case "quarry","sandpit"->quarries.review(town,step,now,worlds,audit);
                case "logging"->forestry.review(town,step,now,worlds,audit);
                default->throw new SQLException("Unsupported compared resource industry");
            };
            return "Shared resource choice "+proposal.kind()+", funded demand value "+selected.fundedDemandValue()+" from "+ranked.size()+" viable adapters: "+result;
        }catch(SQLException|RuntimeException failure){attempts.remove(town);throw failure;}
    }
    private Proposal proposal(String kind,String key,Commodity product,long debit,String evidence){
        return new Proposal(UUID.nameUUIDFromBytes(("civilizations:resource-choice-v1:"+kind+":"+key).getBytes(StandardCharsets.UTF_8)),kind,product,debit,evidence);
    }
    private void saveRows(UUID town,Proposal p,BusinessAllocationPolicy.Candidate selected,List<BusinessAllocationPolicy.Candidate> ranked,
            long before,long next,long step,long now)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Investment audit requires startup transaction");
        UUID business=null;
        try(var s=c.prepareStatement("SELECT business_id,settlement_id FROM startup_grants WHERE rowid>? ORDER BY rowid LIMIT 2")){
            s.setLong(1,before);try(var r=s.executeQuery()){if(r.next()){business=UUID.fromString(r.getString(1));if(!r.getString(2).equals(town.toString())||r.next())throw new SQLException("Investment did not create exactly one local grant");}}
        }
        if(business==null)return; // Revalidation refused a now-stale opportunity; no grant or cursor advancement.
        try(var s=c.prepareStatement("INSERT INTO resource_investment_reviews VALUES(?,?,?,?,?,?,?,?,?,?)")){
            s.setString(1,business.toString());s.setString(2,town.toString());s.setLong(3,next);s.setString(4,p.id().toString());s.setString(5,p.kind());s.setLong(6,step);s.setLong(7,now);
            s.setLong(8,selected.fundedDemandValue());s.setString(9,policy.toString());s.setString(10,p.evidence()+"; compared "+ranked+"; actual protected startup revalidated; no new money");s.executeUpdate();
        }
        try(var s=c.prepareStatement("INSERT INTO resource_investment_state VALUES(?,?,?,?) ON CONFLICT(settlement_id) DO UPDATE SET last_proposal=excluded.last_proposal,sequence=excluded.sequence,observed_ms=excluded.observed_ms")){
            s.setString(1,town.toString());s.setString(2,p.id().toString());s.setLong(3,next);s.setLong(4,now);s.executeUpdate();
        }
    }
}
