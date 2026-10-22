package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HousingRoadAccessPlannerTest {
    final UUID world=UUID.randomUUID();final Position need=new Position(0,64,0);
    final Map<Position,Cell> cells=new HashMap<>();final Map<Position,ConstructionLandPolicy.Kind> terrain=new HashMap<>();
    final Bounds bounds=new Bounds(new Position(-8,63,-8),new Position(8,67,8));
    HousingRoadAccessPlannerTest(){
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=63;y<=67;y++){
            var at=new Position(x,y,z);boolean road=x==0&&z==0&&y==63;
            cells.put(at,new Cell(y>63,true,road,false));terrain.put(at,y>63?ConstructionLandPolicy.Kind.AIR:road?ConstructionLandPolicy.Kind.ROAD:ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND);
        }
    }
    ConstructionTemplate plan(String id)throws Exception{return ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/"+id+".yml"));}
    void blockInfill(){for(var p:List.of(new Position(2,64,0),new Position(-2,64,0),new Position(0,64,2),new Position(0,64,-2))){cells.put(p,new Cell(false,false,false,false));terrain.put(p,ConstructionLandPolicy.Kind.OTHER);}}
    ConstructionSitePlanner.Survey survey(){return new ConstructionSitePlanner.Survey(world,1000,true,true,cells,List.of(),List.of());}
    HousingRoadAccessPlanner.Result review(ConstructionSitePlanner.Survey survey,LocalRoadAccessPlanner.Policy roads)throws Exception{
        return HousingRoadAccessPlanner.select(plan("starter_home"),plan("starter_path"),world,need,survey,new ApprovedArea(UUID.randomUUID(),world,bounds),new ConstructionLandPolicy(0),terrain,roads,1001,5000,7);
    }
    @Test void existingSafeAccessAlwaysWinsWithoutUnnecessaryRoadProposal()throws Exception {
        var result=review(survey(),LocalRoadAccessPlanner.Policy.prototype());assertEquals(HousingRoadAccessPlanner.Status.EXISTING_ACCESS,result.status());
        assertTrue(result.expansion().isEmpty());assertTrue(result.infill().selected().isPresent());assertEquals(0,result.candidates());assertEquals(0,result.routeNodes());
    }
    @Test void jointlyFindsFrozenPaidTermsAndRoadThatCannotCrossItsFutureHome()throws Exception {
        blockInfill();var result=review(survey(),LocalRoadAccessPlanner.Policy.prototype());
        assertTrue(result.infill().selected().isEmpty());assertEquals(HousingRoadAccessPlanner.Status.READY,result.status(),result.toString());
        var proposal=result.expansion().orElseThrow();var footprint=ConstructionPreview.footprint(plan("starter_home"),proposal.homeTransform(),proposal.homeOrigin());
        assertEquals(plan("starter_home").fingerprint(),proposal.homeHash());assertTrue(proposal.road().template().work()>0);assertFalse(proposal.road().template().materials().isEmpty());
        for(var stage:proposal.road().template().stages())for(var local:stage.blocks().keySet()){
            var p=proposal.road().origin().plus(local);assertFalse(p.x()>=footprint.minimum().x()&&p.x()<=footprint.maximum().x()&&p.z()>=footprint.minimum().z()&&p.z()<=footprint.maximum().z());
        }
        assertEquals(proposal,review(survey(),LocalRoadAccessPlanner.Policy.prototype()).expansion().orElseThrow());
        assertTrue(result.candidates()<=HousingRoadAccessPlanner.MAX_CANDIDATES);assertTrue(result.cellReviews()<=HousingRoadAccessPlanner.MAX_CELL_REVIEWS);assertTrue(result.routeNodes()<=HousingRoadAccessPlanner.MAX_ROUTE_NODES);
        assertFalse(result.rejected().isEmpty(),"Rejected alternatives stay explainable");
    }
    @Test void protectionAndStaleOrIncompleteEvidenceCannotBecomeConditionalHousing()throws Exception {
        blockInfill();cells.replaceAll((p,c)->new Cell(c.air(),c.stableSupport(),c.road(),true));assertTrue(review(survey(),LocalRoadAccessPlanner.Policy.prototype()).expansion().isEmpty());
        var old=new ConstructionSitePlanner.Survey(world,0,true,true,cells,List.of(),List.of());
        var result=HousingRoadAccessPlanner.select(plan("starter_home"),plan("starter_path"),world,need,old,new ApprovedArea(UUID.randomUUID(),world,bounds),new ConstructionLandPolicy(0),terrain,LocalRoadAccessPlanner.Policy.prototype(),6000,5000,7);
        assertEquals(HousingRoadAccessPlanner.Status.INVALID_EVIDENCE,result.status());assertTrue(result.expansion().isEmpty());
        var missing=new ConstructionSitePlanner.Survey(world,1000,false,true,cells,List.of(),List.of());assertEquals(HousingRoadAccessPlanner.Status.INVALID_EVIDENCE,review(missing,LocalRoadAccessPlanner.Policy.prototype()).status());
    }
    @Test void exhaustedRouteBudgetReturnsNoPartialRouteOrRoadAuthority()throws Exception {
        blockInfill();var result=review(survey(),new LocalRoadAccessPlanner.Policy(16,1));assertTrue(result.expansion().isEmpty());assertTrue(result.budgetLimited());assertTrue(result.routeNodes()<=HousingRoadAccessPlanner.MAX_ROUTE_NODES);
    }
}
