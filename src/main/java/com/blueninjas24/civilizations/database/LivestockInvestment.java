package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Trusted, loaded local proof -> ownership, real grant, job and herd in one transaction. No Paper calls. */
public final class LivestockInvestment {
    public record Village(UUID town,UUID world,Position center) {}
    public record Result(boolean opened,UUID business,String explanation) {}
    public record Preparation(Optional<BusinessStartupGrants.WorkshopRequest> request,String explanation) {}
    private final Connection connection;
    private final LivestockAdoptionPolicy policy;
    private final LivestockPolicy livestock;
    private final StartupGrantPolicy grants;
    private final long adulthood;
    public LivestockInvestment(Connection connection,LivestockAdoptionPolicy policy,LivestockPolicy livestock,StartupGrantPolicy grants,long adulthood){
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);
        this.livestock=Objects.requireNonNull(livestock);this.grants=Objects.requireNonNull(grants);
        if(adulthood<1)throw new IllegalArgumentException("Invalid adulthood");this.adulthood=adulthood;
    }
    public List<Village> villages()throws SQLException {
        var result=new ArrayList<Village>();
        try(var s=connection.createStatement();var r=s.executeQuery("""
            SELECT s.id,w.world_id,s.center_x,s.center_y,s.center_z FROM settlements s JOIN settlement_worlds w ON w.settlement_id=s.id
            WHERE EXISTS(SELECT 1 FROM residents r WHERE r.settlement_id=s.id AND r.status='ALIVE') ORDER BY s.id LIMIT 1025
            """)) {
            while(r.next())result.add(new Village(UUID.fromString(r.getString(1)),UUID.fromString(r.getString(2)),new Position(r.getInt(3),r.getInt(4),r.getInt(5))));
        }
        // Do not silently starve settlements beyond a bounded page.
        if(result.size()>1024)throw new SQLException("Livestock village review exceeds 1024; paging required");
        return List.copyOf(result);
    }
    /** Read-only proof/qualification/budget review; acceptance independently revalidates before claims. */
    public Preparation prepare(LivestockOpportunity opportunity,long step,long now,Set<UUID> worlds,BooleanSupplier valid)throws SQLException {
        Objects.requireNonNull(opportunity);Objects.requireNonNull(valid);
        if(step<0||now<0)throw new IllegalArgumentException("Invalid founding time");
        if(!connection.getAutoCommit())throw new SQLException("Livestock preparation requires idle SQL owner");connection.setAutoCommit(false);
        try {var result=prepareRows(opportunity,step,now,worlds==null?Set.of():worlds,valid);connection.commit();return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{connection.setAutoCommit(true);}
    }
    public Result accept(LivestockOpportunity opportunity,long step,long now,Set<UUID> worlds,BooleanSupplier valid)throws SQLException {
        return accept(opportunity,step,now,worlds,valid,()->{});
    }
    public Result accept(LivestockOpportunity opportunity,long step,long now,Set<UUID> worlds,BooleanSupplier valid,ResourceInvestment.StartupAudit audit)throws SQLException {
        Objects.requireNonNull(opportunity);Objects.requireNonNull(valid);
        if(step<0||now<0)throw new IllegalArgumentException("Invalid founding time");
        if(!connection.getAutoCommit())throw new SQLException("Livestock investment requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var result=acceptRows(opportunity,step,now,worlds==null?Set.of():worlds,valid);
            if(!valid.getAsBoolean())throw new BusinessStartupGrants.Declined("Animal evidence or capture changed before startup committed");
            if(result.opened())audit.save();connection.commit();return result;
        }catch(BusinessStartupGrants.Declined declined){connection.rollback();return new Result(false,null,declined.getMessage());}
        catch(SQLException|RuntimeException failure){try{connection.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{connection.setAutoCommit(true);}
    }
    private Preparation prepareRows(LivestockOpportunity o,long step,long now,Set<UUID> worlds,BooleanSupplier valid)throws SQLException {
        if(!policy.enabled()||!livestock.enabled()||!valid.getAsBoolean())return none("Livestock adoption/work disabled or capture untrusted");
        if(!worlds.contains(o.world())||o.capturedMillis()>now||now-o.capturedMillis()>10000||o.observedMillis()<policy.observationMillis())return none("Fresh repeated complete local proof and present world required");
        var known=villages();
        var village=known.stream().filter(v->v.town().equals(o.settlement())&&v.world().equals(o.world())).findFirst();
        if(village.isEmpty())return none("Saved living village/world unavailable");
        if(o.animals().size()>policy.maximumAnimals()||o.animals().stream().filter(LivestockObservation.Animal::adult).count()<policy.minimumAdults())return none("Insufficient adults or animal review limit exceeded");
        for(var animal:o.animals()) {
            if(!LivestockOpportunity.natural(animal)||distance(animal.position(),village.orElseThrow().center())>square(policy.villageRadius())
                    ||Math.abs((long)animal.position().y()-village.orElseThrow().center().y())>policy.villageRadius()
                    ||Math.abs((long)animal.position().x()-o.site().x())>policy.clearRadius()||Math.abs((long)animal.position().z()-o.site().z())>policy.clearRadius()
                    ||Math.abs((long)animal.position().y()-o.site().y())>2)return none("Animal origin, protection flags or local distance ineligible");
            var nearest=known.stream().filter(v->v.world().equals(o.world())).min(Comparator.comparingLong((Village v)->distance(animal.position(),v.center())).thenComparing(v->v.town().toString())).orElseThrow();
            if(!nearest.town().equals(o.settlement()))return none("Another saved living village is closer; no overlapping automatic claim");
            if(new AnimalOwnershipRepository(connection).load(animal.id()).isPresent())return none("Animal already has permanent ownership; no automatic transfer");
            if(!new PenProtectionRepository(connection).atAll(o.world(),animal.position().y(),animal.position().x(),animal.position().z()).isEmpty())return none("Animal is inside a saved protected area");
        }
        try(var s=connection.prepareStatement("""
            SELECT COUNT(*) FROM livestock_herds h JOIN businesses b ON b.id=h.business_id
            WHERE b.settlement_id=? AND h.species=? AND NOT EXISTS(SELECT 1 FROM business_closures c WHERE c.business_id=b.id)
            """)) {
            s.setString(1,o.settlement().toString());s.setString(2,o.species().name());try(var r=s.executeQuery()){r.next();if(r.getLong(1)>=policy.maximumPerSpecies())return none("Village already has its configured open herd allowance for this species");}
        }
        var candidates=new ArrayList<UUID>();var population=new PopulationRepository(connection);UUID founder=null,worker=null;
        try(var s=connection.prepareStatement("SELECT id FROM residents WHERE settlement_id=? AND status='ALIVE' AND age_steps>=? ORDER BY id LIMIT 256")) {
            s.setString(1,o.settlement().toString());s.setLong(2,adulthood);try(var r=s.executeQuery()){while(r.next())candidates.add(UUID.fromString(r.getString(1)));}
        }
        for(var id:candidates){
            var person=population.findResident(id).orElseThrow();var profile=population.findWorkProfile(id).orElseThrow();
            if(founder==null&&WorkEligibility.evaluate(person,profile,requirements(policy.founderFarming())).eligible())founder=id;
            if(worker==null&&WorkEligibility.evaluate(person,profile,requirements(policy.workerFarming())).eligible()&&available(id))worker=id;
        }
        if(founder==null||worker==null)return none("Waiting for a farming-qualified owner and an available adult worker; no free skill or duplicate job");
        var ordered=o.animals().stream().sorted(Comparator.comparing(a->a.id().toString())).toList();
        String key=o.settlement()+":"+o.world()+":"+o.species()+":"+ordered.getFirst().id();
        UUID business=id("civilizations:free-range-business-v1:"+key),site=id("civilizations:free-range-site-v1:"+key);
        // A repeated old observation cannot silently create a replacement for a closed firm.
        if(new BusinessRepository(connection).findBusiness(business).isPresent())return none("This initial animal group already has business history");
        String reason="Repeated loaded natural free-range proof; local clearance radius="+policy.clearRadius()+"; "+policy;
        var p=o.site();var location=new BusinessSite(site,o.settlement(),o.world(),p.x(),p.y(),p.z(),o.capturedMillis(),reason);
        var firm=new Business(business,o.settlement(),o.species().businessName(),founder,founder,"livestock",site,2,step);
        var founding=new BusinessFounding(firm,location,requirements(policy.founderFarming()),requirements(policy.workerFarming()),policy.startupHundredths(),reason);
        var product=o.species().product();
        var request=new BusinessStartupGrants.WorkshopRequest(founding,256,Map.of(new Commodity("grain"),1L,product,1L),policy.capitalHundredths(),worker);
        var budget=new BusinessStartupGrants(connection).previewRows(o.settlement(),"livestock",policy.startupHundredths(),policy.capitalHundredths(),grants);
        if(!budget.eligible())return none(budget.reason());
        return new Preparation(Optional.of(request),"Qualified local owner/worker, unowned natural animals and protected grant ready; nothing reserved");
    }
    private Result acceptRows(LivestockOpportunity o,long step,long now,Set<UUID> worlds,BooleanSupplier valid)throws SQLException {
        var prepared=prepareRows(o,step,now,worlds,valid);
        if(prepared.request().isEmpty())return no(prepared.explanation());
        var request=prepared.request().orElseThrow();var business=request.founding().business().id();
        var ordered=o.animals().stream().sorted(Comparator.comparing(a->a.id().toString())).toList();
        var animals=new TreeMap<UUID,Integer>();var ownership=new AnimalOwnershipRepository(connection);
        for(var animal:ordered){
            ownership.recordRows(new AnimalOwnershipRepository.Claim(animal.id(),o.world(),LivestockAdoption.Ownership.VILLAGE,null,o.settlement(),now,"Automatic natural free-range adoption; business="+business));
            int maturity=animal.adult()?0:(int)Math.max(1,Math.min(livestock.maturityPeriods(),(-Math.max(-24000L,animal.ageTicks())*livestock.maturityPeriods()+23999)/24000));
            animals.put(animal.id(),maturity);
        }
        new BusinessStartupGrants(connection).startLivestockRows(request,o.species(),new Commodity("grain"),policy.herdCapacity(),animals,grants);
        return new Result(true,business,"Real protected grant, owner, employee, animal ownership and empty herd store committed together; feed must be purchased");
    }
    private boolean available(UUID person)throws SQLException {
        try(var s=connection.prepareStatement("SELECT 1 FROM business_employment WHERE resident_id=? AND ended_step IS NULL UNION ALL SELECT 1 FROM resident_courses WHERE resident_id=? AND status='ACTIVE' LIMIT 1")) {
            s.setString(1,person.toString());s.setString(2,person.toString());try(var r=s.executeQuery()){return !r.next();}
        }
    }
    private JobRequirements requirements(long skill){return new JobRequirements(adulthood,skill==0?Map.of():Map.of("farming",skill));}
    private static Preparation none(String reason){return new Preparation(Optional.empty(),reason);}
    private static Result no(String reason){return new Result(false,null,reason);}
    private static UUID id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));}
    private static long square(long n){return n*n;}
    private static long distance(Position a,Position b){long x=(long)a.x()-b.x(),z=(long)a.z()-b.z();if(Math.abs(x)>128||Math.abs(z)>128)return Long.MAX_VALUE;return x*x+z*z;}
}
