package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.WorkLearningPolicy;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Skill awards belong to the same transaction as a newly produced food period. */
public final class FarmLearning {
    private final Connection connection;
    private final WorkLearningPolicy policy;
    public FarmLearning(Connection connection,WorkLearningPolicy policy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);
    }
    String reviewRows(UUID town,long period,long fieldSlots)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Farm learning requires food-period transaction");
        if(fieldSlots<0)throw new IllegalArgumentException("Negative field slots");
        var old=explanation(town,period);if(old.isPresent())return old.orElseThrow();
        long output;
        try(var s=connection.prepareStatement("SELECT production FROM economy_periods WHERE settlement_id=? AND period=? AND commodity='food'")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()) {
                if(!r.next())throw new SQLException("Farm learning requires saved food production");output=r.getLong(1);
            }
        }
        var firms=new BusinessRepository(connection);var business=FarmEnterprises.businessId(town);
        var founding=firms.findFounding(business);
        if(founding.isEmpty())return "No saved farm business; no farm experience awarded";
        var skill=policy.industrySkills().get("farming");var awards=new ArrayList<IndustryLearning.Entry>();
        String reason;
        if(!policy.enabled())reason="Farm work learning disabled";
        else if(skill==null)reason="No configured farming skill; no experience awarded";
        else if(output==0||fieldSlots==0)reason="No actual farm production; no experience awarded";
        else if(BusinessClosure.blocked(connection,business))throw new SQLException("Closed farm has positive production");
        else {
            var jobs=new ArrayList<>(firms.currentWorkers(business));
            jobs.sort(Comparator.comparing(job->job.residentId().toString()));
            if(jobs.size()>policy.maximumWorkersPerReview())reason="Farm learning deferred: worker budget exceeded; no partial awards";
            else {
                var people=new PopulationRepository(connection);var requirements=founding.orElseThrow().workerRequirements();
                int count=(int)Math.min(fieldSlots,jobs.size());
                // Field loss can leave more saved employees than productive slots. Rotate only those slots.
                int offset=jobs.isEmpty()?0:(int)Math.floorMod(period-1,(long)jobs.size());
                for(int i=0;i<count;i++) {
                    var person=people.findResident(jobs.get((offset+i)%jobs.size()).residentId()).orElseThrow();
                    var profile=people.findWorkProfile(person.id()).orElseThrow();
                    if(!person.settlementId().equals(town)||!WorkEligibility.evaluate(person,profile,requirements).eligible())continue;
                    long before=profile.experience().getOrDefault(skill,0L),after=policy.nextExperience(before);
                    if(after>before)awards.add(new IndustryLearning.Entry(person.id(),before,after));
                }
                reason="Actual farm production: "+output+" food units; "+count+" productive worker slots; "+awards.size()+" employees learned "+skill+"; no extra labor or ownership waiver";
            }
        }
        try(var s=connection.prepareStatement("INSERT INTO farm_learning_reviews(settlement_id,period,commodity,business_id,field_slots,production,enabled,skill,gain,ceiling,worker_budget,explanation) VALUES(?,?,'food',?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,town.toString());s.setLong(2,period);s.setString(3,business.toString());s.setLong(4,fieldSlots);s.setLong(5,output);s.setInt(6,policy.enabled()?1:0);
            if(skill==null)s.setNull(7,Types.VARCHAR);else s.setString(7,skill);
            s.setLong(8,policy.experiencePerProductivePeriod());s.setLong(9,policy.maximumExperience());s.setInt(10,policy.maximumWorkersPerReview());s.setString(11,reason);s.executeUpdate();
        }
        var people=new PopulationRepository(connection);
        for(var award:awards) {
            people.recordSkillExperience(award.resident(),skill,award.after());
            try(var s=connection.prepareStatement("INSERT INTO farm_learning_entries VALUES(?,?,?,?,?)")) {
                s.setString(1,town.toString());s.setLong(2,period);s.setString(3,award.resident().toString());s.setLong(4,award.before());s.setLong(5,award.after());s.executeUpdate();
            }
        }
        return reason;
    }
    public Optional<String> explanation(UUID town,long period)throws SQLException {
        try(var s=connection.prepareStatement("SELECT explanation FROM farm_learning_reviews WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){return r.next()?Optional.of(r.getString(1)):Optional.empty();}
        }
    }
}
