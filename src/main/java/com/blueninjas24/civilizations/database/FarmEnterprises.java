package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.FarmObservation;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Reliable village fields, actual capital and available qualified residents. No block edits. */
public final class FarmEnterprises {
    private final Connection connection;
    private final BusinessRepository businesses;
    private final PopulationRepository population;
    private final FarmBusinessPolicy policy;
    private final long adulthood;
    private final BusinessOperationPolicy operations;
    public FarmEnterprises(Connection connection,FarmBusinessPolicy policy,long adulthood) throws SQLException {
        this(connection,policy,adulthood,BusinessOperationPolicy.disabled());
    }
    public FarmEnterprises(Connection connection,FarmBusinessPolicy policy,long adulthood,BusinessOperationPolicy operations) throws SQLException {
        this.connection=connection;this.policy=policy;this.adulthood=adulthood;
        this.operations=Objects.requireNonNull(operations);
        businesses=new BusinessRepository(connection);population=new PopulationRepository(connection);
    }
    public static UUID businessId(UUID town){return id("civilizations:field-enterprise-v1:"+town);}
    private static UUID id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));}
    public String prepare(FarmObservation farm,int cropsPerWorker) throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Farm planning requires idle SQL owner");
        if(!policy.enabled())return "Automatic farm businesses disabled; legacy field accounting";
        if(farm==null || !farm.complete())return "Farm business blocked: complete crop evidence required";
        long slots=farm.plantedCrops()/cropsPerWorker;
        if(slots==0)return "Farm business blocked: insufficient fields for one worker";
        long month=0;
        try(var s=connection.createStatement();var r=s.executeQuery("SELECT completed_steps FROM demographic_clock WHERE id=1")){if(r.next())month=r.getLong(1);}
        var saved=businesses.findFounding(businessId(farm.settlement()));
        if(saved.isEmpty()) {
            if(farm.representativeCrop()==null)return "Farm business blocked: crop position required; legacy counts cannot locate a site";
            var requirements=new JobRequirements(Math.max(adulthood,policy.minimumAgeMonths()),Map.of("farming",policy.minimumFarmingExperience()));
            var candidates=candidates(farm.settlement(),requirements);
            if(candidates.isEmpty())return "Farm business blocked: no available qualified adult founder";
            var treasury=new TreasuryRepository(connection).load(farm.settlement());
            if(policy.startupHundredths()>0 && (treasury.isEmpty() || treasury.orElseThrow().hundredths()<policy.startupHundredths()))
                return "Farm business blocked: insufficient actual startup funds";
            var crop=farm.representativeCrop();
            try(var s=connection.prepareStatement("SELECT 1 FROM business_sites WHERE world_id=? AND x=? AND y=? AND z=?")) {
                s.setString(1,farm.world().toString());s.setInt(2,crop.x());s.setInt(3,crop.y());s.setInt(4,crop.z());
                try(var r=s.executeQuery()){if(r.next())return "Farm business blocked: site occupied by another business";}
            }
            var site=new BusinessSite(id("civilizations:field-site-v1:"+farm.settlement()),farm.settlement(),farm.world(),crop.x(),crop.y(),crop.z(),farm.observedMillis(),
                    "Complete planted fields; representative crop; no ownership/block rights");
            var founder=candidates.getFirst();
            var business=new Business(businessId(farm.settlement()),farm.settlement(),"Village Farm",founder,founder,"farming",site.id(),policy.workerCapacity(),month);
            saved=Optional.of(businesses.found(new BusinessFounding(business,site,requirements,requirements,policy.startupHundredths(),
                    "Prototype field enterprise: complete crops, qualified founder and actual startup capital")));
        }
        var founding=saved.orElseThrow();var business=businesses.findBusiness(founding.business().id()).orElseThrow();
        if(BusinessClosure.blocked(connection,business.id()))return "Farm business closed or unsold deadline expired; field/site/assets/history retained";
        if(!business.industry().equals("farming") || !founding.site().worldId().equals(farm.world()))
            throw new SQLException("Automatic farm identity conflicts with saved industry/world");
        businesses.reconcileBusiness(business.id(),month);
        if(operations.enabled()) {
            var availability=new BusinessOperations(connection,operations).evaluate(business.id());
            if(!availability.permitted())return "Farm business: "+availability.explanation();
            var owner=population.findResident(business.ownerId()).orElseThrow();
            if(owner.status()==Resident.Status.DECEASED)return "Farm business: "+businesses.currentWorkers(business.id()).size()+" existing workers; "+availability.explanation();
        }
        int target=(int)Math.min(slots,business.workerCapacity());
        int current=businesses.currentWorkers(business.id()).size();
        for(var candidate:candidates(farm.settlement(),founding.workerRequirements())) {
            if(current>=target)break;
            long episodes;
            try(var s=connection.prepareStatement("SELECT COUNT(*) FROM business_employment WHERE business_id=? AND resident_id=?")) {
                s.setString(1,business.id().toString());s.setString(2,candidate.toString());try(var r=s.executeQuery()){episodes=r.getLong(1);}
            }
            businesses.hire(id("civilizations:field-hire-v1:"+business.id()+":"+candidate+":"+episodes),business.id(),candidate,month);current++;
        }
        return "Farm business: "+current+" workers; observed slots "+slots+"; capacity "+business.workerCapacity()+"; startup charged once";
    }
    public long workers(UUID town) throws SQLException {
        var id=businessId(town);
        if(BusinessClosure.blocked(connection,id))return 0;
        if(operations.enabled()&&(businesses.findBusiness(id).isEmpty()||!new BusinessOperations(connection,operations).evaluate(id).permitted()))return 0;
        return businesses.currentWorkers(id).size();
    }
    private List<UUID> candidates(UUID town,JobRequirements requirements) throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
                SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
                AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
                AND COALESCE((SELECT experience FROM resident_skills WHERE resident_id=r.id AND skill='farming'),0)>=?
                ORDER BY r.id LIMIT 64
                """)) {
            s.setString(1,town.toString());s.setLong(2,requirements.minimumAgeSteps());s.setLong(3,requirements.minimumExperience().getOrDefault("farming",0L));
            try(var r=s.executeQuery()){while(r.next()) {
                var candidate=UUID.fromString(r.getString(1));
                if(WorkEligibility.evaluate(population.findResident(candidate).orElseThrow(),population.findWorkProfile(candidate).orElseThrow(),requirements).eligible())result.add(candidate);
            }}
        }
        return List.copyOf(result);
    }
}
