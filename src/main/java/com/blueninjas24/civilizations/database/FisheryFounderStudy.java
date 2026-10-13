package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** One prospective fishing owner associated with actual funded water and repeatedly confirmed banks. */
final class FisheryFounderStudy {
    private final Connection connection;
    private final FisheryOpportunities opportunities;
    private final FisheryFoundingPolicy founding;
    private final FisheryShorelinePolicy shoreline;
    private final FisheryOpportunityPolicy terms;
    private final CareerStudyPolicy careers;
    private final StudyPolicy study;
    private final long adulthood;
    FisheryFounderStudy(Connection connection,FisheryOpportunities opportunities,FisheryFoundingPolicy founding,FisheryShorelinePolicy shoreline,
            FisheryOpportunityPolicy terms,CareerStudyPolicy careers,StudyPolicy study,long adulthood) {
        this.connection=connection;this.opportunities=opportunities;this.founding=founding;this.shoreline=shoreline;this.terms=terms;this.careers=careers;this.study=study;this.adulthood=adulthood;
    }
    private boolean enabled(){return founding.enabled()&&shoreline.enabled()&&careers.enabled()&&careers.industries().contains("fishing")&&study.enabled();}
    static String key(WaterPatch.Patch water){return FisheryWorkplaces.hash(water.evidence().world()+":"+water.surfaceY()+":"+water.evidence().columns());}
    String reviewRows(FisheryOpportunities.Opportunity opportunity,ShorelineEvidence bank,long now,Set<UUID> worlds,long period,long eligibleAfter)throws SQLException {
        owner();if(!enabled())return "First fishing-owner study disabled";
        var courseTerms=study.skills().get("fishing");
        if(courseTerms==null||courseTerms.targetExperience()<founding.founderExperience())return "Fishing study cannot qualify an owner";
        var review=opportunities.reviewRows(opportunity.town(),now,worlds);
        if(!review.candidates().contains(opportunity))return "Fishing study waits for current funded water opportunity";
        if(!bank.world().equals(opportunity.water().evidence().world())||bank.capturedMillis()<opportunity.water().evidence().observedMillis()
                ||bank.capturedMillis()>now||now-bank.capturedMillis()>terms.evidenceLifetimeMillis())throw new SQLException("Fresh same-world fishing study bank required");
        var expected=new ShorelineScan(p->ShorelineScan.Cell.UNKNOWN,opportunity.water(),shoreline.maximumRise()).candidates();
        if(!expected.equals(bank.assessments().stream().map(ShorelineScan.Assessment::position).toList())
                ||ShorelineRecovery.decide(bank.origin(),shoreline.maximumDistance(),bank.assessments()).action()!=ShorelineRecovery.Action.KEEP)throw new SQLException("Exact actually safe fishing study bank required");
        try(var s=connection.prepareStatement("""
            SELECT 1 FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
              AND COALESCE((SELECT experience FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='fishing'),0)>=?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE') LIMIT 1
            """)) {
            s.setString(1,opportunity.town().toString());s.setLong(2,Math.max(adulthood,founding.minimumAgeSteps()));s.setLong(3,founding.founderExperience());
            try(var r=s.executeQuery()){if(r.next())return "Qualified first fishing owner already available; no extra course";}
        }
        String habitat=key(opportunity.water());UUID existing=null;
        try(var s=connection.prepareStatement("SELECT f.course_id,f.habitat_key FROM fishery_founder_courses f JOIN resident_courses c ON c.id=f.course_id WHERE f.settlement_id=? AND c.status='ACTIVE' ORDER BY f.course_id LIMIT 2")) {
            s.setString(1,opportunity.town().toString());try(var r=s.executeQuery()){if(r.next()){if(!habitat.equals(r.getString(2)))return "Existing fishing learner waits for their original habitat";existing=UUID.fromString(r.getString(1));if(r.next())throw new SQLException("Multiple first fishing-owner courses");}}
        }
        if(existing!=null){refresh(existing,bank);return "Existing first fishing-owner course has a fresh safe bank; no new skill, job or spending";}
        var account=new CommodityRepository(connection).read(opportunity.town()).orElseThrow();
        for(var e:courseTerms.materials().entrySet()){var stock=account.state().stocks().get(e.getKey());if(stock==null||stock.units()<Math.addExact(e.getValue(),study.materialReserveUnits()))return "Fishing-owner study waits for real practice supplies/reserve";}
        UUID learner=null;
        try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
              AND COALESCE((SELECT experience FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='fishing'),0)<?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
              AND NOT EXISTS(SELECT 1 FROM businesses b WHERE b.owner_id=r.id AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id))
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,opportunity.town().toString());s.setLong(2,Math.max(adulthood,founding.minimumAgeSteps()));s.setLong(3,founding.founderExperience());s.setInt(4,careers.candidatesPerReview());
            try(var r=s.executeQuery()){if(r.next())learner=UUID.fromString(r.getString(1));}
        }
        if(learner==null)return "No eligible unemployed adult for first fishing-owner study";
        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM resident_courses WHERE settlement_id=? AND status='ACTIVE'")) {
            s.setString(1,opportunity.town().toString());try(var r=s.executeQuery()){if(r.getInt(1)>=study.activeCoursesPerVillage())return "Village study budget reached";}
        }
        UUID course=UUID.nameUUIDFromBytes(("civilizations:first-fishing-study-v1:"+opportunity.town()+":"+habitat+":"+learner+":"+period).getBytes(StandardCharsets.UTF_8));
        new ResidentStudy(connection,study,adulthood).enrollRows(course,learner,opportunity.town(),"fishing",eligibleAfter);
        try(var s=connection.prepareStatement("INSERT INTO fishery_founder_courses VALUES(?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,course.toString());s.setString(2,opportunity.town().toString());s.setString(3,bank.world().toString());s.setString(4,habitat);s.setInt(5,bank.origin().x());s.setInt(6,bank.origin().y());s.setInt(7,bank.origin().z());s.setLong(8,bank.capturedMillis());s.setLong(9,period);s.executeUpdate();
        }
        return "One first fishing-owner course enrolled for real funded water and safe bank; future real-supply study only";
    }
    private void refresh(UUID course,ShorelineEvidence bank)throws SQLException {
        try(var s=connection.prepareStatement("UPDATE fishery_founder_courses SET x=?,y=?,z=?,observed_ms=? WHERE course_id=? AND observed_ms<=?")) {
            s.setInt(1,bank.origin().x());s.setInt(2,bank.origin().y());s.setInt(3,bank.origin().z());s.setLong(4,bank.capturedMillis());s.setString(5,course.toString());s.setLong(6,bank.capturedMillis());s.executeUpdate();
        }
    }
    ResidentStudy.FounderState stateRows(UUID course,long now,Set<UUID> worlds)throws SQLException {
        owner();UUID town,world;String habitat;long observed;
        try(var s=connection.prepareStatement("SELECT * FROM fishery_founder_courses WHERE course_id=?")) {
            s.setString(1,course.toString());try(var r=s.executeQuery()){if(!r.next())return ResidentStudy.FounderState.READY;town=UUID.fromString(r.getString("settlement_id"));world=UUID.fromString(r.getString("world_id"));habitat=r.getString("habitat_key");observed=r.getLong("observed_ms");}
        }
        try(var s=connection.prepareStatement("SELECT 1 FROM businesses WHERE settlement_id=? AND industry='fishing' LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())return ResidentStudy.FounderState.CANCEL;}
        }
        if(!enabled()||!worlds.contains(world)||observed>now||now-observed>terms.evidenceLifetimeMillis())return ResidentStudy.FounderState.WAIT;
        var courseTerms=study.skills().get("fishing");if(courseTerms==null||courseTerms.targetExperience()<founding.founderExperience())return ResidentStudy.FounderState.WAIT;
        return opportunities.reviewForTrainingRows(town,now,worlds).candidates().stream().anyMatch(c->key(c.water()).equals(habitat))?ResidentStudy.FounderState.READY:ResidentStudy.FounderState.WAIT;
    }
    private void owner()throws SQLException{if(connection.getAutoCommit())throw new SQLException("Fishing study requires economy owner transaction");}
}
