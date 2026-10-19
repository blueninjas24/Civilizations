package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Bounded preparation followed by atomic current revalidation. Does not inspect Paper, schedule or train. */
public final class CraftsmanInvestment {
    public record Draft(BusinessStartupGrants.WorkshopRequest startup,WorkshopStarterSupplies.Request supplies,WorkplaceOperations.Anchor anchor) {
        public Draft {Objects.requireNonNull(startup);Objects.requireNonNull(supplies);Objects.requireNonNull(anchor);}
    }
    public record Preparation(Optional<Draft> draft,String explanation) {}
    private final Connection connection;
    private final CraftsmanOpportunities opportunities;
    private final WorkshopFoundingPolicy policy;
    private final WorkshopOpportunityPolicy terms;
    private final IndustryRuntimePolicy industry;
    private final StartupGrantPolicy grants;
    private final WorkshopStarterPolicy starter;
    private final long adulthood,seed;
    public CraftsmanInvestment(Connection connection,CraftsmanOpportunities opportunities,WorkshopFoundingPolicy policy,
            WorkshopOpportunityPolicy terms,IndustryRuntimePolicy industry,StartupGrantPolicy grants,WorkshopStarterPolicy starter,long adulthood,long seed) {
        this.connection=Objects.requireNonNull(connection);this.opportunities=Objects.requireNonNull(opportunities);this.policy=Objects.requireNonNull(policy);
        this.terms=Objects.requireNonNull(terms);this.industry=Objects.requireNonNull(industry);this.grants=Objects.requireNonNull(grants);this.starter=Objects.requireNonNull(starter);
        if(adulthood<1)throw new IllegalArgumentException("Invalid founder adulthood");this.adulthood=adulthood;this.seed=seed;
    }
    public Preparation prepare(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        if(step<0||now<0)throw new IllegalArgumentException("Invalid investment time");
        return transaction(()-> {
            if(!policy.enabled())return none("First-craftsman founding disabled");
            var review=opportunities.reviewRows(town,now,worlds==null?Set.of():worlds);
            if(review.candidates().isEmpty())return none(review.explanation());
            var founders=new ArrayList<UUID>();var people=new PopulationRepository(connection);
            try(var s=connection.prepareStatement("""
                SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
                  AND EXISTS(SELECT 1 FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='crafting' AND k.experience>=?)
                  AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
                  AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
                ORDER BY r.id LIMIT ?
                """)) {
                s.setString(1,town.toString());s.setLong(2,Math.max(adulthood,policy.minimumAgeSteps()));s.setLong(3,policy.founderExperience());s.setInt(4,policy.foundersPerReview());
                try(var r=s.executeQuery()){while(r.next()) {
                    var id=UUID.fromString(r.getString(1));
                    if(WorkEligibility.evaluate(people.findResident(id).orElseThrow(),people.findWorkProfile(id).orElseThrow(),ownerRequirements()).eligible())founders.add(id);
                }}
            }
            if(founders.isEmpty())return none("No qualified unemployed adult in bounded founder page; no free skill or employer created");
            // Stable seeded sampling among feasible alternatives; preparation repeats do not reroll or spend.
            var random=new SplittableRandom(seed^town.getMostSignificantBits()^town.getLeastSignificantBits());
            var candidate=review.candidates().get(random.nextInt(review.candidates().size()));var founder=founders.get(random.nextInt(founders.size()));
            return new Preparation(Optional.of(draft(candidate,founder,step)),"Qualified owner and real customer/funding/surplus ready; fresh loaded marker and final SQL revalidation still required; nothing reserved");
        });
    }
    public BusinessStartupGrants.Workshop accept(Draft draft,WorkplaceOperations.Confirmation evidence,long now,Set<UUID> worlds)throws SQLException {
        return accept(draft,evidence,now,worlds,()->{});
    }
    public BusinessStartupGrants.Workshop accept(Draft draft,WorkplaceOperations.Confirmation evidence,long now,Set<UUID> worlds,ResourceInvestment.StartupAudit audit)throws SQLException {
        Objects.requireNonNull(draft);Objects.requireNonNull(evidence);
        if(!draft.anchor().equals(evidence.anchor()))throw new BusinessStartupGrants.Declined("Loaded marker proof does not match proposed fixed anchor");
        return transaction(()-> {
            UUID business=draft.startup().founding().business().id();var firms=new BusinessRepository(connection);
            boolean existing=firms.findBusiness(business).isPresent();
            if(!existing) {
                if(!policy.enabled())throw new BusinessStartupGrants.Declined("First-craftsman founding disabled");
                var request=draft.startup();var b=request.founding().business();
                var current=opportunities.reviewRows(b.settlementId(),now,worlds==null?Set.of():worlds);
                var candidate=current.candidates().stream().filter(c->c.workplace().world().equals(draft.anchor().world())
                        &&c.workplace().position().equals(draft.anchor().position())&&c.workplace().kind()==draft.anchor().kind()).findFirst();
                if(candidate.isEmpty())throw new BusinessStartupGrants.Declined("Craftsman opportunity changed: "+current.explanation());
                if(!draft(candidate.orElseThrow(),b.founderId(),b.foundedStep()).equals(draft))throw new BusinessStartupGrants.Declined("Craftsman terms or observation changed; prepare again");
                if(evidence.capturedMillis()<request.founding().site().observedMillis())throw new BusinessStartupGrants.Declined("Point proof predates proposed marker evidence");
                try(var s=connection.prepareStatement("SELECT 1 FROM business_employment WHERE resident_id=? AND ended_step IS NULL UNION ALL SELECT 1 FROM resident_courses WHERE resident_id=? AND status='ACTIVE' LIMIT 1")) {
                    s.setString(1,b.founderId().toString());s.setString(2,b.founderId().toString());try(var r=s.executeQuery()){if(r.next())throw new BusinessStartupGrants.Declined("Founder already employed or studying");}
                }
            } else if(!firms.findFounding(business).orElseThrow().equals(draft.startup().founding()))throw new SQLException("Accepted craftsman identity reused with changed founding");
            return new BusinessStartupGrants(connection).startMarkerWorkshopRows(draft.startup(),grants,draft.supplies(),starter,worlds,evidence,now);
        },audit);
    }
    private Draft draft(CraftsmanOpportunities.Opportunity opportunity,UUID founder,long step) {
        var marker=opportunity.workplace();var p=marker.position();String key=opportunity.town()+":"+marker.world()+":"+p.x()+":"+p.y()+":"+p.z();
        UUID business=id("civilizations:first-craftsman-business-v1:"+key),site=id("civilizations:first-craftsman-site-v1:"+key);
        var reason="First craftsman for actual funded storage fittings; provisional marker, no property rights; "+policy+"; "+terms;
        var location=new BusinessSite(site,opportunity.town(),marker.world(),p.x(),p.y(),p.z(),marker.capturedMillis(),reason);
        var firm=new Business(business,opportunity.town(),"Craftsman",founder,founder,"craftsman",site,policy.workerCapacity(),step);
        var founding=new BusinessFounding(firm,location,ownerRequirements(),new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of("crafting",policy.workerExperience())),terms.startupHundredths(),reason);
        var request=new BusinessStartupGrants.WorkshopRequest(founding,terms.capacity(),industry.mining().storageSpacePerUnit(),terms.capitalHundredths(),founder);
        var supply=new WorkshopStarterSupplies.Request(id("civilizations:first-craftsman-starter-v1:"+key),business,opportunity.recipe(),1,0,industry.mining().localDeliveryPeriods(),industry.mining().localHaulUnitsPerPeriod());
        return new Draft(request,supply,new WorkplaceOperations.Anchor(business,marker.world(),p,marker.kind()));
    }
    private JobRequirements ownerRequirements(){return new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of("crafting",policy.founderExperience()));}
    private static UUID id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));}
    private static Preparation none(String reason){return new Preparation(Optional.empty(),reason);}
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        return transaction(work,()->{});
    }
    private <T>T transaction(Work<T> work,ResourceInvestment.StartupAudit audit)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Investment requires idle SQL owner");connection.setAutoCommit(false);
        try{var result=work.run();audit.save();connection.commit();return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{connection.setAutoCommit(true);}
    }
}
