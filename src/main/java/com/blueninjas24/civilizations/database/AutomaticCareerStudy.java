package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Bounded, saved career choices for actual vacancies, inside the village economy owner transaction. */
final class AutomaticCareerStudy {
    private final Connection connection;
    private final CareerStudyPolicy policy;
    private final StudyPolicy studyPolicy;
    private final long adulthood;
    AutomaticCareerStudy(Connection connection,CareerStudyPolicy policy,StudyPolicy studyPolicy,long adulthood) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);
        this.studyPolicy=Objects.requireNonNull(studyPolicy);this.adulthood=adulthood;
    }
    String reviewRows(UUID business,long step,long period,long eligibleAfter)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Career review requires economy transaction");
        if(!policy.enabled()||!studyPolicy.enabled())return "Automatic career study disabled";
        try(var s=connection.prepareStatement("SELECT explanation FROM career_study_reviews WHERE business_id=? AND period=?")) {
            s.setString(1,business.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(r.next())return r.getString(1);}
        }
        var businesses=new BusinessRepository(connection);var founding=businesses.findFounding(business).orElseThrow();
        var b=founding.business();var requirements=founding.workerRequirements();
        int slots=vacancies(connection,business),enrolled=0;String after="";
        try(var s=connection.prepareStatement("SELECT after_resident FROM career_study_reviews WHERE business_id=? ORDER BY period DESC LIMIT 1")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){if(r.next())after=r.getString(1);}
        }
        String reason;
        if(step<b.foundedStep()||!policy.industries().contains(b.industry())||slots<=0)reason="No permitted live-owner vacancy; no career enrollment";
        else {
            int sponsored,active;
            try(var s=connection.prepareStatement("SELECT COUNT(*) FROM career_study_enrollments e JOIN resident_courses c ON c.id=e.course_id WHERE e.business_id=? AND c.status='ACTIVE'")) {
                s.setString(1,business.toString());try(var r=s.executeQuery()){sponsored=r.getInt(1);}
            }
            try(var s=connection.prepareStatement("SELECT COUNT(*) FROM resident_courses WHERE settlement_id=? AND status='ACTIVE'")) {
                s.setString(1,b.settlementId().toString());try(var r=s.executeQuery()){active=r.getInt(1);}
            }
            slots=Math.min(slots-sponsored,studyPolicy.activeCoursesPerVillage()-active);
            var candidates=page(b.settlementId(),after,Math.max(adulthood,requirements.minimumAgeSteps()));
            if(candidates.isEmpty()&&!after.isEmpty())candidates=page(b.settlementId(),"",Math.max(adulthood,requirements.minimumAgeSteps()));
            var people=new PopulationRepository(connection);var study=new ResidentStudy(connection,studyPolicy,adulthood);
            var catalog=new CommodityRepository(connection).read(b.settlementId()).orElseThrow().state().stocks().keySet();
            for(var candidate:candidates) {
                if(enrolled>=Math.min(slots,policy.enrollmentsPerReview()))break;
                after=candidate.toString();
                var experience=people.findWorkProfile(candidate).orElseThrow().experience();
                var missing=new TreeMap<String,Long>();
                requirements.minimumExperience().forEach((skill,minimum)->{if(experience.getOrDefault(skill,0L)<minimum)missing.put(skill,minimum);});
                if(missing.isEmpty())continue; // Hiring, not unnecessary training, handles qualified people.
                boolean feasible=missing.entrySet().stream().allMatch(e->{var course=studyPolicy.skills().get(e.getKey());return course!=null
                        &&course.targetExperience()>=e.getValue()&&studyPolicy.maximumExperience()>=e.getValue()&&catalog.containsAll(course.materials().keySet());});
                if(!feasible)continue;
                String skill=missing.firstKey();
                var course=UUID.nameUUIDFromBytes(("civilizations:vacancy-study-v1:"+business+":"+candidate+":"+skill+":"+period).getBytes(StandardCharsets.UTF_8));
                study.enrollRows(course,candidate,b.settlementId(),skill,eligibleAfter);
                try(var s=connection.prepareStatement("INSERT INTO career_study_enrollments VALUES(?,?,?)")) {
                    s.setString(1,course.toString());s.setString(2,business.toString());s.setLong(3,period);s.executeUpdate();
                }
                enrolled++;
            }
            reason="Vacancy career study: "+enrolled+" actual local adults enrolled; remaining vacancies "+Math.max(0,slots-enrolled)
                    +"; supplies/food checked at each future study period; no skill, employment or money created at enrollment";
        }
        try(var s=connection.prepareStatement("INSERT INTO career_study_reviews VALUES(?,?,?,'food',?,?,?)")) {
            s.setString(1,business.toString());s.setString(2,b.settlementId().toString());s.setLong(3,period);
            s.setString(4,after);s.setString(5,policy.toString());s.setString(6,reason);s.executeUpdate();
        }
        return reason;
    }
    static int vacancies(Connection connection,UUID business)throws SQLException {
        if(BusinessClosure.blocked(connection,business))return 0;
        try(var s=connection.prepareStatement("""
            SELECT b.worker_capacity-(SELECT COUNT(*) FROM business_employment e WHERE e.business_id=b.id AND e.ended_step IS NULL)
            FROM businesses b JOIN residents r ON r.id=b.owner_id
            WHERE b.id=? AND r.status='ALIVE' AND r.settlement_id=b.settlement_id
            """)) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next()?Math.max(0,r.getInt(1)):0;}
        }
    }
    static boolean opportunityAvailable(Connection connection,UUID course)throws SQLException {
        try(var s=connection.prepareStatement("SELECT business_id FROM career_study_enrollments WHERE course_id=?")) {
            s.setString(1,course.toString());try(var r=s.executeQuery()){return !r.next()||vacancies(connection,UUID.fromString(r.getString(1)))>0;}
        }
    }
    private List<UUID> page(UUID town,String after,long age)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=? AND r.id>?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
              AND NOT EXISTS(SELECT 1 FROM businesses b WHERE b.owner_id=r.id AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id))
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setLong(2,age);s.setString(3,after);s.setInt(4,policy.candidatesPerReview());
            try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }
        return result;
    }
}
