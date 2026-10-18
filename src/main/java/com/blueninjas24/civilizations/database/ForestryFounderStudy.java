package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** One real-supply learner linked to a currently fundable observed forest opportunity. */
final class ForestryFounderStudy {
    private final Connection connection;
    private final AutomaticForestryFounding forestry;
    private final CareerStudyPolicy careers;
    private final StudyPolicy study;
    private final ForestryFoundingPolicy founding;
    private final long adulthood;

    ForestryFounderStudy(Connection connection,AutomaticForestryFounding forestry,CareerStudyPolicy careers,
                         StudyPolicy study,ForestryFoundingPolicy founding,long adulthood) {
        this.connection=connection;this.forestry=forestry;this.careers=careers;this.study=study;this.founding=founding;this.adulthood=adulthood;
    }

    String reviewRows(AutomaticForestryFounding.Opportunity opportunity,long period,long eligibleAfter)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Forestry founder study requires economy owner transaction");
        if(!enabled())return "Automatic forestry founder study disabled";
        var terms=study.skills().get("forestry");
        if(terms==null||terms.targetExperience()<founding.founderExperience())return "Forestry study target cannot qualify a logging-business founder";
        UUID town=opportunity.town();String after="";
        try(var s=connection.prepareStatement("SELECT explanation FROM forestry_founder_reviews WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(r.next())return r.getString(1);}
        }
        try(var s=connection.prepareStatement("SELECT after_resident FROM forestry_founder_reviews WHERE settlement_id=? ORDER BY period DESC LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())after=r.getString(1);}
        }
        int active;
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM forestry_founder_courses f JOIN resident_courses c ON c.id=f.course_id WHERE f.settlement_id=? AND c.status='ACTIVE'")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){active=r.getInt(1);}
        }
        int allActive;
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM resident_courses WHERE settlement_id=? AND status='ACTIVE'")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){allActive=r.getInt(1);}
        }
        String reason;var account=new CommodityRepository(connection).read(town).orElseThrow();
        boolean supplied=terms.materials().entrySet().stream().allMatch(e->{var stock=account.state().stocks().get(e.getKey());return stock!=null&&stock.units()>=Math.addExact(e.getValue(),study.materialReserveUnits());});
        if(active>0)reason="One actual forestry founder prospect already studying; no duplicate sponsor";
        else if(allActive>=study.activeCoursesPerVillage())reason="Village study enrollment budget reached; no partial enrollment";
        else if(!supplied)reason="Forestry founder study waits for actual practice supplies and reserve";
        else {
            var candidates=page(town,after);if(candidates.isEmpty()&&!after.isEmpty())candidates=page(town,"");
            if(candidates.isEmpty())reason="No eligible unemployed adult in bounded forestry-founder page";
            else {
                UUID resident=candidates.getFirst();after=resident.toString();Position origin=opportunity.origin();
                UUID course=UUID.nameUUIDFromBytes(("civilizations:forestry-founder-study-v1:"+opportunity.key()+":"+resident+":"+period).getBytes(StandardCharsets.UTF_8));
                new ResidentStudy(connection,study,adulthood).enrollRows(course,resident,town,"forestry",eligibleAfter);
                try(var s=connection.prepareStatement("INSERT INTO forestry_founder_courses VALUES(?,?,?,?,?,?,?)")) {
                    s.setString(1,course.toString());s.setString(2,town.toString());s.setString(3,opportunity.patch().world().toString());
                    s.setInt(4,origin.x());s.setInt(5,origin.y());s.setInt(6,origin.z());s.setLong(7,period);s.executeUpdate();
                }
                reason="One prospective forestry owner enrolled for a real observed forest and protected funding; future supply-backed study only";
            }
        }
        try(var s=connection.prepareStatement("INSERT INTO forestry_founder_reviews VALUES(?,?,?,?,?,?)")) {
            s.setString(1,town.toString());s.setLong(2,period);s.setString(3,opportunity.key());s.setString(4,after);
            s.setString(5,careers+"; "+study+"; "+founding);s.setString(6,reason);s.executeUpdate();
        }
        return reason;
    }

    ResidentStudy.FounderState stateRows(UUID course,long now,Set<UUID> worlds)throws SQLException {
        AutomaticForestryFounding.Opportunity saved;
        try(var s=connection.prepareStatement("SELECT * FROM forestry_founder_courses WHERE course_id=?")) {
            s.setString(1,course.toString());try(var r=s.executeQuery()) {
                if(!r.next())return ResidentStudy.FounderState.READY;
                var observation=new ForestObservation(UUID.fromString(r.getString("world_id")),0,true,
                        List.of(new Position(r.getInt("x"),r.getInt("y"),r.getInt("z"))));
                saved=new AutomaticForestryFounding.Opportunity(UUID.fromString(r.getString("settlement_id")),observation);
            }
        }
        if(!enabled()||worlds==null||!worlds.contains(saved.patch().world())||!forestry.provenanceTrusted())return ResidentStudy.FounderState.WAIT;
        var evidence=forestry.evidenceState(saved,now);
        if(evidence==AutomaticForestryFounding.EvidenceState.UNKNOWN)return ResidentStudy.FounderState.WAIT;
        if(evidence==AutomaticForestryFounding.EvidenceState.ABSENT)return ResidentStudy.FounderState.CANCEL;
        try{return forestry.opportunityRows(saved.town(),now,worlds,saved).opportunity()!=null?ResidentStudy.FounderState.READY:ResidentStudy.FounderState.CANCEL;}
        catch(BusinessStartupGrants.Declined refusal){return ResidentStudy.FounderState.CANCEL;}
    }

    private boolean enabled(){return founding.enabled()&&careers.enabled()&&careers.industries().contains("logging")&&study.enabled();}

    private List<UUID> page(UUID town,String after)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=? AND r.id>?
              AND COALESCE((SELECT experience FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='forestry'),0)<?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
              AND NOT EXISTS(SELECT 1 FROM businesses b WHERE b.owner_id=r.id AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id))
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setLong(2,Math.max(adulthood,founding.minimumAgeSteps()));s.setString(3,after);
            s.setLong(4,founding.founderExperience());s.setInt(5,careers.candidatesPerReview());
            try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }
        return result;
    }
}
