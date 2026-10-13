package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import java.util.logging.*;

/** One saved worksite frame at a time; same original water, strict main-thread budgets, no block edits. */
public final class PaperFisheryWorkplaces extends BukkitRunnable {
    private final PopulationSession session;private final FisheryShorelinePolicy policy;private final Logger logger;private final LongSupplier clock;
    private CompletableFuture<List<PopulationSession.FisheryWorksite>> page;private CompletableFuture<?> pending;
    private PopulationSession.FisheryWorksite target;private WaterSupportScan water;private ShorelineScan bank;
    private ShorelineScan.Position selected;private int finalStage;private boolean finalSafe=true;
    private UUID cursor;private long ticks,captured;private String diagnostic="Fishery workplace checks awaiting saved bindings";
    public PaperFisheryWorkplaces(PopulationSession session,FisheryShorelinePolicy policy,Logger logger){this(session,policy,logger,System::currentTimeMillis);}
    public PaperFisheryWorkplaces(PopulationSession session,FisheryShorelinePolicy policy,Logger logger,LongSupplier clock){this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.logger=Objects.requireNonNull(logger);this.clock=Objects.requireNonNull(clock);}
    public String diagnostic(){return diagnostic;}
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Fishery bank observer needs main thread");if(!policy.enabled()||!session.status().ready())return;ticks++;
        try {
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;clear();diagnostic="Fishery habitat/bank checked; resources and industry retained";return;}
            if(target!=null) {
                var world=Bukkit.getWorld(target.binding().world());if(world==null){clear();diagnostic="World absent; fishery data preserved";return;}
                if(selected!=null) {
                    var source=new PaperShorelineSource(world);int used=0;
                    while(finalStage<3&&used++<policy.cellsPerTick()) {
                        var cell=source.cell(new ShorelineScan.Position(selected.x(),selected.y()+finalStage-1,selected.z()));
                        if(cell!=(finalStage==0?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR))finalSafe=false;finalStage++;
                    }
                    if(finalStage<3)return;
                    if(!finalSafe){clear();diagnostic="Fishery bank changed during review; fresh evidence required, saved state retained";return;}
                    pending=session.reconcileFisheryWorksite(target.binding().site(),new ShorelineEvidence(world.getUID(),target.binding().point(),captured,bank.assessments()),water.observation(captured));return;
                }
                if(!water.complete()){water.advance(policy.cellsPerTick());return;}
                if(bank==null){bank=new ShorelineScan(new PaperShorelineSource(world),target.original(),target.binding().maximumRise());return;}
                bank.advance(policy.cellsPerTick());if(!bank.complete())return;
                var decision=ShorelineRecovery.decide(target.binding().point(),target.binding().maximumDistance(),bank.assessments());
                if(decision.confirmedSafe()){selected=decision.position();return;}
                var evidence=new ShorelineEvidence(world.getUID(),target.binding().point(),captured,bank.assessments());
                pending=session.reconcileFisheryWorksite(target.binding().site(),evidence,water.observation(captured));return;
            }
            if(page!=null) {
                if(!page.isDone())return;var candidates=page.join();page=null;
                if(candidates.isEmpty()){cursor=null;return;}target=candidates.getFirst();cursor=target.binding().site();var world=Bukkit.getWorld(target.binding().world());captured=clock.getAsLong();
                if(world==null||captured<=target.binding().observedMillis()||!world.isChunkLoaded(Math.floorDiv(target.binding().point().x(),16),Math.floorDiv(target.binding().point().z(),16))
                        ||target.original().evidence().columns().stream().anyMatch(c->!world.isChunkLoaded(Math.floorDiv(c.x(),16),Math.floorDiv(c.z(),16)))){clear();diagnostic="Unloaded bank/supports: last verified state preserved, no chunks loaded";return;}
                water=new WaterSupportScan(new PaperWaterSupportSource(world),target.original(),world.getMinHeight(),world.getMaxHeight(),target.binding().minimumDepth());return;
            }
            if(ticks%policy.intervalTicks()==1)page=session.fisheryWorksites(cursor,1);
        }catch(RuntimeException failure){clear();page=null;pending=null;diagnostic="Fishery worksite review deferred: "+failure.getMessage();logger.log(Level.WARNING,"Fishery workplace proof failed; saved state retained",failure);}
    }
    private void clear(){target=null;water=null;bank=null;selected=null;finalStage=0;finalSafe=true;}
}
