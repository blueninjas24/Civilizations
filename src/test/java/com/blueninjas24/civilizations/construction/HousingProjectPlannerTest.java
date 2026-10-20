package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.HousingProjectPlanner.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import com.blueninjas24.civilizations.population.Household;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HousingProjectPlannerTest {
    final UUID town=new UUID(0,1),world=new UUID(0,2);
    ConstructionTemplate home()throws Exception{return ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));}
    Need family(int id,int members){return new Need(new Household(new UUID(0,id),town,"Fixture household"),members,false);}
    Plan review(List<Need> families,List<Vacancy> homes,Set<UUID> pending,boolean complete,boolean active)throws Exception {
        return HousingProjectPlanner.prototype().review(town,world,families,homes,List.of(home()),pending,complete,active,42,1);
    }
    @Test void suitableVacanciesAreReusedOnceBeforeNewWholeHouseholdProposalsRegardlessOfInputOrder()throws Exception {
        var a=family(10,2);var b=family(11,2);var vacancy=new Vacancy(new UUID(0,20),town,world,4,true,true,false);
        var result=review(List.of(a,b),List.of(vacancy),Set.of(),true,true);
        assertEquals(result,review(List.of(b,a),List.of(vacancy),Set.of(),true,true));
        assertEquals(1,result.recommendations().stream().filter(r->r.action()==Action.REUSE).count());
        assertEquals(1,result.recommendations().stream().filter(r->r.action()==Action.PROPOSE).count());
        var proposal=result.recommendations().stream().filter(r->r.action()==Action.PROPOSE).findFirst().orElseThrow();
        assertEquals(home().fingerprint(),proposal.fingerprint());assertThrows(UnsupportedOperationException.class,()->result.recommendations().clear());
    }
    @Test void incompleteSurveyAbsentWorldAndExistingProjectsCannotCreateDuplicateNewHomes()throws Exception {
        var family=family(10,2);
        assertEquals(Action.WAIT_SURVEY,review(List.of(family),List.of(),Set.of(),false,true).recommendations().getFirst().action());
        assertEquals(Action.WAIT_WORLD,review(List.of(family),List.of(),Set.of(),true,false).recommendations().getFirst().action());
        assertEquals(Action.WAIT_PROJECT,review(List.of(family),List.of(),Set.of(family.household().id()),true,true).recommendations().getFirst().action());
        var capped=review(List.of(family,family(11,2)),List.of(),Set.of(),true,true);
        assertEquals(1,capped.recommendations().stream().filter(r->r.action()==Action.PROPOSE).count());
        assertEquals(1,capped.recommendations().stream().filter(r->r.action()==Action.WAIT_CAP).count());
    }
    @Test void privateWrongWorldReservedOrTooSmallHomesNeverCountAndLargeFamiliesAreNotSplit()throws Exception {
        var family=family(10,5);
        var homes=List.of(new Vacancy(new UUID(0,20),town,world,8,false,true,false),
                new Vacancy(new UUID(0,21),town,UUID.randomUUID(),8,true,true,false),
                new Vacancy(new UUID(0,22),town,world,8,true,true,true),new Vacancy(new UUID(0,23),town,world,4,true,true,false));
        assertEquals(Action.WAIT_PLAN,review(List.of(family),homes,Set.of(),true,true).recommendations().getFirst().action());
        assertEquals(Action.NO_LIVING_MEMBERS,review(List.of(family(11,0)),List.of(),Set.of(),true,true).recommendations().getFirst().action());
    }
    @Test void capacityRequiresCompleteRealBedPairsAndRejectsDuplicateOrMissingAnchors()throws Exception {
        var source=home();assertEquals(4,HomePlanCapacity.beds(source));
        var anchors=new ArrayList<>(source.anchors());anchors.add(new Anchor(Role.BED,anchors.getLast().at(),Direction.NORTH));
        var duplicate=new ConstructionTemplate(source.id(),1,source.name(),source.attribution(),source.purpose(),source.size(),source.stages(),anchors);
        assertThrows(IllegalArgumentException.class,()->HomePlanCapacity.beds(duplicate));
        var stages=new ArrayList<>(source.stages());var last=stages.removeLast();var blocks=new TreeMap<>(last.blocks());blocks.remove(new Position(1,1,3));
        stages.add(new Stage(last.id(),last.work(),last.materials(),blocks));
        var missing=new ConstructionTemplate(source.id(),1,source.name(),source.attribution(),source.purpose(),source.size(),stages,source.anchors());
        assertThrows(IllegalArgumentException.class,()->HomePlanCapacity.beds(missing));
    }
}
