package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.database.ConstructionBlockJournal;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/** Single main-thread write boundary. Currently obtainable ONLY for marked, explicitly bounded disposable tests. */
public final class PaperConstructionStep {
    public enum State { WAIT,APPLIED,ALREADY_PRESENT,CONFLICT }
    public record Outcome(State state,String explanation){}
    private record Block(Position at,String after){}
    private final JavaPlugin host;private final ConstructionDraft draft;private final ApprovedArea area;
    private final List<Block> blocks;private final Bounds footprint;
    private PaperConstructionStep(JavaPlugin host,ConstructionDraft draft,ApprovedArea area,List<Block> blocks) {
        this.host=host;this.draft=draft;this.area=area;this.blocks=List.copyOf(blocks);
        footprint=ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin());
    }
    /** Not a production permission mechanism. No command or simulation invokes this development-only factory. */
    public static PaperConstructionStep development(JavaPlugin host,ConstructionDraft draft,ApprovedArea area)throws IOException {
        mainThread();Objects.requireNonNull(host);Objects.requireNonNull(draft);Objects.requireNonNull(area);
        var root=host.getServer().getWorldContainer().toPath().toRealPath();var marker=root.resolve("civilizations-dev-marker.txt");
        if(!host.getServer().getIp().equals("127.0.0.1")||host.getServer().getPort()!=25578||!host.getServer().getOnlinePlayers().isEmpty()
                ||!Files.isRegularFile(marker)||Files.size(marker)>128||!Files.readString(marker).trim().equals("Disposable Civilizations Phase 1 test environment"))
            throw new IllegalStateException("Physical construction is restricted to the marked localhost disposable test fixture");
        if(!area.world().equals(draft.world())||!area.bounds().contains(ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin())))
            throw new IllegalArgumentException("Whole development footprint must be explicitly approved");
        var nativePlan=PaperConstructionLibrary.prepare(draft.template(),draft.transform());var blocks=new ArrayList<Block>();
        for(int stage=0;stage<draft.template().stages().size();stage++)for(var local:draft.template().stages().get(stage).blocks().keySet()) {
            var transformed=draft.transform().apply(local,draft.template().size());
            blocks.add(new Block(draft.origin().plus(transformed),nativePlan.stages().get(stage).blocks().get(transformed)));
        }
        return new PaperConstructionStep(host,draft,area,blocks);
    }
    /** Caller must have committed the exact PREPARED journal intent. Return does not itself acknowledge SQL. */
    public Outcome attempt(ConstructionBlockJournal.Intent intent) {
        mainThread();Objects.requireNonNull(intent);
        if(!intent.draft().equals(draft.id())||intent.ordinal()<0||intent.ordinal()>=blocks.size())return conflict("Intent does not belong to this frozen plan");
        var expected=blocks.get(intent.ordinal());
        if(!expected.at().equals(intent.at())||!expected.after().equals(intent.after())||!intent.before().equals("minecraft:air"))return conflict("Intent differs from prepared native geometry/state");
        if(intent.state()==ConstructionBlockJournal.State.CONFLICT)return conflict("Saved intent requires explicit conflict review");
        if(!host.isEnabled()||!host.getServer().getIp().equals("127.0.0.1")||host.getServer().getPort()!=25578||!host.getServer().getOnlinePlayers().isEmpty())return waitFor("Disposable test authority unavailable");
        var world=host.getServer().getWorld(draft.world());if(world==null)return waitFor("World absent");
        if(!area.world().equals(world.getUID())||!area.bounds().contains(footprint))return waitFor("Development area unavailable");
        var minimum=footprint.minimum();var maximum=footprint.maximum();
        if(minimum.y()<=world.getMinHeight()||maximum.y()>=world.getMaxHeight())return waitFor("Plan outside safe world height");
        for(var p:ConstructionPreview.requiredCells(draft.template(),draft.transform(),draft.origin()))
            if(p.y()<world.getMinHeight()||p.y()>=world.getMaxHeight()||!world.isChunkLoaded(p.x()>>4,p.z()>>4))return waitFor("Required chunk unavailable");
        if(!world.getWorldBorder().isInside(new Location(world,minimum.x(),minimum.y(),minimum.z()))
                ||!world.getWorldBorder().isInside(new Location(world,maximum.x(),maximum.y(),maximum.z())))return waitFor("Plan outside world border");
        if(!world.getNearbyEntities(new BoundingBox(minimum.x(),minimum.y(),minimum.z(),maximum.x()+1.0,maximum.y()+1.0,maximum.z()+1.0)).isEmpty())return waitFor("Entity occupies construction space");
        for(int x=minimum.x();x<=maximum.x();x++)for(int z=minimum.z();z<=maximum.z();z++)
            if(!PaperConstructionPreview.stableSupport(world.getBlockAt(x,minimum.y()-1,z).getType()))return waitFor("Ground support changed");
        if(draft.template().purpose()!=Purpose.PATH)for(var anchor:draft.template().anchors())if(anchor.role()==Role.ROAD) {
            var p=draft.origin().plus(draft.transform().apply(anchor.at(),draft.template().size()));
            if(world.getBlockAt(p.x(),p.y(),p.z()).getType()!=Material.DIRT_PATH)return waitFor("Entrance road changed");
        }
        var at=intent.at();var block=world.getBlockAt(at.x(),at.y(),at.z());String current=block.getBlockData().getAsString();
        if(current.equals(intent.after()))return new Outcome(State.ALREADY_PRESENT,"Exact planned state already exists; acknowledge without writing");
        if(intent.state()==ConstructionBlockJournal.State.APPLIED||!current.equals(intent.before()))return conflict("Current block changed; no overwrite performed");
        block.setBlockData(host.getServer().createBlockData(intent.after()),false);
        if(!block.getBlockData().getAsString().equals(intent.after()))return conflict("Native write did not retain the expected state");
        return new Outcome(State.APPLIED,"Exact prepared block placed; SQL acknowledgement still required");
    }
    private static Outcome waitFor(String reason){return new Outcome(State.WAIT,reason);}
    private static Outcome conflict(String reason){return new Outcome(State.CONFLICT,reason);}
    private static void mainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Construction world work requires the server thread");}
}
