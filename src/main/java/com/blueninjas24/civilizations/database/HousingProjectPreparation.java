package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.sql.*;
import java.util.*;

/** Atomic dry preparation under approved guarded automatic growth. Never funds work or writes the world. */
public final class HousingProjectPreparation {
    public record Request(HousingProjects.Proposal proposal,UUID village,UUID world,Position need,Bounds search,
                          ConstructionSitePlanner.Survey survey,Map<Position,ConstructionLandPolicy.Kind> terrain,Map<Position,String> states){
        public Request{
            Objects.requireNonNull(proposal);Objects.requireNonNull(village);Objects.requireNonNull(world);Objects.requireNonNull(need);
            Objects.requireNonNull(search);Objects.requireNonNull(survey);terrain=Map.copyOf(terrain);states=Map.copyOf(states);
            long width=(long)search.maximum().x()-search.minimum().x()+1,depth=(long)search.maximum().z()-search.minimum().z()+1;
            if(width>96||depth>96||!search.contains(need)||terrain.size()>ConstructionPreview.MAX_OBSERVATION_CELLS||states.size()>ConstructionPreview.MAX_OBSERVATION_CELLS)
                throw new IllegalArgumentException("Home preparation exceeds bounded local search");
        }
        public Request(HousingProjects.Proposal proposal,UUID village,UUID world,Position need,Bounds search,ConstructionSitePlanner.Survey survey,Map<Position,ConstructionLandPolicy.Kind> terrain){this(proposal,village,world,need,search,survey,terrain,Map.of());}
    }
    /** Empty review means historical receipt only: it never refreshes authority or reactivates cancelled work. */
    public record Receipt(ConstructionDraft draft,HousingProjects.Binding family,ConstructionSiteReservations.Reservation site,
                          Optional<ConstructionSitePlanner.Result> review){}
    private final Connection c;
    public HousingProjectPreparation(Connection c){this.c=Objects.requireNonNull(c);}
    public Receipt prepare(Request request,DwellingOccupancy.Context context,List<ConstructionTemplate> templates,
                           HousingProjectPlanner planner,ConstructionLandPolicy land,long seed,long period)throws SQLException {
        return prepare(request,context,templates,planner,land,seed,period,64);
    }
    public Receipt prepare(Request request,DwellingOccupancy.Context context,List<ConstructionTemplate> templates,
                           HousingProjectPlanner planner,ConstructionLandPolicy land,long seed,long period,int maximumActive)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(context);templates=List.copyOf(templates);Objects.requireNonNull(planner);Objects.requireNonNull(land);
        if(maximumActive<1||maximumActive>64)throw new IllegalArgumentException("Active home proposal limit must be 1..64");
        if(!c.getAutoCommit())throw new SQLException("Home preparation requires an idle SQL owner");c.setAutoCommit(false);
        try{
            if(!context.present()||!context.world().equals(request.world())||request.proposal().createdMillis()>context.now())
                throw new BuildingRegistry.Declined("Home preparation needs its present world and valid proposal time");
            var projects=new HousingProjects(c);var drafts=new ConstructionDraftRepository(c);var sites=new ConstructionSiteReservations(c);
            var old=projects.find(request.proposal().draft());
            Receipt receipt;
            if(old.isPresent()){
                var binding=old.orElseThrow();
                if(!binding.proposal().equals(request.proposal())||!binding.village().equals(request.village())||!binding.world().equals(request.world()))
                    throw new BuildingRegistry.Declined("Prepared home identity changed");
                receipt=new Receipt(drafts.find(request.proposal().draft()).orElseThrow(),binding,
                        sites.find(request.proposal().draft()).orElseThrow(()->new SQLException("Prepared home has no retained site receipt")),Optional.empty());
            }else{
                if(projects.activeCount(request.village(),request.world())>=maximumActive)throw new BuildingRegistry.Declined("Village active-home proposal limit reached");
                if(drafts.find(request.proposal().draft()).isPresent())throw new BuildingRegistry.Declined("Automatic preparation requires a new draft identity");
                var report=new HousingPlanning(c).review(request.village(),context,templates,planner,seed,period);
                var recommendation=report.plan().recommendations().stream().filter(r->r.household().equals(request.proposal().household())).findFirst()
                        .orElseThrow(()->new BuildingRegistry.Declined("Household has no verified saved housing need here"));
                if(recommendation.action()!=HousingProjectPlanner.Action.PROPOSE)throw new BuildingRegistry.Declined("Home preparation waits: "+recommendation.explanation());
                var template=templates.stream().filter(t->t.fingerprint().equals(recommendation.fingerprint())).findFirst().orElseThrow();
                var exclusions=new ConstructionLandRepository(c).protectedColumns(request.world(),request.search());
                var nativeSurvey=request.survey();var cells=new HashMap<Position,Cell>();var terrain=new HashMap<>(request.terrain());
                nativeSurvey.cells().forEach((p,cell)->{if(request.search().contains(p))cells.put(p,new Cell(cell.air(),cell.stableSupport(),cell.road(),cell.protectedWork()
                        ||exclusions.contains(new ConstructionLandRepository.Column(p.x(),p.z()))));});
                classifyRoads(cells,terrain,request.states(),new CivRoadCompletion(c).expectedStates(request.world(),request.search()));
                var survey=new ConstructionSitePlanner.Survey(nativeSurvey.world(),nativeSurvey.capturedMillis(),nativeSurvey.complete(),nativeSurvey.protectionsComplete(),
                        cells,nativeSurvey.protections(),nativeSurvey.entities());
                // This is only the search envelope of the approved clear-land heuristic, never native write permission.
                var envelope=new ApprovedArea(request.proposal().draft(),request.world(),request.search());
                var result=ConstructionSitePlanner.select(template,request.world(),request.need(),survey,envelope,context.now(),5000,seed,land,terrain);
                var selected=result.selected().orElseThrow(()->new BuildingRegistry.Declined("Home site waits: "+result.status()));
                var draft=new ConstructionDraft(request.proposal().draft(),request.village(),request.world(),selected.origin(),selected.transform(),template,request.proposal().createdMillis());
                drafts.saveRows(draft);
                var family=projects.bindRows(request.proposal(),context,planner,seed,period);
                var site=sites.reserveRows(draft.id(),new ConstructionMaterials.Authority(draft.id(),draft.world(),survey.capturedMillis(),true,true),context.now());
                receipt=new Receipt(draft,family,site,Optional.of(result));
            }
            c.commit();c.setAutoCommit(true);return receipt;
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    /** Only the SQL owner's positive road registry can supply expected; raw paving alone grants nothing. */
    static void classifyRoads(Map<Position,Cell> cells,Map<Position,ConstructionLandPolicy.Kind> terrain,Map<Position,String> states,Map<Position,String> expected){
        expected.forEach((at,state)->{
            var cell=cells.get(at);if(cell==null)return;
            boolean matches=state.equals(states.get(at));
            cells.put(at,new Cell(cell.air(),cell.stableSupport(),matches,cell.protectedWork()));
            terrain.put(at,matches?ConstructionLandPolicy.Kind.ROAD:ConstructionLandPolicy.Kind.OTHER);
        });
    }
}
