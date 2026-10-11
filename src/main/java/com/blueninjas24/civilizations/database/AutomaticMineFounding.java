package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Current observed resources plus actual customers, skills and real grants; no world writes. */
public final class AutomaticMineFounding {
    private final Connection connection;
    private final MineFoundingPolicy policy;
    private final IndustryRuntimePolicy industry;
    private final BusinessOperationPolicy operations;
    private final StartupGrantPolicy grants;
    private final long adulthood,seed;
    private final java.util.function.BooleanSupplier mutationHealthy;
    public AutomaticMineFounding(Connection connection,MineFoundingPolicy policy,IndustryRuntimePolicy industry,
            BusinessOperationPolicy operations,StartupGrantPolicy grants,long adulthood,long seed,java.util.function.BooleanSupplier mutationHealthy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.industry=Objects.requireNonNull(industry);
        this.operations=Objects.requireNonNull(operations);this.grants=Objects.requireNonNull(grants);this.adulthood=adulthood;this.seed=seed;this.mutationHealthy=Objects.requireNonNull(mutationHealthy);
    }
    private String townCursor="";
    public String pulse(long step,long now,Set<UUID> worlds)throws SQLException {
        if(!policy.enabled())return "Automatic mine founding disabled";
        var towns=townPage();if(towns.isEmpty()&&!townCursor.isEmpty()){townCursor="";towns=townPage();}
        String latest="No initialized villages to review";
        for(var town:towns){latest=review(town,step,now,worlds);townCursor=town.toString();}
        return latest;
    }
    private List<UUID> townPage()throws SQLException {
        var result=new ArrayList<UUID>();try(var s=connection.prepareStatement("SELECT settlement_id FROM economy_accounts WHERE settlement_id>? ORDER BY settlement_id LIMIT ?")) {
            s.setString(1,townCursor);s.setInt(2,policy.settlementsPerPulse());try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }return result;
    }
    public String review(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        if(step<0||now<0)throw new IllegalArgumentException("Invalid mine founding observation time");
        if(!policy.enabled())return "Automatic mine founding disabled";
        if(!mutationHealthy.getAsBoolean())return "Mine founding paused: immediate ore mutation trust guard";
        if(!connection.getAutoCommit())throw new SQLException("Mine founding requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            String result=reviewRows(town,step,now,worlds==null?Set.of():Set.copyOf(worlds));
            if(!mutationHealthy.getAsBoolean())throw new BusinessStartupGrants.Declined("Ore mutation trust changed during startup; all assets rolled back");
            connection.commit();connection.setAutoCommit(true);return result;
        }catch(SQLException|RuntimeException failure) {
            try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);throw failure;}
            if(failure instanceof BusinessStartupGrants.Declined)return "Mine founding waits: "+failure.getMessage();
            throw failure;
        }
    }
    private String reviewRows(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        var pressure=new LocalIndustryDemand(connection,industry,operations,mutationHealthy).reviewMining(town,now,policy.surveyLifetimeMillis(),worlds,64);
        if(!pressure.demand().complete())return "Mine founding waits: "+pressure.demand().explanation();
        if(pressure.survey().status()!=MineralOpportunities.Status.CURRENT)return "Mine founding waits: "+pressure.survey().explanation();
        if(pressure.candidates().isEmpty())return "No current unclaimed ore matching actual funded customers";
        Position center;
        try(var s=connection.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Unknown mine settlement");center=new Position(r.getInt(1),r.getInt(2),r.getInt(3));}
        }
        var patches=new ArrayList<MineralObservation>();
        for(var mineral:new TreeSet<>(policy.mineralYield().keySet())) {
            var coordinates=pressure.candidates().stream().filter(e->e.mineral().equals(mineral)).map(MineralOpportunities.Evidence::position).toList();
            patches.addAll(OreClusters.split(new MineralObservation(pressure.survey().world(),pressure.survey().capturedMillis(),true,mineral,coordinates)));
        }
        long period=0;
        try(var s=connection.prepareStatement("SELECT completed_periods FROM economy_accounts WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())period=r.getLong(1);}
        }
        Collections.shuffle(patches,new Random(seed^town.getMostSignificantBits()^town.getLeastSignificantBits()^period));
        int examined=0;
        for(var patch:patches) {
            if(++examined>policy.patchesPerReview())break;
            var mineral=patch.mineral();var origin=patch.ore().getFirst();
            if(!within(center,origin)||occupied(patch.world(),origin))continue;
            if(existingCapacity(town,mineral,pressure.demand().uncovered().getOrDefault(mineral,0L))||!fundedWarehouse(town,mineral))continue;
            var bundle=policy.bundle(mineral,industry.mining());
            if(!industry.mining().storageSpacePerUnit().keySet().containsAll(bundle.keySet()))return "Mine founding waits: storage catalog lacks configured outputs";
            long neededSpace=0;for(var e:bundle.entrySet())neededSpace=Math.addExact(neededSpace,Math.multiplyExact(e.getValue(),industry.mining().storageSpacePerUnit().get(e.getKey())));
            if(neededSpace>industry.mining().storageCapacity())return "Mine founding waits: one extraction bundle cannot fit configured storage";
            var funding=new BusinessStartupGrants(connection).previewRows(town,"mining",policy.startupHundredths(),policy.capitalHundredths(),grants);
            if(!funding.eligible())return "Mine founding waits: "+funding.reason();
            UUID founder=founder(town);if(founder==null)return "Mine founding waits: bounded page has no qualified unemployed founder; no free skills";
            String key=town+":"+patch.world()+":"+mineral.id()+":"+origin.x()+":"+origin.y()+":"+origin.z();
            UUID siteId=id("civilizations:automatic-mine-site-v1:"+key),businessId=id("civilizations:automatic-mine-business-v1:"+key);
            if(new BusinessRepository(connection).findBusiness(businessId).isPresent())continue; // Never reopen or refill a retained firm.
            String explanation="Automatic observed "+mineral.id()+" mine for actual funded local input demand; capture "+patch.observedMillis()+"; provisional resource anchor, no excavation rights; "+policy;
            var site=new BusinessSite(siteId,town,patch.world(),origin.x(),origin.y(),origin.z(),patch.observedMillis(),explanation);
            var business=new Business(businessId,town,mineral.id().equals("coal")?"Coal mine":"Iron mine",founder,founder,"mining",siteId,policy.workerCapacity(),step);
            var owner=new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of("mining",policy.founderExperience()));
            var worker=new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of());
            var request=new MiningStartupRepository.Request(new BusinessFounding(business,site,owner,worker,policy.startupHundredths(),explanation),patch,
                    industry.mining().depositBatchesPerObservedOre(),bundle,industry.mining().storageCapacity(),industry.mining().storageSpacePerUnit(),policy.capitalHundredths(),founder);
            new BusinessStartupGrants(connection).startMineRows(request,grants);
            return "Founded one "+mineral.id()+" mine: real grant, qualified owner/first worker, "+patch.ore().size()+" observed supporting blocks; finite reserves and empty stock; no construction";
        }
        return "No viable new mine in bounded patch page: existing supply capacity, no funded warehouse buyer, or resource anchor outside range/occupied; no assets changed";
    }
    private boolean within(Position center,Position p) {
        long dx=(long)p.x()-center.x(),dz=(long)p.z()-center.z();
        return Math.abs(dx)<=policy.horizontalRange()&&Math.abs(dz)<=policy.horizontalRange()
                &&dx*dx+dz*dz<=(long)policy.horizontalRange()*policy.horizontalRange()&&Math.abs((long)p.y()-center.y())<=policy.verticalRange();
    }
    private boolean occupied(UUID world,Position p)throws SQLException {
        try(var s=connection.prepareStatement("SELECT 1 FROM business_sites WHERE world_id=? AND x=? AND y=? AND z=? UNION ALL SELECT 1 FROM village_beds WHERE world_id=? AND x=? AND y=? AND z=? LIMIT 1")) {
            for(int start:new int[]{1,5}){s.setString(start,world.toString());s.setInt(start+1,p.x());s.setInt(start+2,p.y());s.setInt(start+3,p.z());}
            try(var r=s.executeQuery()){return r.next();}
        }
    }
    private UUID founder(UUID town)throws SQLException {
        var candidates=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
              AND EXISTS(SELECT 1 FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='mining' AND k.experience>=?)
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setLong(2,Math.max(adulthood,policy.minimumAgeSteps()));s.setLong(3,policy.founderExperience());s.setInt(4,policy.foundersPerReview());try(var r=s.executeQuery()){while(r.next())candidates.add(UUID.fromString(r.getString(1)));}
        }
        for(var candidate:candidates)if(new PopulationRepository(connection).findWorkProfile(candidate).orElseThrow().experience().getOrDefault("mining",0L)>=policy.founderExperience())return candidate;
        return null;
    }
    private boolean existingCapacity(UUID town,Commodity mineral,long needed)throws SQLException {
        var firms=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND b.industry='mining' ORDER BY b.id LIMIT 65")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())firms.add(UUID.fromString(r.getString(1)));}
        }
        if(firms.size()>64)throw new BusinessStartupGrants.Declined("Existing mine capacity review budget exceeded");
        var availability=new BusinessOperations(connection,operations);var assets=new IndustryRepository(connection);var people=new PopulationRepository(connection);var businesses=new BusinessRepository(connection);
        long covered=0;
        for(var firm:firms) {
            if(!availability.evaluate(firm).permitted())continue;
            var store=assets.store(firm).orElseThrow();covered=Math.addExact(covered,store.units().getOrDefault(mineral,0L));if(covered>=needed)return true;
            var founding=businesses.findFounding(firm).orElseThrow();int workers=0;
            for(var job:businesses.currentWorkers(firm)) {var resident=people.findResident(job.residentId()).orElseThrow();if(WorkEligibility.evaluate(resident,people.findWorkProfile(resident.id()).orElseThrow(),founding.workerRequirements()).eligible())workers++;}
            if(workers==0)continue;
            var deposits=new ArrayList<UUID>();try(var s=connection.prepareStatement("SELECT id FROM mineral_deposits WHERE site_id=? ORDER BY id LIMIT 2")) {s.setString(1,founding.site().id().toString());try(var r=s.executeQuery()){while(r.next())deposits.add(UUID.fromString(r.getString(1)));}}
            if(deposits.size()!=1)continue;var d=assets.deposit(deposits.getFirst()).orElseThrow();
            if(d.remainingBatches()==0||!d.bundle().containsKey(mineral)||!assets.sourceHistoryTrusted(d.id()))continue;
            long periods=industry.supply().inputPeriods();
            var plan=MineProduction.calculate(d.remainingBatches(),Math.multiplyExact(industry.mining().siteBatchCapacity(),periods),workers,
                    Math.multiplyExact(industry.mining().batchesPerWorker(),periods),Math.multiplyExact(industry.mining().siteBatchCapacity(),periods),store.freeSpace(),d.bundle(),store.spacePerUnit());
            covered=Math.addExact(covered,plan.outputs().getOrDefault(mineral,0L));if(covered>=needed)return true;
        }
        return false;
    }
    private boolean fundedWarehouse(UUID town,Commodity mineral)throws SQLException {
        if(!industry.warehouseSupply().enabled())return false;
        Long price=industry.purchasingSupply().unitHundredths().get(mineral);if(price==null)return false;
        var ids=new ArrayList<UUID>();try(var s=connection.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND b.industry='warehouse' ORDER BY b.id LIMIT ?")) {
            s.setString(1,town.toString());s.setInt(2,industry.supply().warehousesPerReview()+1);try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
        }
        if(ids.size()>industry.supply().warehousesPerReview())throw new BusinessStartupGrants.Declined("Warehouse buyer review budget exceeded");
        var firms=new BusinessRepository(connection);var assets=new IndustryRepository(connection);var people=new PopulationRepository(connection);var available=new BusinessOperations(connection,operations);
        for(var id:ids) {
            if(!available.evaluate(id).permitted())continue;
            var store=assets.store(id).orElseThrow();Long weight=store.spacePerUnit().get(mineral);
            if(weight==null||store.hundredths()<price||store.freeSpace()<weight)continue;
            var requirements=firms.findFounding(id).orElseThrow().workerRequirements();
            for(var job:firms.currentWorkers(id)){var person=people.findResident(job.residentId()).orElseThrow();if(WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),requirements).eligible())return true;}
        }
        return false;
    }
    private static UUID id(String key){return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));}
}
