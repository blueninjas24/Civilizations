package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Read-only bounded work feed for loaded-world home-site surveys. No family, draft, site or funds created. */
public final class HousingPlanningJobs {
    public record Job(UUID proposal,UUID household,UUID village,UUID world,Position need,int templateHeight){
        public Job(UUID proposal,UUID household,UUID village,UUID world,Position need){this(proposal,household,village,world,need,4);}
        public Job{if(templateHeight<1||templateHeight>64)throw new IllegalArgumentException("Invalid selected home height");}
    }
    public record Page(List<Job> jobs,UUID next,int reviewed,int absent,int projectHolds){public Page{jobs=List.copyOf(jobs);}}
    private record Village(UUID id,UUID world,Position at){}
    private final Connection c;
    public HousingPlanningJobs(Connection c){this.c=Objects.requireNonNull(c);}
    public Page page(UUID after,int limit,Set<UUID> worlds,List<ConstructionTemplate> templates,HousingProjectPlanner planner,
                     DwellingInspection.Policy policy,long now,long seed,long period,int maximumActive)throws SQLException{
        worlds=Set.copyOf(worlds);templates=List.copyOf(templates);Objects.requireNonNull(planner);Objects.requireNonNull(policy);
        if(limit<1||limit>8||maximumActive<1||maximumActive>64||now<0||period<0)throw new IllegalArgumentException("Invalid home planning page limits");
        if(!c.getAutoCommit())throw new SQLException("Home planning feed requires an idle SQL owner");c.setAutoCommit(false);
        try{
            var villages=new ArrayList<Village>();
            try(var s=c.prepareStatement("SELECT s.id,w.world_id,s.center_x,s.center_y,s.center_z FROM settlements s JOIN settlement_worlds w ON w.settlement_id=s.id WHERE (? IS NULL OR s.id>?) ORDER BY s.id LIMIT ?")){
                s.setString(1,after==null?null:after.toString());s.setString(2,after==null?null:after.toString());s.setInt(3,limit+1);
                try(var r=s.executeQuery()){while(r.next())villages.add(new Village(UUID.fromString(r.getString(1)),UUID.fromString(r.getString(2)),new Position(r.getInt(3),r.getInt(4),r.getInt(5))));}
            }
            var jobs=new ArrayList<Job>();int absent=0,held=0,reviewed=0;UUID cursor=null;
            for(var village:villages.stream().limit(limit).toList()){
                reviewed++;cursor=village.id();
                if(!worlds.contains(village.world())){absent++;continue;}
                if(new HousingProjects(c).activeCount(village.id(),village.world())>=maximumActive){held++;continue;}
                var report=new HousingPlanning(c).review(village.id(),new DwellingOccupancy.Context(village.world(),true,policy,now),templates,planner,seed,period);
                var recommendation=report.plan().recommendations().stream().filter(r->r.action()==HousingProjectPlanner.Action.PROPOSE).findFirst();
                if(recommendation.isEmpty())continue;UUID household=recommendation.orElseThrow().household();long previous;
                try(var s=c.prepareStatement("SELECT count(*) FROM housing_project_bindings WHERE household_id=?")){
                    s.setString(1,household.toString());try(var r=s.executeQuery()){previous=r.getLong(1);}
                }
                UUID proposal=UUID.nameUUIDFromBytes(("Civilizations:home-proposal:"+village.world()+":"+household+":"+period+":"+previous).getBytes(StandardCharsets.UTF_8));
                var choice=recommendation.orElseThrow();
                int height=templates.stream().filter(t->t.id().equals(choice.templateId())&&t.fingerprint().equals(choice.fingerprint())).findFirst()
                        .orElseThrow(()->new SQLException("Recommended home differs from selected catalogue")).size().y();
                jobs.add(new Job(proposal,household,village.id(),village.world(),village.at(),height));
            }
            c.commit();c.setAutoCommit(true);return new Page(jobs,villages.size()>limit?cursor:null,reviewed,absent,held);
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
