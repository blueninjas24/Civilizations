package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Qualified first-warehouse preparation and atomic current acceptance. Never trains or fabricates stock. */
public final class WarehouseInvestment {
    public record Draft(BusinessStartupGrants.WarehouseRequest startup,WorkplaceOperations.Anchor anchor) {
        public Draft {Objects.requireNonNull(startup);Objects.requireNonNull(anchor);}
    }
    public record Preparation(Optional<Draft> draft,String explanation) {}
    private final Connection connection;
    private final WarehouseOpportunities opportunities;
    private final WarehouseFoundingPolicy policy;
    private final WarehouseOpportunityPolicy terms;
    private final IndustryRuntimePolicy industry;
    private final StartupGrantPolicy grants;
    private final long adulthood,seed;
    public WarehouseInvestment(Connection connection,WarehouseOpportunities opportunities,WarehouseFoundingPolicy policy,
            WarehouseOpportunityPolicy terms,IndustryRuntimePolicy industry,StartupGrantPolicy grants,long adulthood,long seed) {
        this.connection=Objects.requireNonNull(connection);this.opportunities=Objects.requireNonNull(opportunities);this.policy=Objects.requireNonNull(policy);
        this.terms=Objects.requireNonNull(terms);this.industry=Objects.requireNonNull(industry);this.grants=Objects.requireNonNull(grants);
        if(adulthood<1)throw new IllegalArgumentException("Invalid founder adulthood");this.adulthood=adulthood;this.seed=seed;
    }
    public Preparation prepare(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);if(step<0||now<0)throw new IllegalArgumentException("Invalid investment time");
        return transaction(()-> {
            if(!policy.enabled())return none("First-warehouse founding disabled");
            var review=opportunities.reviewRows(town,now,worlds==null?Set.of():Set.copyOf(worlds));
            if(review.candidates().isEmpty())return none(review.explanation());
            var founders=new ArrayList<UUID>();var people=new PopulationRepository(connection);
            try(var s=connection.prepareStatement("""
                SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
                  AND EXISTS(SELECT 1 FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='commerce' AND k.experience>=?)
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
            if(founders.isEmpty())return none("No qualified unemployed adult in bounded merchant page; no free skill or business created");
            var random=new SplittableRandom(seed^town.getMostSignificantBits()^town.getLeastSignificantBits());
            var candidate=review.candidates().get(random.nextInt(review.candidates().size()));var founder=founders.get(random.nextInt(founders.size()));
            return new Preparation(Optional.of(draft(candidate,founder,step)),"Qualified merchant and complete fundable mineral-chain prospect; fresh loaded barrel and final SQL revalidation required; nothing reserved");
        });
    }
    public BusinessStartupGrants.Warehouse accept(Draft draft,WorkplaceOperations.Confirmation evidence,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(draft);Objects.requireNonNull(evidence);
        if(now<0)throw new IllegalArgumentException("Invalid warehouse acceptance time");
        if(!draft.anchor().equals(evidence.anchor()))throw new BusinessStartupGrants.Declined("Loaded barrel proof does not match proposed fixed anchor");
        return transaction(()-> {
            UUID business=draft.startup().founding().business().id();var firms=new BusinessRepository(connection);
            if(firms.findBusiness(business).isEmpty()) {
                if(!policy.enabled())throw new BusinessStartupGrants.Declined("First-warehouse founding disabled");
                var request=draft.startup();var b=request.founding().business();
                var current=opportunities.reviewRows(b.settlementId(),now,worlds==null?Set.of():Set.copyOf(worlds));
                var candidate=current.candidates().stream().filter(c->c.workplace().world().equals(draft.anchor().world())
                        &&c.workplace().position().equals(draft.anchor().position())&&c.workplace().kind()==draft.anchor().kind()).findFirst();
                if(candidate.isEmpty())throw new BusinessStartupGrants.Declined("Warehouse opportunity changed: "+current.explanation());
                if(!draft(candidate.orElseThrow(),b.founderId(),b.foundedStep()).equals(draft))throw new BusinessStartupGrants.Declined("Warehouse terms or observation changed; prepare again");
                if(evidence.capturedMillis()<request.founding().site().observedMillis())throw new BusinessStartupGrants.Declined("Point proof predates proposed barrel evidence");
                try(var s=connection.prepareStatement("SELECT 1 FROM business_employment WHERE resident_id=? AND ended_step IS NULL UNION ALL SELECT 1 FROM resident_courses WHERE resident_id=? AND status='ACTIVE' LIMIT 1")) {
                    s.setString(1,b.founderId().toString());s.setString(2,b.founderId().toString());try(var r=s.executeQuery()){if(r.next())throw new BusinessStartupGrants.Declined("Merchant already employed or studying");}
                }
            } else if(!firms.findFounding(business).orElseThrow().equals(draft.startup().founding()))throw new SQLException("Accepted warehouse identity reused with changed founding");
            return new BusinessStartupGrants(connection).startMarkerWarehouseRows(draft.startup(),grants,worlds,evidence,now);
        });
    }
    private Draft draft(WarehouseOpportunities.Opportunity opportunity,UUID founder,long step) {
        var marker=opportunity.workplace();var p=marker.position();String key=opportunity.town()+":"+marker.world()+":"+p.x()+":"+p.y()+":"+p.z();
        UUID business=id("civilizations:first-warehouse-business-v1:"+key),site=id("civilizations:first-warehouse-site-v1:"+key);
        var reason="First warehouse for actual funded workshop inputs and suitable finite ore; provisional barrel, no property rights; "+policy+"; "+terms;
        var location=new BusinessSite(site,opportunity.town(),marker.world(),p.x(),p.y(),p.z(),marker.capturedMillis(),reason);
        var firm=new Business(business,opportunity.town(),"Village warehouse",founder,founder,"warehouse",site,policy.workerCapacity(),step);
        var founding=new BusinessFounding(firm,location,ownerRequirements(),new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of("commerce",policy.workerExperience())),terms.startupHundredths(),reason);
        var request=new BusinessStartupGrants.WarehouseRequest(founding,terms.capacity(),industry.mining().storageSpacePerUnit(),terms.capitalHundredths(),founder);
        return new Draft(request,new WorkplaceOperations.Anchor(business,marker.world(),p,marker.kind()));
    }
    private JobRequirements ownerRequirements(){return new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of("commerce",policy.founderExperience()));}
    private static UUID id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));}
    private static Preparation none(String reason){return new Preparation(Optional.empty(),reason);}
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Warehouse investment requires idle SQL owner");connection.setAutoCommit(false);
        try{var result=work.run();connection.commit();return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{connection.setAutoCommit(true);}
    }
}
