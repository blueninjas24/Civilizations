package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Current observed resources plus actual customers, skills and real grants; no world writes. */
public final class AutomaticQuarryFounding {
    private final Connection connection;
    private final QuarryFoundingPolicy policy;
    private final IndustryRuntimePolicy industry;
    private final QuarryPolicy resources;
    private VillageSupplyPolicy publicSupply=VillageSupplyPolicy.disabled();
    private StudyPolicy study=StudyPolicy.disabled();
    public AutomaticQuarryFounding withPublicSupplies(VillageSupplyPolicy supplies,StudyPolicy study){this.publicSupply=Objects.requireNonNull(supplies);this.study=Objects.requireNonNull(study);return this;}
    private final BusinessOperationPolicy operations;
    private final StartupGrantPolicy grants;
    private final long adulthood,seed;
    private final java.util.function.BooleanSupplier mutationHealthy;
    public AutomaticQuarryFounding(Connection connection,QuarryFoundingPolicy policy,QuarryPolicy resources,IndustryRuntimePolicy industry,
            BusinessOperationPolicy operations,StartupGrantPolicy grants,long adulthood,long seed,java.util.function.BooleanSupplier mutationHealthy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.resources=Objects.requireNonNull(resources);this.industry=Objects.requireNonNull(industry);
        this.operations=Objects.requireNonNull(operations);this.grants=Objects.requireNonNull(grants);this.adulthood=adulthood;this.seed=seed;this.mutationHealthy=Objects.requireNonNull(mutationHealthy);
    }
    public record Opportunity(UUID town,MineralObservation patch) {
        public Opportunity {Objects.requireNonNull(town);Objects.requireNonNull(patch);if(patch.ore().isEmpty())throw new IllegalArgumentException("Observed patch required");}
        public Position origin(){return patch.ore().getFirst();}
        public String key(){var p=origin();return town+":"+patch.world()+":"+patch.mineral().id()+":"+p.x()+":"+p.y()+":"+p.z();}
    }
    record Selection(Opportunity opportunity,String explanation) {}
    @FunctionalInterface public interface FounderTraining {String review(Opportunity opportunity,long now)throws SQLException;}
    private FounderTraining training;
    public void configureTraining(FounderTraining training){this.training=Objects.requireNonNull(training);}
    private String townCursor="";
    public String pulse(long step,long now,Set<UUID> worlds)throws SQLException {
        if(!policy.enabled()||!resources.enabled()||!industry.enabled())return "Automatic quarry/sandpit founding disabled";
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
        return review(town,step,now,worlds,()->{});
    }
    String review(UUID town,long step,long now,Set<UUID> worlds,ResourceInvestment.StartupAudit audit)throws SQLException {
        if(step<0||now<0)throw new IllegalArgumentException("Invalid mine founding observation time");
        if(!policy.enabled()||!resources.enabled()||!industry.enabled())return "Automatic quarry/sandpit founding disabled";
        if(!mutationHealthy.getAsBoolean())return "Quarry founding paused: immediate ore mutation trust guard";
        if(!connection.getAutoCommit())throw new SQLException("Quarry founding requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            String result=reviewRows(town,step,now,worlds==null?Set.of():Set.copyOf(worlds));
            if(!mutationHealthy.getAsBoolean())throw new BusinessStartupGrants.Declined("Ore mutation trust changed during startup; all assets rolled back");
            audit.save();
            connection.commit();connection.setAutoCommit(true);return result;
        }catch(SQLException|RuntimeException failure) {
            try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);throw failure;}
            if(failure instanceof BusinessStartupGrants.Declined)return "Quarry founding waits: "+failure.getMessage();
            throw failure;
        }
    }
    private String reviewRows(UUID town,long step,long now,Set<UUID> worlds)throws SQLException {
        var choice=opportunityRows(town,now,worlds,null);if(choice.opportunity()==null)return choice.explanation();
        var opportunity=choice.opportunity();var patch=opportunity.patch();var mineral=patch.mineral();var origin=opportunity.origin();
        UUID founder=founder(town);
        if(founder==null)return "Quarry founding waits: bounded page has no qualified unemployed founder; no free skills"
                +(training==null?"":"; "+training.review(opportunity,now));
        UUID siteId=id("civilizations:automatic-quarry-site-v1:"+opportunity.key()),businessId=id("civilizations:automatic-quarry-business-v1:"+opportunity.key());
        String explanation="Automatic observed "+mineral.id()+" extraction business for actual funded local input demand; capture "+patch.observedMillis()+"; provisional resource anchor, no excavation rights; "+policy;
        var site=new BusinessSite(siteId,town,patch.world(),origin.x(),origin.y(),origin.z(),patch.observedMillis(),explanation);
        var business=new Business(businessId,town,mineral.equals(Commodity.STONE)?"Stone quarry":"Sand pit",founder,founder,mineral.equals(Commodity.STONE)?"quarry":"sandpit",siteId,policy.workerCapacity(),step);
        var owner=new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of("mining",policy.founderExperience()));
        var worker=new JobRequirements(Math.max(adulthood,policy.minimumAgeSteps()),Map.of());
        var request=new MiningStartupRepository.Request(new BusinessFounding(business,site,owner,worker,policy.startupHundredths(),explanation),patch,
                resources.terms(mineral).reserveBatchesPerBlock(),resources.bundle(mineral),industry.mining().storageCapacity(),industry.mining().storageSpacePerUnit(),policy.capitalHundredths(),founder);
        new BusinessStartupGrants(connection).startMineRows(request,grants);
        return "Founded one "+(mineral.equals(Commodity.STONE)?"stone quarry":"sandpit")+": real grant, qualified owner/first worker, "+patch.ore().size()+" observed supporting blocks; finite reserves and empty stock; no construction";
    }
    Selection opportunityRows(UUID town,long now,Set<UUID> worlds,Opportunity only)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Mine opportunity requires owner transaction");
        if(!policy.enabled()||!resources.enabled()||!industry.enabled()||!mutationHealthy.getAsBoolean())return new Selection(null,"Quarry founding disabled or mutation trust unavailable");
        var pressure=new LocalIndustryDemand(connection,industry,operations,mutationHealthy).review(town,worlds,64);
        if(!pressure.complete())return new Selection(null,"Quarry founding waits: "+pressure.explanation());
        var demand=new TreeMap<>(pressure.uncovered());
        var direct=new VillageSupplyPurchases(connection,publicSupply,study,operations).demandAfterSpending(town,worlds,Math.addExact(policy.startupHundredths(),policy.capitalHundredths()));
        direct.fundedUnits().forEach((good,units)->demand.merge(good,units,Math::addExact));
        var survey=MineralOpportunities.quarries(connection).review(town,now,policy.surveyLifetimeMillis(),worlds);
        if(survey.status()!=MineralOpportunities.Status.CURRENT)return new Selection(null,"Quarry founding waits: "+survey.explanation());
        if(survey.candidates().isEmpty())return new Selection(null,"No current unclaimed stone/sand evidence");
        Position center;
        try(var s=connection.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Unknown mine settlement");center=new Position(r.getInt(1),r.getInt(2),r.getInt(3));}
        }
        var patches=new ArrayList<MineralObservation>();
        for(var mineral:new TreeSet<>(Set.of(Commodity.STONE,Commodity.SAND))) {
            var chunks=new TreeMap<String,List<com.blueninjas24.civilizations.settlement.VillageObservation.Position>>();
            for(var evidence:survey.candidates())if(evidence.mineral().equals(mineral)) {
                var p=evidence.position();String chunk=Math.floorDiv(p.x(),16)+":"+Math.floorDiv(p.z(),16);
                chunks.computeIfAbsent(chunk,key->new ArrayList<>()).add(p);
            }
            for(var coordinates:chunks.values())patches.addAll(OreClusters.split(new MineralObservation(survey.world(),survey.capturedMillis(),true,mineral,coordinates)));
        }
        long period=0;
        try(var s=connection.prepareStatement("SELECT completed_periods FROM economy_accounts WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())period=r.getLong(1);}
        }
        Collections.shuffle(patches,new Random(seed^town.getMostSignificantBits()^town.getLeastSignificantBits()^period));
        int examined=0;
        for(var patch:patches) {
            if(only!=null&&(!patch.world().equals(only.patch().world())||!patch.mineral().equals(only.patch().mineral())||!patch.ore().getFirst().equals(only.origin())))continue;
            if(++examined>policy.patchesPerReview())break;
            var mineral=patch.mineral();var origin=patch.ore().getFirst();
            if(!within(center,origin)||occupied(patch.world(),origin))continue;
            long needed=demand.getOrDefault(mineral,0L);
            if(needed==0||existingCapacity(town,mineral,needed)||(direct.fundedUnits().getOrDefault(mineral,0L)==0&&!fundedWarehouse(town,mineral)))continue;
            var bundle=resources.bundle(mineral);
            if(!industry.mining().storageSpacePerUnit().keySet().containsAll(bundle.keySet()))return new Selection(null,"Quarry founding waits: storage catalog lacks configured outputs");
            long neededSpace=0;for(var e:bundle.entrySet())neededSpace=Math.addExact(neededSpace,Math.multiplyExact(e.getValue(),industry.mining().storageSpacePerUnit().get(e.getKey())));
            if(neededSpace>industry.mining().storageCapacity())return new Selection(null,"Quarry founding waits: one extraction bundle cannot fit configured storage");
            var funding=new BusinessStartupGrants(connection).previewRows(town,mineral.equals(Commodity.STONE)?"quarry":"sandpit",policy.startupHundredths(),policy.capitalHundredths(),grants);
            if(!funding.eligible())return new Selection(null,"Quarry founding waits: "+funding.reason());
            var opportunity=new Opportunity(town,patch);
            if(new BusinessRepository(connection).findBusiness(id("civilizations:automatic-quarry-business-v1:"+opportunity.key())).isPresent())continue;
            return new Selection(opportunity,"Current fundable quarry/pit opportunity with real customers");
        }
        return new Selection(null,"No viable new quarry/pit in bounded patch page: existing supply capacity, no funded warehouse buyer, or resource anchor outside range/occupied; no assets changed");
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
    UUID founder(UUID town)throws SQLException {
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
    long investmentCost(){return Math.addExact(policy.startupHundredths(),policy.capitalHundredths());}
    private boolean existingCapacity(UUID town,Commodity mineral,long needed)throws SQLException {
        var firms=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id WHERE b.settlement_id=? AND b.industry IN('mining','quarry','sandpit') ORDER BY b.id LIMIT 65")) {
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
            String kind=founding.business().industry();
            var quarry=kind.equals("mining")?null:resources.terms(kind.equals("quarry")?Commodity.STONE:Commodity.SAND);
            long capacity=quarry==null?industry.mining().siteBatchCapacity():quarry.siteCapacity();
            long rate=quarry==null?industry.mining().batchesPerWorker():quarry.batchesPerWorker();
            var plan=MineProduction.calculate(d.remainingBatches(),Math.multiplyExact(capacity,periods),workers,
                    Math.multiplyExact(rate,periods),Math.multiplyExact(capacity,periods),store.freeSpace(),d.bundle(),store.spacePerUnit());
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
