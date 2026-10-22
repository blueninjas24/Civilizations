package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import static com.blueninjas24.civilizations.paper.PaperConstructionStep.State;
import com.blueninjas24.civilizations.paper.PaperConstructionStep.Outcome;

/** Exact paid natural-ground CAS, separate from home/air placement. Not an automatic road scheduler. */
public final class PaperRoadWriter {
    private final JavaPlugin host;private final PopulationSession session;private int tick=Integer.MIN_VALUE,used;
    public PaperRoadWriter(JavaPlugin host,PopulationSession session){this.host=Objects.requireNonNull(host);this.session=Objects.requireNonNull(session);}
    public Outcome attempt(ConstructionWorkQueue.Work work,PaperRoadWorkSurvey.Frame frame,PopulationSession.PreparedConstructionStep prepared){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Road writes require server thread");
        Objects.requireNonNull(work);Objects.requireNonNull(frame);Objects.requireNonNull(prepared);
        var draft=work.draft();var intent=prepared.intent();var geometry=LocalRoadDesign.geometry(draft);var observation=frame.observation();
        if(work.state()!=ConstructionWorkQueue.State.READY&&work.state()!=ConstructionWorkQueue.State.RESUME||!frame.ready()||!observation.complete())return outcome(State.WAIT,"Road work/evidence is not ready");
        if(!geometry.equals(frame.geometry())||!draft.world().equals(observation.world())||!draft.world().equals(prepared.permit().world())||prepared.permit().checkedMillis()!=observation.capturedMillis()
                ||!draft.id().equals(intent.draft())||intent.ordinal()!=work.nextOrdinal()||intent.state()==ConstructionBlockJournal.State.CONFLICT||!RoadGroundReview.naturalGroundStates().contains(intent.before()))return outcome(State.CONFLICT,"Road intent, frame or permit differs from exact paid ground work");
        var placements=geometry.template().stages().getFirst().blocks().entrySet().stream().toList();
        if(intent.ordinal()<0||intent.ordinal()>=placements.size())return outcome(State.CONFLICT,"Road ordinal outside frozen plan");
        var expected=placements.get(intent.ordinal());
        if(!geometry.origin().plus(expected.getKey()).equals(intent.at())||!expected.getValue().equals(intent.after()))return outcome(State.CONFLICT,"Road position/material differs from frozen plan");
        PaperConstructionLibrary.prepare(draft.template(),draft.transform());
        var required=RoadGroundReview.requiredCells(geometry,frame.buffer());if(!observation.states().keySet().containsAll(required))return outcome(State.WAIT,"Road frame lacks complete clearance");
        if(!observation.registeredRoads().equals(RoadGroundReview.registeredWithin(work.roadSurfaces(),required)))return outcome(State.CONFLICT,"Road frame provenance differs from immutable owner snapshot");
        var prefix=new HashMap<Position,String>();for(int i=0;i<intent.ordinal();i++)prefix.put(geometry.origin().plus(placements.get(i).getKey()),placements.get(i).getValue());
        if(intent.after().equals(observation.states().get(intent.at())))prefix.put(intent.at(),intent.after());
        if(!RoadGroundReview.inspect(draft.world(),geometry,observation,observation.capturedMillis(),prefix,frame.buffer()).suitable())return outcome(State.WAIT,"Road frame no longer satisfies its exact clearance profile");
        return session.tryNativeConstructionStep(prepared,()->current(draft,frame,prepared,required)).orElseGet(()->outcome(State.WAIT,"SQL owner busy/changed, evidence expired or stopping; renew without writing"));
    }
    private Outcome current(ConstructionDraft draft,PaperRoadWorkSurvey.Frame frame,PopulationSession.PreparedConstructionStep prepared,Set<Position> required){
        if(!host.isEnabled())return outcome(State.WAIT,"Plugin stopping");var world=host.getServer().getWorld(draft.world());if(world==null)return outcome(State.WAIT,"Road world absent");
        int current=Bukkit.getCurrentTick();if(tick!=current){tick=current;used=0;}
        if(required.size()+1>256-used)return outcome(State.WAIT,"Road final-scene budget exhausted");used+=required.size()+1;
        var intent=prepared.intent();var at=intent.at();if(!PaperRoadWorkSurvey.available(world,at))return outcome(State.WAIT,"Road target chunk/height/border unavailable");
        var target=world.getBlockAt(at.x(),at.y(),at.z());String state=target.getBlockData().getAsString();
        if(state.equals(intent.after()))return outcome(State.ALREADY_PRESENT,"Exact paid road output already present; acknowledge without replacing");
        if(!state.equals(intent.before())||intent.state()==ConstructionBlockJournal.State.APPLIED)return outcome(State.CONFLICT,"Road ground changed; no replacement or free repair");
        if(!state.equals(frame.observation().states().get(at)))return outcome(State.CONFLICT,"Road evidence no longer contains the exact original ground");
        for(var p:required){
            if(p.equals(at))continue;if(!PaperRoadWorkSurvey.available(world,p))return outcome(State.WAIT,"Road surroundings unavailable");
            if(!world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString().equals(frame.observation().states().get(p)))return outcome(State.WAIT,"Road surroundings changed; inspect again");
        }
        if(!PaperRoadWorkSurvey.entityChunksLoaded(world,frame.geometry()))return outcome(State.WAIT,"Road entity census chunks unavailable");
        if(!world.getNearbyEntities(PaperRoadWorkSurvey.entityBounds(frame.geometry())).isEmpty())return outcome(State.WAIT,"Entity occupies road or retained access");
        if(!session.claimNativeConstructionWrite(prepared))return outcome(State.WAIT,"Road before-state is ambiguous after an earlier attempt/session; manual review required");
        target.setBlockData(host.getServer().createBlockData(intent.after()),false); // No mining drops or replacement resources.
        if(!target.getBlockData().getAsString().equals(intent.after()))return outcome(State.CONFLICT,"Road output did not persist exactly; no automatic replacement");
        return outcome(State.APPLIED,"One paid road cell paved; durable acknowledgement still required");
    }
    private static Outcome outcome(State state,String reason){return new Outcome(state,reason);}
}
