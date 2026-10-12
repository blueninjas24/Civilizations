package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.StudyPolicy;
import com.blueninjas24.civilizations.population.Resident;
import java.sql.*;
import java.util.*;

/** Supply-backed adult study composed with the actual village food-period transaction. */
public final class ResidentStudy {
    public record Course(UUID id,UUID resident,UUID settlement,String skill,long target,long gain,long eligibleAfter,String status,Map<Commodity,Long> materials) {
        public Course {materials=Map.copyOf(materials);}
    }
    record Review(Course course,long before,long after,String status,String reason) {}
    record Plan(List<Review> reviews,Map<Commodity,Flow> flows) {
        Plan {reviews=List.copyOf(reviews);flows=Map.copyOf(flows);}
    }
    private final Connection connection;
    private final StudyPolicy policy;
    private final long adulthood;
    @FunctionalInterface interface OpportunityCheck {boolean available(UUID course)throws SQLException;}
    private OpportunityCheck founderOpportunity=course->true;
    ResidentStudy withFounderOpportunity(OpportunityCheck check){founderOpportunity=Objects.requireNonNull(check);return this;}
    enum FounderState {READY,WAIT,CANCEL}
    @FunctionalInterface interface FounderCheck {FounderState state(UUID course)throws SQLException;}
    private FounderCheck founderPractice=course->FounderState.READY;
    ResidentStudy withFounderPractice(FounderCheck check){founderPractice=Objects.requireNonNull(check);return this;}
    public ResidentStudy(Connection connection,StudyPolicy policy,long adulthood) {
        this.connection=Objects.requireNonNull(connection);this.policy=Objects.requireNonNull(policy);this.adulthood=adulthood;
        if(adulthood<1)throw new IllegalArgumentException("Invalid study adulthood");
    }
    /** Trusted opportunity caller; never invents industry demand or chooses a career itself. */
    Course enrollRows(UUID id,UUID resident,UUID town,String skill,long eligibleAfter)throws SQLException {
        requireTransaction();
        var old=find(id);if(old.isPresent()) {
            var saved=old.orElseThrow();
            if(!saved.resident().equals(resident)||!saved.settlement().equals(town)||!saved.skill().equals(skill))
                throw new SQLException("Study identity reused with different participants");
            return saved;
        }
        var terms=policy.skills().get(skill);
        if(!policy.enabled()||terms==null)throw new SQLException("Study skill disabled or unknown");
        if(eligibleAfter<0||eligibleAfter==Long.MAX_VALUE)throw new SQLException("No future study period");
        var person=new PopulationRepository(connection).findResident(resident).orElseThrow(()->new SQLException("Unknown learner"));
        if(!eligible(person,town))throw new SQLException("Study requires a living local unemployed adult without an operating business");
        long before=experience(resident,skill);if(before>=terms.targetExperience())throw new SQLException("Study target already reached");
        var account=new CommodityRepository(connection).read(town).orElseThrow(()->new SQLException("No village supply account"));
        if(!account.state().stocks().keySet().containsAll(terms.materials().keySet()))throw new SQLException("Practice supplies missing from village catalog");
        if(active(town).size()>=policy.activeCoursesPerVillage())throw new SQLException("Study enrollment budget reached");
        try(var s=connection.prepareStatement("INSERT INTO resident_courses(id,resident_id,settlement_id,skill,target,gain,eligible_after,status) VALUES(?,?,?,?,?,?,?,'ACTIVE')")) {
            s.setString(1,id.toString());s.setString(2,resident.toString());s.setString(3,town.toString());s.setString(4,skill);
            s.setLong(5,terms.targetExperience());s.setLong(6,policy.gainPerPeriod());s.setLong(7,eligibleAfter);s.executeUpdate();
        }
        try(var s=connection.prepareStatement("INSERT INTO study_materials(course_id,commodity,units) VALUES(?,?,?)")) {
            for(var e:terms.materials().entrySet()){s.setString(1,id.toString());s.setString(2,e.getKey().id());s.setLong(3,e.getValue());s.executeUpdate();}
        }
        return find(id).orElseThrow();
    }
    Plan planRows(UUID town,long period,State before,Map<Commodity,Flow> initial,boolean laborAvailable)throws SQLException {
        requireTransaction();var flows=new TreeMap<>(initial);var reviews=new ArrayList<Review>();var courses=active(town);
        if(courses.size()>policy.activeCoursesPerVillage())return new Plan(List.of(),flows); // Retuning never partially erases courses.
        if(!courses.isEmpty())Collections.rotate(courses,-(int)Math.floorMod(period-1,courses.size()));
        var food=flows.get(Commodity.FOOD);long demand=food.demand();
        long available=Math.addExact(before.stocks().get(Commodity.FOOD).units(),food.incoming());
        boolean foodSafe=available>=demand&&available-demand>=Math.multiplyExact(demand,policy.foodReservePeriods());
        int chosen=0;
        for(var course:courses) {
            if(period<=course.eligibleAfter())continue;
            long experience=experience(course.resident(),course.skill()),after=experience;String status="ACTIVE",reason;
            var person=new PopulationRepository(connection).findResident(course.resident()).orElseThrow();
            var founderState=founderPractice.state(course.id());
            if(!eligible(person,town)){status="CANCELLED";reason="Resident no longer a living local unemployed adult";}
            else if(!AutomaticCareerStudy.opportunityAvailable(connection,course.id())){status="CANCELLED";reason="Linked business vacancy no longer available; practice supplies retained";}
            else if(!founderOpportunity.available(course.id())){status="CANCELLED";reason="Linked mining founder opportunity no longer viable; practice supplies retained";}
            else if(experience>=course.target()){status="COMPLETED";reason="Target already reached; no supplies spent";}
            else if(founderState==FounderState.CANCEL){status="CANCELLED";reason="First-workshop opportunity already taken; practice supplies retained";}
            else if(founderState==FounderState.WAIT)reason="First-workshop opportunity unavailable or unknown; study paused, supplies retained";
            else if(!policy.enabled()||!policy.skills().containsKey(course.skill()))reason="Study disabled; supplies retained";
            else if(!laborAvailable)reason="Legacy field output counts all adults; study deferred to avoid double labor";
            else if(!foodSafe)reason="Food needs/reserve take priority; study paused";
            else if(chosen>=policy.studentsPerPeriod())reason="Student budget reached; study deferred";
            else if(experience>=policy.maximumExperience())reason="Current experience ceiling reached; supplies retained";
            else {
                boolean supplied=true;
                for(var e:course.materials().entrySet()) {
                    var stock=before.stocks().get(e.getKey());var flow=flows.get(e.getKey());
                    if(stock==null||flow==null){supplied=false;break;}
                    long remaining=Math.addExact(stock.units(),flow.incoming())-Math.min(Math.addExact(stock.units(),flow.incoming()),flow.demand());
                    if(remaining<e.getValue()||remaining-e.getValue()<policy.materialReserveUnits()){supplied=false;break;}
                }
                if(!supplied)reason="Actual practice supplies/reserve insufficient; study paused";
                else {
                    after=experience+Math.min(course.gain(),Math.min(course.target(),policy.maximumExperience())-experience);
                    for(var e:course.materials().entrySet()) {
                        var f=flows.get(e.getKey());flows.put(e.getKey(),new Flow(f.production(),f.imports(),f.households(),f.industry(),f.construction(),f.exports(),Math.addExact(f.events(),e.getValue())));
                    }
                    chosen++;if(after==course.target())status="COMPLETED";
                    reason="Actual village practice supplies consumed; gradual "+course.skill()+" study";
                }
            }
            reviews.add(new Review(course,experience,after,status,reason));
        }
        return new Plan(reviews,flows);
    }
    void applyRows(UUID town,long period,Plan plan,Result result)throws SQLException {
        requireTransaction();
        for(var r:result.reports())if(r.flow().events()>0&&r.unmet()>0)throw new SQLException("Study supply consumption was not fulfilled");
        for(var review:plan.reviews()) {
            var c=review.course();
            if(review.after()>review.before())new PopulationRepository(connection).recordSkillExperience(c.resident(),c.skill(),review.after());
            try(var s=connection.prepareStatement("INSERT INTO study_reviews(course_id,settlement_id,period,before_experience,after_experience,status,reason,food_reserve,material_reserve,student_budget) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,c.id().toString());s.setString(2,town.toString());s.setLong(3,period);s.setLong(4,review.before());s.setLong(5,review.after());s.setString(6,review.status());s.setString(7,review.reason());s.setInt(8,policy.foodReservePeriods());s.setLong(9,policy.materialReserveUnits());s.setInt(10,policy.studentsPerPeriod());s.executeUpdate();
            }
            if(review.after()>review.before())try(var s=connection.prepareStatement("INSERT INTO study_spending(course_id,period,commodity,units) VALUES(?,?,?,?)")) {
                for(var e:c.materials().entrySet()){s.setString(1,c.id().toString());s.setLong(2,period);s.setString(3,e.getKey().id());s.setLong(4,e.getValue());s.executeUpdate();}
            }
            try(var s=connection.prepareStatement("UPDATE resident_courses SET status=? WHERE id=? AND status='ACTIVE'")) {
                s.setString(1,review.status());s.setString(2,c.id().toString());if(s.executeUpdate()!=1)throw new SQLException("Study course disappeared");
            }
        }
    }
    public Optional<Course> find(UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM resident_courses WHERE id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();
                var materials=new TreeMap<Commodity,Long>();try(var m=connection.prepareStatement("SELECT commodity,units FROM study_materials WHERE course_id=? ORDER BY commodity")) {
                    m.setString(1,id.toString());try(var rows=m.executeQuery()){while(rows.next())materials.put(new Commodity(rows.getString(1)),rows.getLong(2));}
                }
                return Optional.of(new Course(id,UUID.fromString(r.getString("resident_id")),UUID.fromString(r.getString("settlement_id")),r.getString("skill"),r.getLong("target"),r.getLong("gain"),r.getLong("eligible_after"),r.getString("status"),materials));
            }
        }
    }
    public String summary(UUID town)throws SQLException {
        int active=active(town).size();
        if(active>policy.activeCoursesPerVillage())return "Study deferred: "+active+" saved courses exceed current village budget; no partial awards";
        String reason="awaiting an eligible future food period";
        try(var s=connection.prepareStatement("SELECT reason FROM study_reviews WHERE settlement_id=? ORDER BY period DESC,course_id LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())reason=r.getString(1);}
        }
        return "Adult study: "+(policy.enabled()?"enabled":"disabled")+"; active courses "+active+"; latest review: "+reason;
    }
    private ArrayList<Course> active(UUID town)throws SQLException {
        var courses=new ArrayList<Course>();try(var s=connection.prepareStatement("SELECT id FROM resident_courses WHERE settlement_id=? AND status='ACTIVE' ORDER BY id LIMIT 33")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){while(r.next())courses.add(find(UUID.fromString(r.getString(1))).orElseThrow());}
        }
        return courses;
    }
    private long experience(UUID id,String skill)throws SQLException {return new PopulationRepository(connection).findWorkProfile(id).orElseThrow().experience().getOrDefault(skill,0L);}
    private boolean eligible(Resident person,UUID town)throws SQLException {
        if(person.status()!=Resident.Status.ALIVE||person.ageSteps()<adulthood||!person.settlementId().equals(town))return false;
        try(var s=connection.prepareStatement("SELECT 1 FROM business_employment WHERE resident_id=? AND ended_step IS NULL UNION ALL SELECT 1 FROM businesses b WHERE b.owner_id=? AND NOT EXISTS(SELECT 1 FROM business_closures c WHERE c.business_id=b.id) LIMIT 1")) {
            s.setString(1,person.id().toString());s.setString(2,person.id().toString());try(var r=s.executeQuery()){return !r.next();}
        }
    }
    private void requireTransaction()throws SQLException {if(connection.getAutoCommit())throw new SQLException("Study requires village owner transaction");}
}
