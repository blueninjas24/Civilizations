package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Transactional formation for already unassigned adults; never guesses kinship or splits saved homes. */
public final class HouseholdFormation {
    private final Connection c;
    public HouseholdFormation(Connection c){this.c=Objects.requireNonNull(c);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE household_formations (
                  household_id TEXT PRIMARY KEY NOT NULL REFERENCES households(id),
                  settlement_id TEXT NOT NULL,world_id TEXT NOT NULL,
                  founder_id TEXT NOT NULL REFERENCES residents(id),partner_id TEXT REFERENCES residents(id),
                  step INTEGER NOT NULL CHECK(typeof(step)='integer' AND step>=0),
                  policy TEXT NOT NULL,created_ms INTEGER NOT NULL CHECK(typeof(created_ms)='integer' AND created_ms>=0),
                  FOREIGN KEY(settlement_id,world_id) REFERENCES settlement_worlds(settlement_id,world_id),
                  CHECK(partner_id IS NULL OR partner_id<>founder_id)
                )
                """);
            s.execute("CREATE INDEX household_formation_period ON household_formations(settlement_id,step)");
            s.execute("""
                CREATE TABLE household_formation_members (
                  resident_id TEXT PRIMARY KEY NOT NULL REFERENCES residents(id),
                  household_id TEXT NOT NULL REFERENCES household_formations(household_id)
                )
                """);
        }
    }
    public int form(UUID village,UUID world,long adulthood,long step,long now,HouseholdFormationPolicy policy)throws SQLException {
        if(adulthood<0||step<0||now<0)throw new IllegalArgumentException("Invalid household formation clock/age");
        if(!policy.enabled())return 0;
        if(!c.getAutoCommit())throw new SQLException("Household formation requires the SQL owner outside another transaction");
        c.setAutoCommit(false);
        try{
            try(var s=c.prepareStatement("SELECT 1 FROM settlement_worlds WHERE settlement_id=? AND world_id=?")){
                s.setString(1,village.toString());s.setString(2,world.toString());try(var r=s.executeQuery()){
                    if(!r.next())throw new BuildingRegistry.Declined("Household formation world does not match village");}
            }
            int used;
            try(var s=c.createStatement();var r=s.executeQuery("SELECT completed_steps FROM demographic_clock WHERE id=1")){
                if(!r.next()||r.getLong(1)!=step)throw new BuildingRegistry.Declined("Household formation requires the current saved calendar month");
            }
            try(var s=c.prepareStatement("SELECT count(*) FROM household_formations WHERE settlement_id=? AND step=?")){
                s.setString(1,village.toString());s.setLong(2,step);try(var r=s.executeQuery()){used=r.getInt(1);}
            }
            if(used>=policy.maximumPerMonth()){c.commit();c.setAutoCommit(true);return 0;}
            var ids=new ArrayList<UUID>();
            try(var s=c.prepareStatement("SELECT id FROM residents WHERE settlement_id=? AND status='ALIVE' AND household_id IS NULL AND age_steps>=? ORDER BY id LIMIT ?")){
                s.setString(1,village.toString());s.setLong(2,adulthood);s.setInt(3,policy.maximumCandidates());try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
            }
            var people=new PopulationRepository(c);int formed=0;
            for(var id:ids){
                if(used+formed>=policy.maximumPerMonth())break;
                var founder=people.findResident(id).orElseThrow();
                if(founder.householdId()!=null||recorded(id)||hasUnassignedChild(id,adulthood))continue;
                var partners=new ArrayList<Resident>();
                for(var tie:people.findFamilyTies(id))if(tie.type()==FamilyTie.Type.PARTNER){
                    UUID partner=tie.first().equals(id)?tie.second():tie.first();var person=people.findResident(partner).orElseThrow();
                    if(person.status()==Resident.Status.ALIVE)partners.add(person);
                }
                if(partners.size()>1)continue;
                Resident partner=partners.isEmpty()?null:partners.getFirst();
                if(partner!=null&&(partner.householdId()!=null||!partner.settlementId().equals(village)||partner.ageSteps()<adulthood
                        ||recorded(partner.id())||hasUnassignedChild(partner.id(),adulthood)||!onlyPartner(people,partner.id(),id)))continue;
                var members=new ArrayList<>(List.of(id));if(partner!=null)members.add(partner.id());members.sort(Comparator.comparing(UUID::toString));
                UUID household=UUID.nameUUIDFromBytes(("civilizations:formed-household:1:"+village+":"+members).getBytes(StandardCharsets.UTF_8));
                people.insertHousehold(new Household(household,village,"New household "+household.toString().substring(0,8)));
                try(var s=c.prepareStatement("INSERT INTO household_formations VALUES(?,?,?,?,?,?,?,?)")){
                    s.setString(1,household.toString());s.setString(2,village.toString());s.setString(3,world.toString());s.setString(4,id.toString());
                    if(partner==null)s.setNull(5,Types.VARCHAR);else s.setString(5,partner.id().toString());
                    s.setLong(6,step);s.setString(7,policy.toString());s.setLong(8,now);s.executeUpdate();
                }
                for(var member:members){
                    try(var s=c.prepareStatement("UPDATE residents SET household_id=? WHERE id=? AND settlement_id=? AND status='ALIVE' AND household_id IS NULL AND age_steps>=?")){
                        s.setString(1,household.toString());s.setString(2,member.toString());s.setString(3,village.toString());s.setLong(4,adulthood);
                        if(s.executeUpdate()!=1)throw new SQLException("Household formation eligibility changed");
                    }
                    try(var s=c.prepareStatement("INSERT INTO household_formation_members VALUES(?,?)")){
                        s.setString(1,member.toString());s.setString(2,household.toString());s.executeUpdate();
                    }
                }
                new DemographicRepository(c).recordCapacity(household,0);formed++;
            }
            c.commit();c.setAutoCommit(true);return formed;
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private boolean recorded(UUID resident)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM household_formation_members WHERE resident_id=?")){
            s.setString(1,resident.toString());try(var r=s.executeQuery()){return r.next();}}
    }
    private boolean hasUnassignedChild(UUID parent,long adulthood)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM family_ties f JOIN residents r ON r.id=f.second_id WHERE f.first_id=? AND f.type='PARENT_CHILD' AND r.status='ALIVE' AND r.household_id IS NULL AND r.age_steps<? LIMIT 1")){
            s.setString(1,parent.toString());s.setLong(2,adulthood);try(var r=s.executeQuery()){return r.next();}}
    }
    private boolean onlyPartner(PopulationRepository people,UUID resident,UUID expected)throws SQLException {
        for(var tie:people.findFamilyTies(resident))if(tie.type()==FamilyTie.Type.PARTNER){
            UUID other=tie.first().equals(resident)?tie.second():tie.first();
            if(!other.equals(expected)&&people.findResident(other).orElseThrow().status()==Resident.Status.ALIVE)return false;
        }return true;
    }
}
