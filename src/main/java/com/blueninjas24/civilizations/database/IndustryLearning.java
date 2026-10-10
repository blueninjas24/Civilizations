package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.WorkLearningPolicy;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Experience and immutable participant audit inside the actual new production transaction. */
public final class IndustryLearning {
    public record Entry(UUID resident,long before,long after) {}
    private final Connection connection;
    private final WorkLearningPolicy policy;
    public IndustryLearning(Connection connection,WorkLearningPolicy policy){this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);}
    String reviewRows(UUID operation,UUID business,boolean freshProduction)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Learning requires actual production transaction");
        var saved=explanation(operation);if(saved.isPresent())return saved.orElseThrow();
        if(!freshProduction)return "Historical production replay; no retroactive skill award";
        long quantity,period;
        try(var s=connection.prepareStatement("SELECT p.period,o.quantity FROM industry_production p JOIN industry_operations o ON o.id=p.operation_id WHERE p.operation_id=? AND p.business_id=?")) {
            s.setString(1,operation.toString());s.setString(2,business.toString());try(var r=s.executeQuery()) {
                if(!r.next())return "No productive receipt; no skill award";period=r.getLong(1);quantity=r.getLong(2);
            }
        }
        var firms=new BusinessRepository(connection);var b=firms.findBusiness(business).orElseThrow();
        var skill=policy.industrySkills().get(b.industry());var learners=new ArrayList<Entry>();
        String reason;
        if(!policy.enabled())reason="Work learning disabled; no experience awarded";
        else if(skill==null)reason="No configured skill for industry; no experience awarded";
        else if(quantity==0)reason="No actual production; no experience awarded";
        else {
            var jobs=firms.currentWorkers(business);
            if(jobs.size()>policy.maximumWorkersPerReview())reason="Work learning deferred: worker review budget exceeded; no partial awards";
            else {
                var people=new PopulationRepository(connection);var requirements=firms.findFounding(business).orElseThrow().workerRequirements();
                for(var job:jobs) {
                    var person=people.findResident(job.residentId()).orElseThrow();var profile=people.findWorkProfile(person.id()).orElseThrow();
                    if(!WorkEligibility.evaluate(person,profile,requirements).eligible())continue;
                    long before=profile.experience().getOrDefault(skill,0L),after=policy.nextExperience(before);
                    if(after>before)learners.add(new Entry(person.id(),before,after));
                }
                reason="Actual productive work: "+learners.size()+" qualified employees learned "+skill+"; gain up to "+policy.experiencePerProductivePeriod()+", ceiling "+policy.maximumExperience()+"; no ownership waiver";
            }
        }
        try(var s=connection.prepareStatement("INSERT INTO industry_learning_reviews(operation_id,business_id,period,enabled,skill,gain,ceiling,worker_budget,explanation) VALUES(?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,business.toString());s.setLong(3,period);s.setInt(4,policy.enabled()?1:0);
            if(skill==null)s.setNull(5,Types.VARCHAR);else s.setString(5,skill);
            s.setLong(6,policy.experiencePerProductivePeriod());s.setLong(7,policy.maximumExperience());s.setInt(8,policy.maximumWorkersPerReview());s.setString(9,reason);s.executeUpdate();
        }
        var people=new PopulationRepository(connection);
        for(var learner:learners) {
            people.recordSkillExperience(learner.resident(),skill,learner.after());
            try(var s=connection.prepareStatement("INSERT INTO industry_learning_entries(operation_id,resident_id,before_experience,after_experience) VALUES(?,?,?,?)")) {
                s.setString(1,operation.toString());s.setString(2,learner.resident().toString());s.setLong(3,learner.before());s.setLong(4,learner.after());s.executeUpdate();
            }
        }
        return reason;
    }
    public Optional<String> explanation(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT explanation FROM industry_learning_reviews WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(r.getString(1)):Optional.empty();}
        }
    }
    public List<Entry> entries(UUID operation)throws SQLException {
        var result=new ArrayList<Entry>();try(var s=connection.prepareStatement("SELECT * FROM industry_learning_entries WHERE operation_id=? ORDER BY resident_id")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()){while(r.next())result.add(new Entry(UUID.fromString(r.getString("resident_id")),r.getLong("before_experience"),r.getLong("after_experience")));}
        }
        return List.copyOf(result);
    }
}
