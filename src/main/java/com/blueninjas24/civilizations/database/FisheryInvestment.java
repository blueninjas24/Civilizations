package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.sql.*;
import java.util.*;

/** Read-only qualified preparation. No invented shoreline anchor, experience, reservation or founding. */
public final class FisheryInvestment {
    public record Proposal(FisheryOpportunities.Opportunity opportunity,UUID founder,long foundedStep,
            JobRequirements ownerRequirements,JobRequirements workerRequirements,int workerCapacity) {}
    public record Preparation(Optional<Proposal> proposal,String explanation) {}
    private final Connection connection;
    private final FisheryOpportunities opportunities;
    private final FisheryFoundingPolicy policy;
    private final long adulthood,seed;
    private final FisheryOpportunityPolicy terms;
    private final IndustryRuntimePolicy industry;
    private final WaterObserverPolicy water;
    private final FisheryShorelinePolicy shoreline;
    private final StartupGrantPolicy grants;
    public FisheryInvestment(Connection connection,FisheryOpportunities opportunities,FisheryFoundingPolicy policy,long adulthood,long seed) {
        this(connection,opportunities,policy,FisheryOpportunityPolicy.disabled(),IndustryRuntimePolicy.disabled(),WaterObserverPolicy.disabled(),FisheryShorelinePolicy.disabled(),StartupGrantPolicy.disabled(),adulthood,seed);
    }
    public FisheryInvestment(Connection connection,FisheryOpportunities opportunities,FisheryFoundingPolicy policy,FisheryOpportunityPolicy terms,
            IndustryRuntimePolicy industry,WaterObserverPolicy water,FisheryShorelinePolicy shoreline,StartupGrantPolicy grants,long adulthood,long seed) {
        this.terms=Objects.requireNonNull(terms);this.industry=Objects.requireNonNull(industry);this.water=Objects.requireNonNull(water);this.shoreline=Objects.requireNonNull(shoreline);this.grants=Objects.requireNonNull(grants);
        this.connection=Objects.requireNonNull(connection);this.opportunities=Objects.requireNonNull(opportunities);this.policy=Objects.requireNonNull(policy);
        if(adulthood<1)throw new IllegalArgumentException("Invalid founder adulthood");this.adulthood=adulthood;this.seed=seed;
    }
    public Preparation prepare(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        Objects.requireNonNull(town);if(step<0||now<0)throw new IllegalArgumentException("Invalid fishery preparation time");
        if(!connection.getAutoCommit())throw new SQLException("Fishery preparation requires idle SQL owner");connection.setAutoCommit(false);
        try {
            var result=prepareRows(town,step,now,worlds==null?Set.of():Set.copyOf(worlds));connection.commit();connection.setAutoCommit(true);return result;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private Preparation prepareRows(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        if(!policy.enabled())return none("First-fishery owner preparation disabled");
        var review=opportunities.reviewRows(town,now,worlds);if(review.candidates().isEmpty())return none(review.explanation());
        long age=Math.max(adulthood,policy.minimumAgeSteps());var ownerRequirements=new JobRequirements(age,Map.of("fishing",policy.founderExperience()));
        var workerRequirements=new JobRequirements(age,Map.of("fishing",policy.workerExperience()));
        var founders=new ArrayList<UUID>();var people=new PopulationRepository(connection);
        try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
              AND EXISTS(SELECT 1 FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='fishing' AND k.experience>=?)
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setLong(2,age);s.setLong(3,policy.founderExperience());s.setInt(4,policy.foundersPerReview());
            try(var r=s.executeQuery()){while(r.next()) {
                var id=UUID.fromString(r.getString(1));var person=people.findResident(id).orElseThrow();var profile=people.findWorkProfile(id).orElseThrow();
                if(WorkEligibility.evaluate(person,profile,ownerRequirements).eligible()&&WorkEligibility.evaluate(person,profile,workerRequirements).eligible())founders.add(id);
            }}
        }
        if(founders.isEmpty())return none("No qualified unemployed adult in bounded fishing-founder page; no free skill or job changes");
        var random=new SplittableRandom(seed^town.getMostSignificantBits()^town.getLeastSignificantBits());
        var candidate=review.candidates().get(random.nextInt(review.candidates().size()));var founder=founders.get(random.nextInt(founders.size()));
        return new Preparation(Optional.of(new Proposal(candidate,founder,step,ownerRequirements,workerRequirements,policy.workerCapacity())),
                "Qualified first owner/worker and funded water opportunity ready; fresh actual shoreline/water proof and final current acceptance still required; nothing reserved");
    }
    /** Fresh caller-verified bank and current SQL opportunity/owner qualification commit with all paid startup rows. */
    public BusinessStartupGrants.Fishery accept(Proposal proposal,ShorelineEvidence evidence,long now,Set<UUID> worlds,FishingPolicy fishing)throws SQLException {
        Objects.requireNonNull(proposal);Objects.requireNonNull(evidence);Objects.requireNonNull(fishing);
        if(!connection.getAutoCommit())throw new SQLException("Fishery acceptance requires idle SQL owner");connection.setAutoCommit(false);
        try {
            var request=draft(proposal,evidence);var firms=new BusinessRepository(connection);var existing=firms.findBusiness(request.founding().business().id());
            if(existing.isEmpty()) {
                if(!policy.enabled()||!shoreline.enabled()||!industry.enabled()||!water.enabled()||!fishing.enabled())throw new BusinessStartupGrants.Declined("First-fishery acceptance disabled");
                var review=opportunities.reviewRows(proposal.opportunity().town(),now,worlds==null?Set.of():Set.copyOf(worlds));
                if(!review.candidates().contains(proposal.opportunity()))throw new BusinessStartupGrants.Declined("Fishery water/customer/funding opportunity changed; prepare again");
                long age=Math.max(adulthood,policy.minimumAgeSteps());
                if(!proposal.ownerRequirements().equals(new JobRequirements(age,Map.of("fishing",policy.founderExperience())))
                        ||!proposal.workerRequirements().equals(new JobRequirements(age,Map.of("fishing",policy.workerExperience())))||proposal.workerCapacity()!=policy.workerCapacity())throw new BusinessStartupGrants.Declined("Founder terms changed");
                try(var s=connection.prepareStatement("SELECT 1 FROM business_employment WHERE resident_id=? AND ended_step IS NULL UNION ALL SELECT 1 FROM resident_courses WHERE resident_id=? AND status='ACTIVE' LIMIT 1")) {
                    s.setString(1,proposal.founder().toString());s.setString(2,proposal.founder().toString());try(var r=s.executeQuery()){if(r.next())throw new BusinessStartupGrants.Declined("Founder now employed or studying");}
                }
            }else if(!firms.findFounding(existing.orElseThrow().id()).orElseThrow().equals(request.founding()))throw new SQLException("Fishery founding identity reused with changed terms");
            var result=new BusinessStartupGrants(connection).startShoreFisheryRows(request,proposal.opportunity().water(),water.minimumDepth(),shoreline.maximumRise(),shoreline.maximumDistance(),evidence,now,terms.evidenceLifetimeMillis(),fishing,grants);
            connection.commit();connection.setAutoCommit(true);return result;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private BusinessStartupGrants.WorkshopRequest draft(Proposal proposal,ShorelineEvidence evidence) {
        var opportunity=proposal.opportunity();var p=evidence.origin();
        String key=opportunity.town()+":"+opportunity.water().evidence().world()+":"+opportunity.water().evidence().columns()+":"+p;
        UUID site=UUID.nameUUIDFromBytes(("civilizations:first-fishery-site-v1:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID business=UUID.nameUUIDFromBytes(("civilizations:first-fishery-business-v1:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var reason="First habitat-backed fishery for actual funded food reserve/shortage; existing bank only, no property claim; "+policy+"; "+terms;
        var location=new BusinessSite(site,opportunity.town(),opportunity.water().evidence().world(),p.x(),p.y(),p.z(),evidence.capturedMillis(),reason);
        var firm=new Business(business,opportunity.town(),"Shore fishery",proposal.founder(),proposal.founder(),"fishing",site,proposal.workerCapacity(),proposal.foundedStep());
        return new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(firm,location,proposal.ownerRequirements(),proposal.workerRequirements(),terms.startupHundredths(),reason),terms.capacity(),industry.mining().storageSpacePerUnit(),terms.capitalHundredths(),proposal.founder());
    }
    private static Preparation none(String reason){return new Preparation(Optional.empty(),reason);}
}
