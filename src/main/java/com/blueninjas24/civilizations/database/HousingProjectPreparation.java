package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.sql.*;
import java.util.*;

/** Atomic dry preparation under approved guarded automatic growth. Never funds work or writes the world. */
public final class HousingProjectPreparation {
    public record Request(HousingProjects.Proposal proposal,UUID village,UUID world,Position need,Bounds search,
                          ConstructionSitePlanner.Survey survey,Map<Position,ConstructionLandPolicy.Kind> terrain){
        public Request{
            Objects.requireNonNull(proposal);Objects.requireNonNull(village);Objects.requireNonNull(world);Objects.requireNonNull(need);
            Objects.requireNonNull(search);Objects.requireNonNull(survey);terrain=Map.copyOf(terrain);
            long width=(long)search.maximum().x()-search.minimum().x()+1,depth=(long)search.maximum().z()-search.minimum().z()+1;
            if(width>96||depth>96||!search.contains(need)||terrain.size()>ConstructionPreview.MAX_OBSERVATION_CELLS)
                throw new IllegalArgumentException("Home preparation exceeds bounded local search");
        }
    }
    /** Empty review means historical receipt only: it never refreshes authority or reactivates cancelled work. */
    public record Receipt(ConstructionDraft draft,HousingProjects.Binding family,ConstructionSiteReservations.Reservation site,
                          Optional<ConstructionSitePlanner.Result> review){}
    private final Connection c;
    public HousingProjectPreparation(Connection c){this.c=Objects.requireNonNull(c);}
    public Receipt prepare(Request request,DwellingOccupancy.Context context,List<ConstructionTemplate> templates,
                           HousingProjectPlanner planner,ConstructionLandPolicy land,long seed,long period)throws SQLException {
        Objects.requireNonNull(request);Objects.requireNonNull(context);templates=List.copyOf(templates);Objects.requireNonNull(planner);Objects.requireNonNull(land);
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
                if(drafts.find(request.proposal().draft()).isPresent())throw new BuildingRegistry.Declined("Automatic preparation requires a new draft identity");
                var report=new HousingPlanning(c).review(request.village(),context,templates,planner,seed,period);
                var recommendation=report.plan().recommendations().stream().filter(r->r.household().equals(request.proposal().household())).findFirst()
                        .orElseThrow(()->new BuildingRegistry.Declined("Household has no verified saved housing need here"));
                if(recommendation.action()!=HousingProjectPlanner.Action.PROPOSE)throw new BuildingRegistry.Declined("Home preparation waits: "+recommendation.explanation());
                var template=templates.stream().filter(t->t.fingerprint().equals(recommendation.fingerprint())).findFirst().orElseThrow();
                var exclusions=new ConstructionLandRepository(c).protectedColumns(request.world(),request.search());
                var nativeSurvey=request.survey();var cells=new HashMap<Position,Cell>();
                nativeSurvey.cells().forEach((p,cell)->{if(request.search().contains(p))cells.put(p,new Cell(cell.air(),cell.stableSupport(),cell.road(),cell.protectedWork()
                        ||exclusions.contains(new ConstructionLandRepository.Column(p.x(),p.z()))));});
                var survey=new ConstructionSitePlanner.Survey(nativeSurvey.world(),nativeSurvey.capturedMillis(),nativeSurvey.complete(),nativeSurvey.protectionsComplete(),
                        cells,nativeSurvey.protections(),nativeSurvey.entities());
                // This is only the search envelope of the approved clear-land heuristic, never native write permission.
                var envelope=new ApprovedArea(request.proposal().draft(),request.world(),request.search());
                var result=ConstructionSitePlanner.select(template,request.world(),request.need(),survey,envelope,context.now(),5000,seed,land,request.terrain());
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
}
