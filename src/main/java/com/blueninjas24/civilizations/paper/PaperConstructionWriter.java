package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import java.util.*;
import static com.blueninjas24.civilizations.paper.PaperConstructionStep.State;
import com.blueninjas24.civilizations.paper.PaperConstructionStep.Outcome;

/** Internal guarded new-project writer. Not scheduled in ordinary gameplay until completion/activation is connected. */
public final class PaperConstructionWriter {
    public static final int CELL_READS_PER_TICK=256;
    private final JavaPlugin host;private final PopulationSession session;private int tick=Integer.MIN_VALUE,used;
    public PaperConstructionWriter(JavaPlugin host,PopulationSession session){this.host=Objects.requireNonNull(host);this.session=Objects.requireNonNull(session);}
    public Outcome attempt(ConstructionWorkQueue.Work work,PaperConstructionWorkSurvey.Frame frame,PopulationSession.PreparedConstructionStep prepared){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Construction writes require server thread");
        Objects.requireNonNull(work);Objects.requireNonNull(frame);Objects.requireNonNull(prepared);
        var draft=work.draft();var intent=prepared.intent();var authority=frame.authority();
        if(work.state()!=ConstructionWorkQueue.State.READY&&work.state()!=ConstructionWorkQueue.State.RESUME)return outcome(State.WAIT,"Saved work is not ready for a native step");
        if(!intent.draft().equals(draft.id())||intent.ordinal()!=work.nextOrdinal()||intent.ordinal()<0||intent.ordinal()>=draft.template().blockCount()
                ||intent.state()==ConstructionBlockJournal.State.CONFLICT||!intent.before().equals("minecraft:air"))return outcome(State.CONFLICT,"Native intent differs from saved ready work");
        if(!authority.draft().equals(draft.id())||!authority.world().equals(draft.world())||!authority.worldLoaded()||!authority.siteCleared()
                ||authority.checkedMillis()!=prepared.permit().checkedMillis()||!prepared.permit().world().equals(draft.world()))return outcome(State.WAIT,"Exact verified native frame and prepared owner authority required");
        var cells=ConstructionPreview.requiredCells(draft.template(),draft.transform(),draft.origin());
        if(cells.size()+1>CELL_READS_PER_TICK)return outcome(State.WAIT,"Plan exceeds this prototype's bounded final-review size; no cells changed");
        if(!frame.cells().keySet().equals(cells))return outcome(State.WAIT,"Native frame coverage differs from frozen structure");
        var nativePlan=PaperConstructionLibrary.prepare(draft.template(),draft.transform());int ordinal=0;boolean matched=false;
        for(int stage=0;stage<draft.template().stages().size();stage++)for(var local:draft.template().stages().get(stage).blocks().keySet()){
            if(ordinal++!=intent.ordinal())continue;var transformed=draft.transform().apply(local,draft.template().size());
            matched=intent.at().equals(draft.origin().plus(transformed))&&intent.after().equals(nativePlan.stages().get(stage).blocks().get(transformed));
        }
        if(!matched)return outcome(State.CONFLICT,"Native state/position differs from exact authored transformed ordinal");
        return session.tryNativeConstructionStep(prepared,()->currentStep(draft,frame,prepared,cells))
                .orElseGet(()->outcome(State.WAIT,"SQL owner busy, changed, expired or stopped; renew review without writing"));
    }
    private Outcome currentStep(ConstructionDraft draft,PaperConstructionWorkSurvey.Frame frame,PopulationSession.PreparedConstructionStep prepared,Set<Position> cells){
        if(!host.isEnabled())return outcome(State.WAIT,"Plugin is stopping");var world=host.getServer().getWorld(draft.world());
        if(world==null)return outcome(State.WAIT,"World absent");int current=Bukkit.getCurrentTick();if(tick!=current){tick=current;used=0;}
        var intent=prepared.intent();var at=intent.at();
        if(!available(world,at))return outcome(State.WAIT,"Target chunk, height or border unavailable");
        if(used==CELL_READS_PER_TICK)return outcome(State.WAIT,"Native cell budget exhausted for this tick");used++;
        Block target=world.getBlockAt(at.x(),at.y(),at.z());String state=target.getBlockData().getAsString();
        if(state.equals(intent.after()))return outcome(State.ALREADY_PRESENT,"Exact output already present; acknowledge without writing");
        if(!state.equals(intent.before())||intent.state()==ConstructionBlockJournal.State.APPLIED)return outcome(State.CONFLICT,"Target changed; no overwrite or replacement performed");
        if(!state.equals(frame.cells().get(at)))return outcome(State.CONFLICT,"Observed output disappeared; no replacement performed");
        // Includes every remaining cell plus post-write verification. Never split this atomic final scene check across ticks.
        if(cells.size()>CELL_READS_PER_TICK-used)return outcome(State.WAIT,"Native cell budget exhausted for this tick");used+=cells.size();
        for(var p:cells){
            if(p.equals(at))continue;if(!available(world,p))return outcome(State.WAIT,"Required chunk, height or border changed");
            if(!world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString().equals(frame.cells().get(p)))return outcome(State.WAIT,"Construction surroundings changed after survey; renew without writing");
        }
        var bounds=ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin());var min=bounds.minimum();var max=bounds.maximum();
        if(!world.getNearbyEntities(new BoundingBox(min.x(),min.y(),min.z(),max.x()+1.0,max.y()+1.0,max.z()+1.0)).isEmpty())return outcome(State.WAIT,"Entity occupies construction space");
        if(!session.claimNativeConstructionWrite(prepared))return outcome(State.WAIT,"Unfinished before-state is ambiguous after an earlier attempt/session; explicit review required");
        target.setBlockData(host.getServer().createBlockData(intent.after()),false);
        if(!target.getBlockData().getAsString().equals(intent.after()))return outcome(State.CONFLICT,"Native write did not retain exact output; no automatic replacement authorized");
        return outcome(State.APPLIED,"One exact paid/journaled block placed; durable acknowledgement still required");
    }
    private boolean available(World world,Position at){return at.y()>=world.getMinHeight()&&at.y()<world.getMaxHeight()
            &&world.isChunkLoaded(at.x()>>4,at.z()>>4)&&world.getWorldBorder().isInside(new Location(world,at.x(),at.y(),at.z()));}
    private static Outcome outcome(State state,String reason){return new Outcome(state,reason);}
}
