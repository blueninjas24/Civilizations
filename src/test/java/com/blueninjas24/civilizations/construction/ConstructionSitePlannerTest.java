package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionSitePlanner.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionSitePlannerTest {
    private final UUID world=UUID.randomUUID();
    private final Bounds area=new Bounds(new Position(-20,63,-20),new Position(20,70,20));
    private ConstructionTemplate template()throws Exception{return ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_storehouse.yml"));}
    private Survey survey(boolean complete,boolean protectionsComplete,List<Bounds> protections,List<Bounds> entities) {
        var cells=new HashMap<Position,Cell>();
        for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)for(int y=63;y<=68;y++)cells.put(new Position(x,y,z),new Cell(y>63,y==63,y==63&&x==0&&z==0,false));
        return new Survey(world,1000,complete,protectionsComplete,cells,protections,entities);
    }
    private Result select(Survey survey,ApprovedArea permission)throws Exception{return ConstructionSitePlanner.select(template(),world,new Position(8,64,0),survey,permission,1100,1000,42);}
    private Map<Position,ConstructionLandPolicy.Kind> terrain(Survey survey){
        var result=new HashMap<Position,ConstructionLandPolicy.Kind>();survey.cells().forEach((p,c)->result.put(p,c.road()?ConstructionLandPolicy.Kind.ROAD:c.air()?ConstructionLandPolicy.Kind.AIR:ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND));return result;
    }
    private Result buffered(Survey survey,Map<Position,ConstructionLandPolicy.Kind> terrain)throws Exception {
        return ConstructionSitePlanner.select(template(),world,new Position(8,64,0),survey,new ApprovedArea(new UUID(0,1),world,area),1100,1000,42,new ConstructionLandPolicy(4),terrain);
    }
    @Test void bufferedSearchScreensEveryOrientationAndRejectsNeighborsBeforeChoosingAlternatives()throws Exception {
        var clean=survey(true,true,List.of(),List.of());var result=buffered(clean,terrain(clean));
        assertEquals(Status.READY,result.status());assertEquals(8,result.reviewed().size());
        assertTrue(result.reviewed().stream().allMatch(c->c.landFindings().isEmpty()));
        var first=result.selected().orElseThrow();var at=first.origin().plus(new Position(-4,0,0));
        var cells=new HashMap<>(clean.cells());cells.put(at,new Cell(false,false,false,false));
        var kinds=terrain(clean);kinds.put(at,ConstructionLandPolicy.Kind.OTHER);
        var changed=buffered(new Survey(world,1000,true,true,cells,List.of(),List.of()),kinds);
        var rejected=changed.reviewed().stream().filter(c->c.origin().equals(first.origin())&&c.transform().equals(first.transform())).findFirst().orElseThrow();
        assertTrue(rejected.landFindings().contains(ConstructionLandPolicy.Finding.NEARBY_OBSTACLE));assertFalse(rejected.assessment().suitable());
        assertTrue(changed.selected().isEmpty()||!changed.selected().orElseThrow().origin().equals(first.origin())||!changed.selected().orElseThrow().transform().equals(first.transform()));
        assertEquals(changed,buffered(new Survey(world,1000,true,true,cells,List.of(),List.of()),new TreeMap<>(kinds)));
    }
    @Test void missingContradictoryAndProtectedBufferEvidenceNeverCountsAsAvailableLand()throws Exception {
        var clean=survey(true,true,List.of(),List.of());
        assertEquals(Status.NO_SAFE_SITE,buffered(clean,Map.of()).status());
        var contradictory=terrain(clean);contradictory.replaceAll((p,k)->ConstructionLandPolicy.Kind.AIR);
        assertEquals(Status.NO_SAFE_SITE,buffered(clean,contradictory).status());
        var protectedAll=new Survey(world,1000,true,true,clean.cells(),List.of(new Bounds(new Position(-20,64,-20),area.maximum())),List.of());
        assertEquals(Status.NO_SAFE_SITE,buffered(protectedAll,terrain(clean)).status());
        assertTrue(buffered(protectedAll,terrain(clean)).reviewed().stream().allMatch(c->c.landFindings().contains(ConstructionLandPolicy.Finding.PROTECTED)));
    }
    @Test void roadFirstSelectionIsDeterministicAcrossInputOrderingAndCannotInventARoad()throws Exception {
        var survey=survey(true,true,List.of(),List.of());var approval=new ApprovedArea(UUID.randomUUID(),world,area);
        var result=select(survey,approval);assertEquals(Status.READY,result.status());assertEquals(8,result.reviewed().size());
        var chosen=result.selected().orElseThrow();assertTrue(chosen.assessment().suitable());
        var road=template().anchors().stream().filter(a->a.role()==Role.ROAD).findFirst().orElseThrow();
        assertEquals(new Position(0,63,0),chosen.origin().plus(chosen.transform().apply(road.at(),template().size())));
        var entries=new ArrayList<>(survey.cells().entrySet());Collections.reverse(entries);var reversed=new LinkedHashMap<Position,Cell>();entries.forEach(e->reversed.put(e.getKey(),e.getValue()));
        assertEquals(result,select(new Survey(world,1000,true,true,reversed,List.of(),List.of()),approval));
        var noRoad=new HashMap<>(survey.cells());noRoad.put(new Position(0,63,0),new Cell(false,true,false,false));
        assertEquals(Status.NO_ROAD,select(new Survey(world,1000,true,true,noRoad,List.of(),List.of()),approval).status());
    }
    @Test void missingPermissionOrIncompleteWorldProtectionAndTimeEvidenceBlocksSearch()throws Exception {
        var approval=new ApprovedArea(UUID.randomUUID(),world,area);var clean=survey(true,true,List.of(),List.of());
        assertEquals(Status.NO_PERMISSION,select(clean,null).status());
        assertEquals(Status.INCOMPLETE,select(survey(false,true,List.of(),List.of()),approval).status());
        assertEquals(Status.PROTECTION_UNKNOWN,select(survey(true,false,List.of(),List.of()),approval).status());
        assertEquals(Status.WRONG_WORLD,select(new Survey(UUID.randomUUID(),1000,true,true,clean.cells(),List.of(),List.of()),approval).status());
        assertEquals(Status.STALE,select(new Survey(world,1101,true,true,clean.cells(),List.of(),List.of()),approval).status());
    }
    @Test void protectedSpaceEntitiesAndMissingInteriorCellsRejectSitesWithReasons()throws Exception {
        var approval=new ApprovedArea(UUID.randomUUID(),world,area);
        var result=select(survey(true,true,List.of(new Bounds(new Position(-20,64,-20),new Position(20,70,20))),List.of()),approval);
        assertEquals(Status.NO_SAFE_SITE,result.status());assertTrue(result.reviewed().stream().allMatch(c->c.assessment().issues().contains(Issue.PROTECTED)));
        result=select(survey(true,true,List.of(),List.of(area)),approval);assertEquals(Status.NO_SAFE_SITE,result.status());
        assertTrue(result.reviewed().stream().allMatch(c->c.assessment().issues().contains(Issue.ENTITY_PRESENT)));
        var clean=survey(true,true,List.of(),List.of());var partial=new HashMap<>(clean.cells());partial.entrySet().removeIf(e->e.getKey().y()>63);
        result=select(new Survey(world,1000,true,true,partial,List.of(),List.of()),approval);assertEquals(Status.NO_SAFE_SITE,result.status());
        assertTrue(result.reviewed().stream().allMatch(c->c.assessment().issues().contains(Issue.INCOMPLETE)));
    }
    @Test void largeSurveySearchIsCappedAndReportsTheLimit()throws Exception {
        var survey=survey(true,true,List.of(),List.of());var cells=new HashMap<>(survey.cells());
        cells.replaceAll((p,c)->new Cell(c.air(),c.stableSupport(),p.y()==63&&p.x()%2==0,c.protectedWork()));
        var result=select(new Survey(world,1000,true,true,cells,List.of(),List.of()),new ApprovedArea(UUID.randomUUID(),world,area));
        assertTrue(result.budgetLimited());assertTrue(result.roadsConsidered()<=ConstructionSitePlanner.MAX_ROADS);
        assertTrue(result.reviewed().size()<=ConstructionSitePlanner.MAX_CANDIDATES);
        assertTrue(result.reviewed().size()*ConstructionPreview.requiredCells(template(),Transform.identity(),new Position(0,64,0)).size()<=ConstructionSitePlanner.MAX_CELL_REVIEWS);
    }
}
