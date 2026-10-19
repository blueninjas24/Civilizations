package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.BusinessStaffingPolicy;
import com.blueninjas24.civilizations.population.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Owner-thread bounded local hiring. Saved employment/audits commit together, never poaching a current worker. */
public final class AutomaticBusinessStaffing {
    private final Connection connection;
    private final BusinessStaffingPolicy policy;
    private final LinkedHashMap<UUID,String> cursors=new LinkedHashMap<>();
    public AutomaticBusinessStaffing(Connection connection,BusinessStaffingPolicy policy) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);
    }
    public String review(UUID business,long step)throws SQLException {
        return review(business,step,reason->{},false);
    }
    @FunctionalInterface interface ReviewAudit {void save(String reason)throws SQLException;}
    String review(UUID business,long step,ReviewAudit audit)throws SQLException {
        return review(business,step,audit,true);
    }
    private String review(UUID business,long step,ReviewAudit audit,boolean reserveStudents)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Automatic hiring requires idle SQL owner");
        if(step<0)throw new IllegalArgumentException("Negative hiring step");
        if(!policy.enabled())return "Automatic vacancy hiring disabled";
        connection.setAutoCommit(false);
        String nextCursor=null;
        try {
            var businesses=new BusinessRepository(connection);var population=new PopulationRepository(connection);
            var b=businesses.findBusiness(business).orElseThrow(()->new SQLException("Unknown staffing business"));
            var owner=population.findResident(b.ownerId()).orElseThrow();
            String reason;
            if(!policy.industries().contains(b.industry()))reason="Industry outside automatic hiring policy";
            else if(step<b.foundedStep())reason="Hiring cannot predate saved founding";
            else if(BusinessClosure.blocked(connection,business))reason="Closed or sale deadline expired; no hires";
            else if(owner.status()!=Resident.Status.ALIVE||!owner.settlementId().equals(b.settlementId()))reason="Owner unavailable; existing staff only until succession/sale";
            else {
                int assigned;
                try(var s=connection.prepareStatement("SELECT COUNT(*) FROM business_employment WHERE business_id=? AND ended_step IS NULL")) {
                    s.setString(1,business.toString());try(var r=s.executeQuery()){assigned=r.getInt(1);}
                }
                int vacancies=b.workerCapacity()-assigned;
                if(vacancies<=0)reason="No saved worker vacancies";
                else {
                    var requirements=businesses.findFounding(business).orElseThrow().workerRequirements();
                    String cursor=cursors.getOrDefault(business,"");
                    var candidates=page(b.settlementId(),cursor,reserveStudents);
                    if(candidates.isEmpty()&&!cursor.isEmpty())candidates=page(b.settlementId(),"",reserveStudents);
                    int hired=0;
                    for(var candidate:candidates) {
                        nextCursor=candidate.toString();
                        var person=population.findResident(candidate).orElseThrow();
                        if(!WorkEligibility.evaluate(person,population.findWorkProfile(candidate).orElseThrow(),requirements).eligible())continue;
                        long episodes;
                        try(var s=connection.prepareStatement("SELECT COUNT(*) FROM business_employment WHERE business_id=? AND resident_id=?")) {
                            s.setString(1,business.toString());s.setString(2,candidate.toString());try(var r=s.executeQuery()){episodes=r.getLong(1);}
                        }
                        var operation=UUID.nameUUIDFromBytes(("civilizations:auto-hire-v1:"+business+":"+candidate+":"+step+":"+episodes).getBytes(StandardCharsets.UTF_8));
                        businesses.hireRows(operation,business,candidate,step);
                        try(var s=connection.prepareStatement("INSERT INTO business_hiring_reviews VALUES(?,?,?)")) {
                            s.setString(1,operation.toString());s.setString(2,policy.toString());
                            s.setString(3,"Living local owner "+b.ownerId()+"; qualified unemployed local resident fills actual vacancy; requirements "+requirements);s.executeUpdate();
                        }
                        if(++hired>=Math.min(vacancies,policy.hiresPerReview()))break;
                    }
                    reason="Vacancy hiring: "+hired+" qualified local residents; bounded candidate page "+candidates.size()+"; no poaching, training, wages or new business";
                    if(nextCursor==null)nextCursor="";
                }
            }
            audit.save(reason);
            connection.commit();connection.setAutoCommit(true);
            if(nextCursor!=null){cursors.remove(business);cursors.put(business,nextCursor);while(cursors.size()>4096)cursors.remove(cursors.keySet().iterator().next());}
            return reason;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private List<UUID> page(UUID town,String cursor,boolean reserveStudents)throws SQLException {
        var result=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.id>?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND (?=0 OR NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE'))
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setString(2,cursor);s.setInt(3,reserveStudents?1:0);s.setInt(4,policy.candidatesPerReview());
            try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }
        return result;
    }
}
