package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** One supply-backed prospective founder for a current fundable quarry/pit; never invents an employer. */
final class QuarryFounderStudy {
    private final Connection connection;
    private final AutomaticQuarryFounding mines;
    private final CareerStudyPolicy careers;
    private final StudyPolicy study;
    private final QuarryFoundingPolicy founding;
    private final long adulthood;
    QuarryFounderStudy(Connection connection,AutomaticQuarryFounding mines,CareerStudyPolicy careers,StudyPolicy study,QuarryFoundingPolicy founding,long adulthood) {
        this.connection=connection;this.mines=mines;this.careers=careers;this.study=study;this.founding=founding;this.adulthood=adulthood;
    }
    String reviewRows(AutomaticQuarryFounding.Opportunity opportunity,long period,long eligibleAfter)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Founder study requires mine/economy transaction");
        if(!careers.enabled()||!careers.industries().contains(opportunity.patch().mineral().equals(Commodity.STONE)?"quarry":"sandpit")||!study.enabled())return "Automatic mining founder study disabled";
        var terms=study.skills().get("mining");
        if(terms==null||terms.targetExperience()<founding.founderExperience())return "Mining study target cannot qualify a founder";
        UUID town=opportunity.town();String after="";
        try(var s=connection.prepareStatement("SELECT explanation FROM quarry_founder_reviews WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(r.next())return r.getString(1);}
        }
        try(var s=connection.prepareStatement("SELECT after_resident FROM quarry_founder_reviews WHERE settlement_id=? ORDER BY period DESC LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())after=r.getString(1);}
        }
        int active;
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM quarry_founder_courses m JOIN resident_courses c ON c.id=m.course_id WHERE m.settlement_id=? AND c.status='ACTIVE'")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){active=r.getInt(1);}
        }
        int allActive;
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM resident_courses WHERE settlement_id=? AND status='ACTIVE'")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){allActive=r.getInt(1);}
        }
        int enrolled=0;String reason;
        var account=new CommodityRepository(connection).read(town).orElseThrow();
        boolean supplied=terms.materials().entrySet().stream().allMatch(e->{var stock=account.state().stocks().get(e.getKey());return stock!=null&&stock.units()>=Math.addExact(e.getValue(),study.materialReserveUnits());});
        if(active>0)reason="One actual quarry/pit founder prospect already studying; no duplicate sponsor";
        else if(allActive>=study.activeCoursesPerVillage())reason="Village study enrollment budget reached; no partial enrollment";
        else if(!supplied)reason="Founder study waits for actual practice supplies and reserve";
        else {
            var candidates=page(town,after);if(candidates.isEmpty()&&!after.isEmpty())candidates=page(town,"");
            for(var resident:candidates) {
                after=resident.toString();
                if(new PopulationRepository(connection).findWorkProfile(resident).orElseThrow().experience().getOrDefault("mining",0L)>=founding.founderExperience())continue;
                UUID course=UUID.nameUUIDFromBytes(("civilizations:quarry-founder-study-v1:"+opportunity.key()+":"+resident+":"+period).getBytes(StandardCharsets.UTF_8));
                new ResidentStudy(connection,study,adulthood).enrollRows(course,resident,town,"mining",eligibleAfter);
                try(var s=connection.prepareStatement("INSERT INTO quarry_founder_courses VALUES(?,?,?,?,?,?,?,?)")) {
                    s.setString(1,course.toString());s.setString(2,town.toString());s.setString(3,opportunity.patch().world().toString());s.setString(4,opportunity.patch().mineral().id());
                    s.setInt(5,opportunity.origin().x());s.setInt(6,opportunity.origin().y());s.setInt(7,opportunity.origin().z());s.setLong(8,period);s.executeUpdate();
                }
                enrolled=1;break;
            }
            reason=enrolled==1?"One prospective quarry/pit owner enrolled for real customers, observed ore and available protected funding; future supply-backed study only":"No eligible unemployed adult in bounded founder-study page";
        }
        try(var s=connection.prepareStatement("INSERT INTO quarry_founder_reviews VALUES(?,?,?,?,?,?)")) {
            s.setString(1,town.toString());s.setLong(2,period);s.setString(3,opportunity.key());s.setString(4,after);s.setString(5,careers+"; "+study+"; "+founding);s.setString(6,reason);s.executeUpdate();
        }
        return reason;
    }
    boolean availableRows(UUID course,long now,Set<UUID> worlds)throws SQLException {
        AutomaticQuarryFounding.Opportunity saved;
        try(var s=connection.prepareStatement("SELECT * FROM quarry_founder_courses WHERE course_id=?")) {
            s.setString(1,course.toString());try(var r=s.executeQuery()) {
                if(!r.next())return true;
                var patch=new MineralObservation(UUID.fromString(r.getString("world_id")),0,true,new Commodity(r.getString("mineral")),List.of(new Position(r.getInt("x"),r.getInt("y"),r.getInt("z"))));
                saved=new AutomaticQuarryFounding.Opportunity(UUID.fromString(r.getString("settlement_id")),patch);
            }
        }
        try{return mines.opportunityRows(saved.town(),now,worlds==null?Set.of():worlds,saved).opportunity()!=null;}
        catch(BusinessStartupGrants.Declined refusal){return false;}
    }
    private List<UUID> page(UUID town,String after)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=? AND r.id>?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
              AND NOT EXISTS(SELECT 1 FROM businesses b WHERE b.owner_id=r.id AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id))
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setLong(2,Math.max(adulthood,founding.minimumAgeSteps()));s.setString(3,after);s.setInt(4,careers.candidatesPerReview());
            try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }return result;
    }
}
