package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.WorkLearningPolicy;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Actual new warehouse purchases train current employees in the same owner transaction. */
final class CommerceLearning {
    private final Connection connection;
    private final WorkLearningPolicy policy;
    CommerceLearning(Connection connection,WorkLearningPolicy policy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);
    }
    String reviewRows(UUID business,long period,List<UUID> freshPurchases)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Commerce learning requires purchase transaction");
        if(freshPurchases.isEmpty())return "No new paid warehouse orders; no commerce experience";
        if(freshPurchases.size()>128||new HashSet<>(freshPurchases).size()!=freshPurchases.size())
            throw new SQLException("Invalid bounded commerce receipts");
        var firms=new BusinessRepository(connection);
        var founding=firms.findFounding(business).orElseThrow();
        if(!founding.business().industry().equals("warehouse"))throw new SQLException("Commerce review requires warehouse");
        for(var id:freshPurchases)try(var s=connection.prepareStatement("SELECT 1 FROM local_shipments WHERE id=? AND buyer_id=? AND dispatched_period=? AND units>0 AND price>0")) {
            s.setString(1,id.toString());s.setString(2,business.toString());s.setLong(3,period);
            try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Commerce requires actual paid current purchase receipt");}
        }
        String skill=policy.industrySkills().get("warehouse");
        var learners=new ArrayList<IndustryLearning.Entry>();
        String reason;
        var jobs=firms.currentWorkers(business);
        if(!policy.enabled()||skill==null)reason="Commerce learning disabled or unmapped; no award";
        else if(jobs.size()>policy.maximumWorkersPerReview())reason="Commerce worker budget exceeded; no partial awards";
        else {
            var people=new PopulationRepository(connection);
            for(var job:jobs) {
                var resident=people.findResident(job.residentId()).orElseThrow();
                var profile=people.findWorkProfile(resident.id()).orElseThrow();
                if(!resident.settlementId().equals(founding.business().settlementId())
                        ||!WorkEligibility.evaluate(resident,profile,founding.workerRequirements()).eligible())continue;
                long before=profile.experience().getOrDefault(skill,0L),after=policy.nextExperience(before);
                if(after>before)learners.add(new IndustryLearning.Entry(resident.id(),before,after));
            }
            reason="Actual paid warehouse purchases: "+learners.size()+" qualified employees learned "+skill+" once this period; ownership alone gives no experience";
        }
        try(var s=connection.prepareStatement("INSERT INTO commerce_learning_reviews VALUES(?,?,?,?,?,?,?,?)")) {
            s.setString(1,business.toString());s.setLong(2,period);s.setInt(3,policy.enabled()?1:0);s.setString(4,skill);
            s.setLong(5,policy.experiencePerProductivePeriod());s.setLong(6,policy.maximumExperience());s.setInt(7,policy.maximumWorkersPerReview());s.setString(8,reason);s.executeUpdate();
        }
        for(var id:freshPurchases)try(var s=connection.prepareStatement("INSERT INTO commerce_learning_receipts VALUES(?,?,?)")) {
            s.setString(1,business.toString());s.setLong(2,period);s.setString(3,id.toString());s.executeUpdate();
        }
        var people=new PopulationRepository(connection);
        for(var entry:learners) {
            people.recordSkillExperience(entry.resident(),skill,entry.after());
            try(var s=connection.prepareStatement("INSERT INTO commerce_learning_entries VALUES(?,?,?,?,?)")) {
                s.setString(1,business.toString());s.setLong(2,period);s.setString(3,entry.resident().toString());s.setLong(4,entry.before());s.setLong(5,entry.after());s.executeUpdate();
            }
        }
        return reason;
    }
}
