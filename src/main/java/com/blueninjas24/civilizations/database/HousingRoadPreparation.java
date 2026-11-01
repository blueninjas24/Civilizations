package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.sql.*;
import java.util.*;

/** Atomic road-first preparation. Reserves only access land; no purchases, labor or world writes. */
public final class HousingRoadPreparation {
    public record Receipt(ConstructionDraft road,ConstructionDraft home,HousingProjects.Binding family,
                          HousingRoadProjects.Link purpose,ConstructionSiteReservations.Reservation site){}
    private final Connection c;
    public HousingRoadPreparation(Connection c){this.c=Objects.requireNonNull(c);}
    public Receipt prepare(UUID roadId,HousingProjectPreparation.Request request,DwellingOccupancy.Context context,
            List<ConstructionTemplate> templates,HousingProjectPlanner planner,ConstructionLandPolicy land,
            long seed,long period,int maximumActive)throws SQLException{
        Objects.requireNonNull(roadId);templates=List.copyOf(templates);
        if(maximumActive<1||maximumActive>64)throw new IllegalArgumentException("Active home proposal limit must be 1..64");
        if(!c.getAutoCommit())throw new SQLException("Road-first preparation requires idle SQL owner");c.setAutoCommit(false);
        try{
            if(!context.present()||!context.world().equals(request.world())||context.now()<request.proposal().createdMillis())
                throw new BuildingRegistry.Declined("Road-first preparation needs present world and current proposal time");
            var links=new HousingRoadProjects(c);var drafts=new ConstructionDraftRepository(c);var projects=new HousingProjects(c);var sites=new ConstructionSiteReservations(c);
            var old=links.findRoad(roadId);Receipt receipt;
            if(old.isPresent()){
                var link=old.orElseThrow();var family=projects.find(link.homeDraft()).orElseThrow();
                if(!family.proposal().equals(request.proposal())||!family.village().equals(request.village())||!family.world().equals(request.world()))
                    throw new BuildingRegistry.Declined("Road-first preparation identity changed");
                receipt=new Receipt(drafts.find(roadId).orElseThrow(),drafts.find(link.homeDraft()).orElseThrow(),family,link,
                        sites.find(roadId).orElseThrow(()->new SQLException("Prepared access lost retained site receipt")));
            }else{
                if(roadId.equals(request.proposal().draft())||drafts.find(roadId).isPresent()||drafts.find(request.proposal().draft()).isPresent())
                    throw new BuildingRegistry.Declined("Road-first preparation requires two unused draft identities");
                if(projects.activeCount(request.village(),request.world())>=maximumActive)throw new BuildingRegistry.Declined("Village active-home proposal limit reached");
                var report=new HousingPlanning(c).review(request.village(),context,templates,planner,seed,period);
                var need=report.plan().recommendations().stream().filter(r->r.household().equals(request.proposal().household())).findFirst()
                        .orElseThrow(()->new BuildingRegistry.Declined("Household has no verified saved housing need"));
                if(need.action()!=HousingProjectPlanner.Action.PROPOSE)throw new BuildingRegistry.Declined("Road-first preparation waits: "+need.explanation());
                var homePlan=templates.stream().filter(t->t.fingerprint().equals(need.fingerprint())).findFirst().orElseThrow();
                var roadPlan=templates.stream().filter(t->t.purpose()==Purpose.PATH&&t.id().equals("starter_path")).findFirst()
                        .orElseThrow(()->new BuildingRegistry.Declined("No editable local path recipe available"));
                var excluded=new ConstructionLandRepository(c).protectedColumns(request.world(),request.search());
                var nativeSurvey=request.survey();var cells=new HashMap<Position,Cell>();var terrain=new HashMap<>(request.terrain());
                nativeSurvey.cells().forEach((at,cell)->{if(request.search().contains(at))cells.put(at,new Cell(cell.air(),cell.stableSupport(),cell.road(),
                        cell.protectedWork()||excluded.contains(new ConstructionLandRepository.Column(at.x(),at.z()))));});
                var registered=new CivRoadCompletion(c).expectedStates(request.world(),request.search());
                HousingProjectPreparation.classifyRoads(cells,terrain,request.states(),registered);
                var survey=new ConstructionSitePlanner.Survey(nativeSurvey.world(),nativeSurvey.capturedMillis(),nativeSurvey.complete(),nativeSurvey.protectionsComplete(),cells,nativeSurvey.protections(),nativeSurvey.entities());
                var area=new ApprovedArea(request.proposal().draft(),request.world(),request.search());
                long accessSeed=seed^Long.rotateLeft(period,23)^request.proposal().household().hashCode();
                var review=HousingRoadAccessPlanner.select(homePlan,roadPlan,request.world(),request.need(),survey,area,land,terrain,LocalRoadAccessPlanner.Policy.prototype(),context.now(),5000,accessSeed);
                var selected=review.expansion().orElseThrow(()->new BuildingRegistry.Declined("Road-first preparation waits: "+review.status()+"; existing-access homes take priority"));
                var required=RoadGroundReview.requiredCells(selected.road().geometry(),land.buildingBuffer());var protectedCells=new HashSet<Position>();var states=new TreeMap<Position,String>();
                for(var at:required){
                    if(request.states().containsKey(at))states.put(at,request.states().get(at));
                    var cell=cells.get(at);
                    if(cell==null||cell.protectedWork()||nativeSurvey.protections().stream().anyMatch(b->b.contains(at))
                            ||nativeSurvey.entities().stream().anyMatch(b->b.contains(at)))protectedCells.add(at);
                }
                var observed=new RoadGroundReview.Observation(request.world(),survey.capturedMillis(),survey.complete()&&survey.protectionsComplete(),states,protectedCells,
                        RoadGroundReview.registeredWithin(registered,required));
                var ground=RoadGroundReview.inspect(request.world(),selected.road().geometry(),observed,context.now(),Map.of(),land.buildingBuffer());
                if(!ground.suitable())throw new BuildingRegistry.Declined("Road-first native ground waits: "+ground.issues());
                var home=new ConstructionDraft(request.proposal().draft(),request.village(),request.world(),selected.homeOrigin(),selected.homeTransform(),homePlan,request.proposal().createdMillis());
                var road=new ConstructionDraft(roadId,request.village(),request.world(),selected.road().origin(),Transform.identity(),selected.road().template(),request.proposal().createdMillis());
                drafts.saveRows(home);drafts.saveRows(road);
                var link=links.bindRows(roadId,request.proposal(),context,planner,seed,period);
                var site=sites.reserveRows(roadId,new ConstructionMaterials.Authority(roadId,request.world(),survey.capturedMillis(),true,true),context.now());
                receipt=new Receipt(road,home,projects.find(home.id()).orElseThrow(),link,site);
            }
            c.commit();return receipt;
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
}
