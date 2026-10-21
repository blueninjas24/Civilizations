package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import java.sql.*;
import java.util.*;

/** Explicit household -> frozen home proposal identity. No land reservation, purchase, labor or world write. */
public final class HousingProjects {
    public record Proposal(UUID draft,UUID household,long createdMillis){
        public Proposal{Objects.requireNonNull(draft);Objects.requireNonNull(household);if(createdMillis<0)throw new IllegalArgumentException("Negative proposal time");}
    }
    public record Binding(Proposal proposal,UUID village,UUID world,String planHash,int residents,int previousCapacity,
                          UUID cancellation,Long cancelledMillis){public boolean active(){return cancelledMillis==null;}}
    private final Connection c;
    public HousingProjects(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException{
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE housing_project_bindings (
                  draft_id TEXT PRIMARY KEY NOT NULL REFERENCES construction_drafts(id),
                  household_id TEXT NOT NULL REFERENCES households(id),settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,
                  plan_hash TEXT NOT NULL CHECK(length(plan_hash)=64),
                  residents INTEGER NOT NULL CHECK(typeof(residents)='integer' AND residents BETWEEN 1 AND 1024),
                  previous_capacity INTEGER NOT NULL CHECK(typeof(previous_capacity)='integer' AND previous_capacity>=0 AND previous_capacity<residents),
                  created_ms INTEGER NOT NULL CHECK(typeof(created_ms)='integer' AND created_ms>=0),
                  cancel_operation TEXT UNIQUE,cancelled_ms INTEGER,
                  FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                  CHECK((cancel_operation IS NULL AND cancelled_ms IS NULL)
                    OR (cancel_operation IS NOT NULL AND typeof(cancelled_ms)='integer' AND cancelled_ms>=created_ms))
                )
                """);
            s.execute("CREATE UNIQUE INDEX one_active_home_project_per_household ON housing_project_bindings(household_id) WHERE cancelled_ms IS NULL");
            s.execute("CREATE INDEX housing_projects_by_world ON housing_project_bindings(settlement_id,world_id,household_id)");
        }
    }
    public Optional<Binding> find(UUID draft)throws SQLException{
        try(var s=c.prepareStatement("SELECT * FROM housing_project_bindings WHERE draft_id=?")){
            s.setString(1,draft.toString());try(var r=s.executeQuery()){
                if(!r.next())return Optional.empty();String cancellation=r.getString("cancel_operation");
                Long cancelled=cancellation==null?null:r.getLong("cancelled_ms");
                return Optional.of(new Binding(new Proposal(UUID.fromString(r.getString("draft_id")),UUID.fromString(r.getString("household_id")),r.getLong("created_ms")),
                        UUID.fromString(r.getString("settlement_id")),UUID.fromString(r.getString("world_id")),r.getString("plan_hash"),r.getInt("residents"),r.getInt("previous_capacity"),
                        cancellation==null?null:UUID.fromString(cancellation),cancelled));
            }
        }
    }
    public Set<UUID> activeHouseholds(UUID village,UUID world)throws SQLException{
        var ids=new TreeSet<UUID>();
        try(var s=c.prepareStatement("SELECT b.household_id FROM housing_project_bindings b JOIN households h ON h.id=b.household_id WHERE h.settlement_id=? AND b.world_id=? AND b.cancelled_ms IS NULL ORDER BY b.household_id LIMIT 1025")){
            s.setString(1,village.toString());s.setString(2,world.toString());try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
        }
        if(ids.size()>1024)throw new BuildingRegistry.Declined("Housing project budget exceeded");return Set.copyOf(ids);
    }
    /** Recheck unstarted beneficiary need in the same transaction as new funding, effort or first intent. */
    Optional<String> pauseReasonRows(ConstructionDraft draft)throws SQLException{
        if(c.getAutoCommit())throw new SQLException("Housing need review requires the SQL owner's transaction");
        var saved=find(draft.id());if(saved.isEmpty())return Optional.empty(); // Historical unbound projects retain existing checks.
        var binding=saved.orElseThrow();
        if(!binding.active())return Optional.of("Household proposal was cancelled");
        // A durable native intent may already have changed the world after a lost acknowledgement.
        // Preserve its existing recovery/completion path rather than strand a partial structure.
        if(hasRows("construction_block_journal",draft.id()))return Optional.empty();
        try(var s=c.prepareStatement("SELECT settlement_id FROM households WHERE id=?")){
            s.setString(1,binding.proposal().household().toString());try(var r=s.executeQuery()){
                if(!r.next()||!draft.settlement().toString().equals(r.getString(1)))return Optional.of("Beneficiary household moved; review the unused home proposal");
            }
        }
        int living;
        try(var s=c.prepareStatement("SELECT count(*) FROM residents WHERE household_id=? AND settlement_id=? AND status='ALIVE'")){
            s.setString(1,binding.proposal().household().toString());s.setString(2,draft.settlement().toString());try(var r=s.executeQuery()){living=r.getInt(1);}
        }
        Integer capacity=HousingCapacity.read(c,draft.settlement()).get(binding.proposal().household());
        if(living==0)return Optional.of("Beneficiary household has no living members; unused home retained for review");
        if(capacity==null)return Optional.of("Beneficiary housing capacity is unknown; unstarted home paused");
        if(capacity>=living)return Optional.of("Beneficiary now has sufficient housing; unstarted home paused");
        if(living>HomePlanCapacity.beds(draft.template()))return Optional.of("Frozen plan no longer fits the whole household; unstarted home paused");
        return Optional.empty();
    }
    public Binding bind(Proposal proposal,DwellingOccupancy.Context context,HousingProjectPlanner planner,long seed,long period)throws SQLException{
        return transaction(()->bindRows(proposal,context,planner,seed,period));
    }
    Binding bindRows(Proposal proposal,DwellingOccupancy.Context context,HousingProjectPlanner planner,long seed,long period)throws SQLException{
        if(c.getAutoCommit())throw new SQLException("Home binding requires the SQL owner's transaction");
        if(!context.present()||context.now()<proposal.createdMillis())throw new BuildingRegistry.Declined("Present world and current proposal time required");
        var old=find(proposal.draft());
        if(old.isPresent()){
            if(!old.get().proposal().equals(proposal)||!old.get().world().equals(context.world()))throw new BuildingRegistry.Declined("Home project identity or household changed");
            return old.get(); // Historical binding only: cancelled projects never reactivate.
        }
        var draft=new ConstructionDraftRepository(c).find(proposal.draft()).orElseThrow(()->new BuildingRegistry.Declined("Unknown frozen home proposal"));
        if(!draft.world().equals(context.world())||draft.template().purpose()!=ConstructionTemplate.Purpose.HOME||proposal.createdMillis()<draft.createdMillis())
            throw new BuildingRegistry.Declined("Home proposal world, purpose or time differs");
        requireDry(draft.id());
        try(var s=c.createStatement();var r=s.executeQuery("SELECT completed_steps FROM demographic_clock WHERE id=1")){
            if(!r.next()||r.getLong(1)!=period)throw new BuildingRegistry.Declined("Home proposal requires current saved calendar month");
        }
        var candidates=new HousingPlanning(c).review(draft.settlement(),context,List.of(draft.template()),planner,seed,period);
        var recommendation=candidates.plan().recommendations().stream().filter(r->r.household().equals(proposal.household())).findFirst()
                .orElseThrow(()->new BuildingRegistry.Declined("Household housing need is unknown or belongs elsewhere"));
        if(recommendation.action()!=HousingProjectPlanner.Action.PROPOSE||!draft.template().fingerprint().equals(recommendation.fingerprint()))
            throw new BuildingRegistry.Declined("Home proposal waits: "+recommendation.explanation());
        int residents;
        try(var s=c.prepareStatement("SELECT count(*) FROM residents WHERE household_id=? AND settlement_id=? AND status='ALIVE'")){
            s.setString(1,proposal.household().toString());s.setString(2,draft.settlement().toString());try(var r=s.executeQuery()){residents=r.getInt(1);}
        }
        Integer capacity=HousingCapacity.read(c,draft.settlement()).get(proposal.household());
        if(capacity==null||capacity>=residents||residents>HomePlanCapacity.beds(draft.template()))throw new BuildingRegistry.Declined("Whole household no longer fits or needs this home");
        try(var s=c.prepareStatement("INSERT INTO housing_project_bindings VALUES(?,?,?,?,?,?,?,?,NULL,NULL)")){
            s.setString(1,proposal.draft().toString());s.setString(2,proposal.household().toString());s.setString(3,draft.settlement().toString());s.setString(4,draft.world().toString());
            s.setString(5,draft.template().fingerprint());s.setInt(6,residents);s.setInt(7,capacity);s.setLong(8,proposal.createdMillis());s.executeUpdate();
        }
        return find(proposal.draft()).orElseThrow();
    }
    /** Only unused, released projects may lose their household reservation; assets/history are retained. */
    public Binding cancel(UUID operation,UUID draft,long atMillis,DwellingOccupancy.Context context)throws SQLException{
        Objects.requireNonNull(operation);
        return transaction(()->{
            var binding=find(draft).orElseThrow(()->new BuildingRegistry.Declined("Unknown household project"));
            if(!context.present()||!binding.world().equals(context.world())||atMillis<binding.proposal().createdMillis()||atMillis>context.now())
                throw new BuildingRegistry.Declined("Home cancellation world/time changed");
            if(!binding.active()){
                if(!operation.equals(binding.cancellation())||!Objects.equals(atMillis,binding.cancelledMillis()))throw new BuildingRegistry.Declined("Home project already cancelled under different terms");
                return binding;
            }
            var site=new ConstructionSiteReservations(c).find(draft);
            if(site.isPresent()&&site.get().active())throw new BuildingRegistry.Declined("Release the unused site before cancelling its household proposal");
            for(var table:List.of("construction_block_journal","construction_supply_orders"))if(hasRows(table,draft))
                throw new BuildingRegistry.Declined("Started or paid-supply home needs reviewed recovery; household reservation retained");
            try(var s=c.prepareStatement("SELECT operation_id FROM construction_material_reservations WHERE draft_id=?")){
                s.setString(1,draft.toString());try(var r=s.executeQuery()){while(r.next())if(!new ConstructionMaterialReturns(c).returned(UUID.fromString(r.getString(1))))
                    throw new BuildingRegistry.Declined("Return all unused materials before cancelling the home proposal");}
            }
            try(var s=c.prepareStatement("UPDATE housing_project_bindings SET cancel_operation=?,cancelled_ms=? WHERE draft_id=? AND cancelled_ms IS NULL")){
                s.setString(1,operation.toString());s.setLong(2,atMillis);s.setString(3,draft.toString());if(s.executeUpdate()!=1)throw new SQLException("Housing project changed");
            }
            return find(draft).orElseThrow();
        });
    }
    private void requireDry(UUID draft)throws SQLException{
        for(var table:List.of("construction_site_reservations","construction_block_journal","construction_material_reservations","construction_supply_orders"))
            if(hasRows(table,draft))throw new BuildingRegistry.Declined("Bind the household before reserving, funding or starting its new proposal");
    }
    private boolean hasRows(String table,UUID draft)throws SQLException{
        try(var s=c.prepareStatement("SELECT 1 FROM "+table+" WHERE draft_id=? LIMIT 1")){
            s.setString(1,draft.toString());try(var r=s.executeQuery()){return r.next();}}
    }
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException{
        if(!c.getAutoCommit())throw new SQLException("Household project operation requires an idle SQL owner");c.setAutoCommit(false);
        try{T result=work.run();c.commit();c.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
