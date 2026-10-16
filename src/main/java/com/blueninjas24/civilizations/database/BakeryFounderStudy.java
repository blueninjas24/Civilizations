package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** One prospective baker for an actual first-bakery opportunity, using future real practice supplies. */
final class BakeryFounderStudy {
    private final Connection connection;
    private final BakeryOpportunities opportunities;
    private final BakeryFoundingPolicy founding;
    private final CareerStudyPolicy careers;
    private final StudyPolicy study;
    private final long adulthood;
    BakeryFounderStudy(Connection connection,BakeryOpportunities opportunities,BakeryFoundingPolicy founding,CareerStudyPolicy careers,StudyPolicy study,long adulthood) {
        this.connection=connection;this.opportunities=opportunities;this.founding=founding;this.careers=careers;this.study=study;this.adulthood=adulthood;
    }
    private boolean enabled(){return founding.enabled()&&careers.enabled()&&careers.industries().contains("bakery")&&study.enabled();}
    String reviewRows(UUID town,long now,Set<UUID> worlds,long period,long eligibleAfter)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Bakery founder study requires economy owner transaction");
        if(!enabled())return "Automatic bakery founder study disabled";
        var terms=study.skills().get("baking");
        if(terms==null||terms.targetExperience()<founding.founderExperience())return "Baking study target cannot qualify a bakery founder";
        var review=opportunities.reviewRows(town,now,worlds==null?Set.of():worlds);
        if(review.candidates().isEmpty())return "Bakery founder study waits: "+review.explanation();
        String after="";
        try(var s=connection.prepareStatement("SELECT explanation FROM bakery_founder_reviews WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){if(r.next())return r.getString(1);}
        }
        try(var s=connection.prepareStatement("SELECT after_resident FROM bakery_founder_reviews WHERE settlement_id=? ORDER BY period DESC LIMIT 1")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()){if(r.next())after=r.getString(1);}
        }
        String reason;
        var account=new CommodityRepository(connection).read(town).orElseThrow();
        boolean supplied=terms.materials().entrySet().stream().allMatch(e->{var stock=account.state().stocks().get(e.getKey());return stock!=null&&stock.units()>=Math.addExact(e.getValue(),study.materialReserveUnits());});
        if(count("SELECT COUNT(*) FROM bakery_founder_courses f JOIN resident_courses c ON c.id=f.course_id WHERE f.settlement_id=? AND c.status='ACTIVE'",town)>0)
            reason="One prospective bakery owner already studying; no duplicate sponsor";
        else if(count("SELECT COUNT(*) FROM resident_courses WHERE settlement_id=? AND status='ACTIVE'",town)>=study.activeCoursesPerVillage())reason="Village study enrollment budget reached";
        else if(!supplied)reason="Bakery founder study waits for actual practice supplies and reserve";
        else {
            var candidates=page(town,after);if(candidates.isEmpty()&&!after.isEmpty())candidates=page(town,"");
            if(candidates.isEmpty())reason="No eligible unemployed adult in bounded bakery-founder page";
            else {
                UUID resident=candidates.getFirst();after=resident.toString();var marker=review.candidates().getFirst().workplace();var p=marker.position();
                UUID course=UUID.nameUUIDFromBytes(("civilizations:bakery-founder-study-v1:"+town+":"+marker.world()+":"+p+":"+resident+":"+period).getBytes(StandardCharsets.UTF_8));
                new ResidentStudy(connection,study,adulthood).enrollRows(course,resident,town,"baking",eligibleAfter);
                try(var s=connection.prepareStatement("INSERT INTO bakery_founder_courses VALUES(?,?,?,?,?,?,?)")) {
                    s.setString(1,course.toString());s.setString(2,town.toString());s.setString(3,marker.world().toString());s.setInt(4,p.x());s.setInt(5,p.y());s.setInt(6,p.z());s.setLong(7,period);s.executeUpdate();
                }
                reason="One prospective bakery owner enrolled for real paid food demand, observed smoker and protected funding; future supply-backed study only";
            }
        }
        try(var s=connection.prepareStatement("INSERT INTO bakery_founder_reviews VALUES(?,?,?,?,?)")) {
            s.setString(1,town.toString());s.setLong(2,period);s.setString(3,after);s.setString(4,founding+"; "+careers+"; "+study);s.setString(5,reason);s.executeUpdate();
        }
        return reason;
    }
    ResidentStudy.FounderState stateRows(UUID course,long now,Set<UUID> worlds)throws SQLException {
        UUID town,world;Position position;
        try(var s=connection.prepareStatement("SELECT * FROM bakery_founder_courses WHERE course_id=?")) {
            s.setString(1,course.toString());try(var r=s.executeQuery()) {
                if(!r.next())return ResidentStudy.FounderState.READY;
                town=UUID.fromString(r.getString("settlement_id"));world=UUID.fromString(r.getString("world_id"));position=new Position(r.getInt("x"),r.getInt("y"),r.getInt("z"));
            }
        }
        // A taken first-firm opportunity releases the learner; unknown evidence merely waits.
        if(count("SELECT COUNT(*) FROM businesses WHERE settlement_id=? AND industry='bakery'",town)>0)return ResidentStudy.FounderState.CANCEL;
        if(!enabled())return ResidentStudy.FounderState.WAIT;
        var terms=study.skills().get("baking");if(terms==null||terms.targetExperience()<founding.founderExperience())return ResidentStudy.FounderState.WAIT;
        return opportunities.reviewForTrainingRows(town,now,worlds==null?Set.of():worlds).candidates().stream()
                .anyMatch(c->c.workplace().world().equals(world)&&c.workplace().position().equals(position))?ResidentStudy.FounderState.READY:ResidentStudy.FounderState.WAIT;
    }
    private long count(String sql,UUID town)throws SQLException {
        try(var s=connection.prepareStatement(sql)){s.setString(1,town.toString());try(var r=s.executeQuery()){return r.getLong(1);}}
    }
    private List<UUID> page(UUID town,String after)throws SQLException {
        var result=new ArrayList<UUID>();try(var s=connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=? AND r.id>?
              AND COALESCE((SELECT experience FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='baking'),0)<?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
              AND NOT EXISTS(SELECT 1 FROM businesses b WHERE b.owner_id=r.id AND NOT EXISTS(SELECT 1 FROM business_closures x WHERE x.business_id=b.id))
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setLong(2,Math.max(adulthood,founding.minimumAgeSteps()));s.setString(3,after);s.setLong(4,founding.founderExperience());s.setInt(5,careers.candidatesPerReview());
            try(var r=s.executeQuery()){while(r.next())result.add(UUID.fromString(r.getString(1)));}
        }return result;
    }
}
