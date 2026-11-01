package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import java.sql.*;
import java.util.*;

/** Fresh saved-road -> frozen-home site handoff. No replanning, payments, work or housing capacity. */
public final class HousingRoadHandoff {
    public record Receipt(HousingRoadProjects.Link purpose,ConstructionDraft home,ConstructionSiteReservations.Reservation site){}
    private final Connection c;
    public HousingRoadHandoff(Connection c){this.c=Objects.requireNonNull(c);}
    public Receipt prepare(HousingProjectPreparation.Request request,DwellingOccupancy.Context context,ConstructionLandPolicy land)throws SQLException{
        Objects.requireNonNull(request);Objects.requireNonNull(context);Objects.requireNonNull(land);
        if(!c.getAutoCommit())throw new SQLException("Road-to-home handoff requires idle SQL owner");c.setAutoCommit(false);
        try{
            var link=new HousingRoadProjects(c).findHome(request.proposal().draft()).orElseThrow(()->new BuildingRegistry.Declined("Home has no retained access purpose"));
            var family=new HousingProjects(c).find(link.homeDraft()).orElseThrow();
            if(!family.proposal().equals(request.proposal())||!family.village().equals(request.village())||!family.world().equals(request.world())
                    ||!context.world().equals(request.world())||!context.present()||context.now()<request.proposal().createdMillis())
                throw new BuildingRegistry.Declined("Access handoff identity/world/time changed");
            var drafts=new ConstructionDraftRepository(c);var home=drafts.find(link.homeDraft()).orElseThrow();var sites=new ConstructionSiteReservations(c);
            var old=sites.find(home.id());
            if(old.isPresent()){c.commit();return new Receipt(link,home,old.orElseThrow());} // History only; never refresh a released site.
            var need=new HousingProjects(c).pauseReasonRows(home);
            if(need.isPresent())throw new BuildingRegistry.Declined("Access handoff waits: "+need.orElseThrow());
            if(new CivRoadCompletion(c).findDraft(link.roadDraft()).isEmpty())throw new BuildingRegistry.Declined("Access road has not completed and registered");
            var road=drafts.find(link.roadDraft()).orElseThrow();var geometry=LocalRoadDesign.geometry(road);
            var nativeSurvey=request.survey();var exclusions=new ConstructionLandRepository(c).protectedColumns(request.world(),request.search());
            var cells=new HashMap<Position,Cell>();var terrain=new HashMap<>(request.terrain());
            nativeSurvey.cells().forEach((at,cell)->{if(request.search().contains(at))cells.put(at,new Cell(cell.air(),cell.stableSupport(),cell.road(),cell.protectedWork()
                    ||exclusions.contains(new ConstructionLandRepository.Column(at.x(),at.z()))||nativeSurvey.protections().stream().anyMatch(b->b.contains(at))));});
            var registered=new CivRoadCompletion(c).expectedStates(request.world(),request.search());
            HousingProjectPreparation.classifyRoads(cells,terrain,request.states(),registered);
            var requiredRoad=RoadGroundReview.requiredCells(geometry,land.buildingBuffer());var roadStates=new TreeMap<Position,String>();var roadProtection=new HashSet<Position>();
            for(var at:requiredRoad){
                if(request.states().containsKey(at))roadStates.put(at,request.states().get(at));
                var cell=cells.get(at);if(cell==null||cell.protectedWork()||nativeSurvey.entities().stream().anyMatch(b->b.contains(at)))roadProtection.add(at);
            }
            var acknowledged=new TreeMap<Position,String>();road.template().stages().getFirst().blocks().forEach((at,state)->acknowledged.put(road.origin().plus(at),state));
            var evidence=new RoadGroundReview.Observation(request.world(),nativeSurvey.capturedMillis(),nativeSurvey.complete()&&nativeSurvey.protectionsComplete(),roadStates,roadProtection,RoadGroundReview.registeredWithin(registered,requiredRoad));
            var access=RoadGroundReview.inspect(request.world(),geometry,evidence,context.now(),acknowledged,land.buildingBuffer());
            if(!access.suitable())throw new BuildingRegistry.Declined("Completed access is not currently usable: "+access.issues());
            var required=land.requiredCells(home.template(),home.transform(),home.origin());var landCells=new HashMap<Position,ConstructionLandPolicy.Cell>();boolean complete=nativeSurvey.complete()&&nativeSurvey.protectionsComplete();
            for(var at:required){
                var kind=terrain.get(at);var cell=cells.get(at);
                if(kind==null||cell==null||(kind==ConstructionLandPolicy.Kind.AIR)!=cell.air()||(kind==ConstructionLandPolicy.Kind.ROAD)!=cell.road()){complete=false;continue;}
                landCells.put(at,new ConstructionLandPolicy.Cell(kind,cell.protectedWork()));
            }
            var footprint=ConstructionPreview.footprint(home.template(),home.transform(),home.origin());
            boolean entity=nativeSurvey.entities().stream().anyMatch(b->b.minimum().x()<=footprint.maximum().x()&&b.maximum().x()>=footprint.minimum().x()
                    &&b.minimum().y()<=footprint.maximum().y()&&b.maximum().y()>=footprint.minimum().y()&&b.minimum().z()<=footprint.maximum().z()&&b.maximum().z()>=footprint.minimum().z());
            var preview=ConstructionPreview.assess(home.template(),home.transform(),request.world(),home.origin(),new Observation(nativeSurvey.world(),home.origin(),nativeSurvey.capturedMillis(),complete,entity,cells),
                    new ApprovedArea(home.id(),request.world(),request.search()),context.now(),5000);
            var clear=land.review(home.template(),home.transform(),home.origin(),landCells,complete);
            if(!preview.suitable()||!clear.suitable())throw new BuildingRegistry.Declined("Frozen home site changed: "+preview.issues()+" / "+clear.findings());
            var site=sites.reserveRows(home.id(),new ConstructionMaterials.Authority(home.id(),request.world(),nativeSurvey.capturedMillis(),true,true),context.now());
            c.commit();return new Receipt(link,home,site);
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
}
