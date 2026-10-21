package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import org.bukkit.*;
import org.bukkit.util.BoundingBox;
import java.util.*;
import java.util.function.*;

/** Read-only two-pass review of empty or journal-backed partial work. Never loads chunks or writes blocks. */
public final class PaperConstructionWorkSurvey {
    public record Frame(ConstructionMaterials.Authority authority,String nextObserved,String explanation){}
    private final ConstructionWorkQueue.Work work;private final Function<UUID,World> worlds;private final BooleanSupplier primary;
    private final List<Position> positions;private final Map<Position,String> prefix=new HashMap<>(),first=new HashMap<>();
    private final Set<Position> roads=new HashSet<>(),authored=new HashSet<>();private final ConstructionPreview.Bounds footprint;
    private final Position next;private final String nextAfter;private final long started;private long captured;private int cursor;
    private boolean verifying,unsafe;private String reason;private Frame finished;
    public PaperConstructionWorkSurvey(ConstructionWorkQueue.Work work,long now){
        this(work,prepare(work),now,Bukkit::getWorld,Bukkit::isPrimaryThread);
    }
    private static ConstructionTemplate prepare(ConstructionWorkQueue.Work work){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Construction survey preparation requires server thread");
        return PaperConstructionLibrary.prepare(work.draft().template(),work.draft().transform());
    }
    PaperConstructionWorkSurvey(ConstructionWorkQueue.Work work,ConstructionTemplate nativePlan,long now,Function<UUID,World> worlds,BooleanSupplier primary){
        this.work=Objects.requireNonNull(work);this.worlds=Objects.requireNonNull(worlds);this.primary=Objects.requireNonNull(primary);main();
        var draft=work.draft();if(now<0||work.nextOrdinal()<0||work.nextOrdinal()>draft.template().blockCount()
                ||!nativePlan.size().equals(draft.transform().size(draft.template().size()))||nativePlan.blockCount()!=draft.template().blockCount())throw new IllegalArgumentException("Invalid frozen work survey");
        started=now;captured=now;footprint=ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin());
        positions=List.copyOf(ConstructionPreview.requiredCells(draft.template(),draft.transform(),draft.origin()));
        int ordinal=0;Position atNext=null;String afterNext=null;
        // Journal ordinals follow AUTHORED ordering, then transform each coordinate.
        // Sorting the transformed palette would silently change ordinals under rotation/mirroring.
        for(int stage=0;stage<draft.template().stages().size();stage++)for(var local:draft.template().stages().get(stage).blocks().keySet()){
            var transformed=draft.transform().apply(local,draft.template().size());
            String after=nativePlan.stages().get(stage).blocks().get(transformed);
            if(after==null)throw new IllegalArgumentException("Native palette lost authored geometry");
            var at=draft.origin().plus(transformed);authored.add(at);
            if(ordinal<work.nextOrdinal())prefix.put(at,after);
            if(ordinal==work.nextOrdinal()){atNext=at;afterNext=after;}ordinal++;
        }
        next=atNext;nextAfter=afterNext;
        for(var anchor:nativePlan.anchors())if(anchor.role()==Role.ROAD)roads.add(draft.origin().plus(anchor.at()));
        if(work.pending().isPresent()){
            var intent=work.pending().get();if(!intent.draft().equals(draft.id())||intent.ordinal()!=work.nextOrdinal()||!intent.at().equals(next)||!intent.after().equals(nextAfter))throw new IllegalArgumentException("Pending intent differs from frozen native plan");
        }
        if(work.state()==ConstructionWorkQueue.State.PAUSED||work.state()==ConstructionWorkQueue.State.CONFLICT)fail("Saved project is paused or conflicted; native review cannot grant new authority");
    }
    public Optional<Frame> advance(int budget,long now){
        main();if(budget<1||budget>256||now<0)throw new IllegalArgumentException("Invalid construction survey budget/time");
        if(finished!=null)return Optional.of(finished);var world=worlds.apply(work.draft().world());
        if(world==null||now<started||now-started>30_000)return Optional.of(finish(false,"World absent, clock changed or survey expired"));
        if(unsafe&&!verifying&&cursor==0)return Optional.of(finish(false,reason));
        while(budget-->0&&cursor<positions.size()){
            var at=positions.get(cursor++);
            if(!available(world,at)){fail("Required cell is outside loaded chunks, world height or border");continue;}
            var block=world.getBlockAt(at.x(),at.y(),at.z());String state=block.getBlockData().getAsString();
            if(verifying){if(!state.equals(first.get(at)))fail("Native evidence changed between complete passes");}
            else first.put(at,state);
            if(at.y()<footprint.minimum().y()&&!PaperConstructionPreview.stableSupport(block.getType()))fail("Ground support changed");
            if(roads.contains(at)&&block.getType()!=Material.DIRT_PATH)fail("Saved entrance road changed");
            if(footprint.contains(at)){
                String expected=prefix.get(at);
                if(expected!=null){if(!state.equals(expected))fail("Acknowledged construction block changed; no replacement authority");}
                else if(at.equals(next)&&work.pending().isPresent()){
                    if(!state.equals("minecraft:air")&&!state.equals(nextAfter))fail("Prepared intent meets a different native block");
                }else if(authored.contains(at)?!state.equals("minecraft:air"):!block.getType().isAir())fail("Unbuilt structure space is occupied; no overwrite authority");
            }
        }
        if(cursor<positions.size())return Optional.empty();
        if(!verifying&&!unsafe){verifying=true;cursor=0;captured=now;return Optional.empty();}
        if(unsafe)return Optional.of(finish(false,reason));
        for(var at:positions)if(!available(world,at))return Optional.of(finish(false,"Required chunk or world boundary changed before final review"));
        if(now-captured>5000)return Optional.of(finish(false,"Verification pass is too old for a new economic step"));
        var min=footprint.minimum();var max=footprint.maximum();
        if(!world.getNearbyEntities(new BoundingBox(min.x(),min.y(),min.z(),max.x()+1.0,max.y()+1.0,max.z()+1.0)).isEmpty())return Optional.of(finish(false,"Entity occupies construction space"));
        return Optional.of(finish(true,"Two matching loaded-only passes; SQL protection, actual funding/labor and native-step checks remain separate"));
    }
    private boolean available(World world,Position at){return at.y()>=world.getMinHeight()&&at.y()<world.getMaxHeight()
            &&world.isChunkLoaded(at.x()>>4,at.z()>>4)&&world.getWorldBorder().isInside(new Location(world,at.x(),at.y(),at.z()));}
    private void fail(String message){unsafe=true;if(reason==null)reason=message;}
    private Frame finish(boolean safe,String explanation){
        finished=new Frame(new ConstructionMaterials.Authority(work.draft().id(),work.draft().world(),captured,safe,safe),next==null?null:first.get(next),explanation);return finished;
    }
    private void main(){if(!primary.getAsBoolean())throw new IllegalStateException("Construction work survey requires server thread");}
}
