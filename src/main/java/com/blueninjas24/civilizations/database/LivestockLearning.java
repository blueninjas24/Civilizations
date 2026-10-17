package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.WorkLearningPolicy;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Paid feed-backed animal care teaches real workers inside the same herd/clock transaction. */
final class LivestockLearning {
    private final Connection connection;private final WorkLearningPolicy policy;
    LivestockLearning(Connection connection,WorkLearningPolicy policy){this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){
            s.execute("""
                CREATE TABLE livestock_learning_reviews (
                    business_id TEXT NOT NULL,period INTEGER NOT NULL CHECK(typeof(period)='integer' AND period>=1),
                    work_used INTEGER NOT NULL CHECK(typeof(work_used)='integer' AND work_used>=0),
                    skill TEXT,policy TEXT NOT NULL,explanation TEXT NOT NULL,
                    PRIMARY KEY(business_id,period),
                    FOREIGN KEY(business_id,period) REFERENCES livestock_periods(business_id,period)
                )
                """);
            s.execute("""
                CREATE TABLE livestock_learning_entries (
                    business_id TEXT NOT NULL,period INTEGER NOT NULL,resident_id TEXT NOT NULL REFERENCES residents(id),
                    before_experience INTEGER NOT NULL CHECK(typeof(before_experience)='integer' AND before_experience>=0),
                    after_experience INTEGER NOT NULL CHECK(typeof(after_experience)='integer' AND after_experience>before_experience),
                    PRIMARY KEY(business_id,period,resident_id),
                    FOREIGN KEY(business_id,period) REFERENCES livestock_learning_reviews(business_id,period)
                )
                """);
        }
    }
    String reviewRows(UUID business,long period)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Herd learning requires the actual fresh work transaction");
        try(var s=connection.prepareStatement("SELECT explanation FROM livestock_learning_reviews WHERE business_id=? AND period=?")){
            s.setString(1,business.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(r.next())return r.getString(1);}
        }
        long work;
        try(var s=connection.prepareStatement("SELECT work_used FROM livestock_periods WHERE business_id=? AND period=? AND operation_id IS NOT NULL")){
            s.setString(1,business.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Actual saved herd work required for learning");work=r.getLong(1);}
        }
        String skill=policy.industrySkills().get("livestock");var entries=new ArrayList<IndustryLearning.Entry>();String reason;
        var businesses=new BusinessRepository(connection);var people=new PopulationRepository(connection);
        var jobs=new ArrayList<>(businesses.currentWorkers(business));jobs.sort(Comparator.comparing(job->job.residentId().toString()));
        if(!policy.enabled()||skill==null)reason="Herd learning disabled or no configured livestock skill";
        else if(work==0)reason="No actual animal care; no experience awarded";
        else if(jobs.size()>policy.maximumWorkersPerReview())reason="Herd worker review budget exceeded; no partial skill awards";
        else {
            var requirements=businesses.findFounding(business).orElseThrow().workerRequirements();var eligible=new ArrayList<UUID>();
            for(var job:jobs){var person=people.findResident(job.residentId()).orElseThrow();
                if(person.settlementId().equals(businesses.findBusiness(business).orElseThrow().settlementId())&&WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),requirements).eligible())eligible.add(person.id());}
            // At least one real care-work unit per learner; rotate scarce work rather than training every saved employee.
            int count=(int)Math.min(work,eligible.size()),offset=eligible.isEmpty()?0:(int)Math.floorMod(period-1,(long)eligible.size());
            for(int i=0;i<count;i++){var person=eligible.get((offset+i)%eligible.size());long before=people.findWorkProfile(person).orElseThrow().experience().getOrDefault(skill,0L),after=policy.nextExperience(before);
                if(after>before)entries.add(new IndustryLearning.Entry(person,before,after));}
            reason="Actual animal care: "+entries.size()+" qualified workers learned "+skill+"; work="+work+"; no owner qualification waiver";
        }
        try(var s=connection.prepareStatement("INSERT INTO livestock_learning_reviews VALUES(?,?,?,?,?,?)")){
            s.setString(1,business.toString());s.setLong(2,period);s.setLong(3,work);if(skill==null)s.setNull(4,Types.VARCHAR);else s.setString(4,skill);s.setString(5,policy.toString());s.setString(6,reason);s.executeUpdate();
        }
        for(var entry:entries){people.recordSkillExperience(entry.resident(),skill,entry.after());
            try(var s=connection.prepareStatement("INSERT INTO livestock_learning_entries VALUES(?,?,?,?,?)")){
                s.setString(1,business.toString());s.setLong(2,period);s.setString(3,entry.resident().toString());s.setLong(4,entry.before());s.setLong(5,entry.after());s.executeUpdate();}
        }
        return reason;
    }
}
