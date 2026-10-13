package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.FisheryInvestment;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.Resident;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import java.util.logging.*;

/** One qualified proposal/live support+bank proof/paid acceptance in flight. No chunk loads or block edits. */
public final class PaperFisheryStartup extends BukkitRunnable {
    private final PopulationSession session;private final FisheryShorelinePolicy policy;private final WaterObserverPolicy waterPolicy;
    private final Logger logger;private final LongSupplier clock;
    private CompletableFuture<FisheryInvestment.Preparation> preparation;private CompletableFuture<?> acceptance;
    private FisheryInvestment.Proposal proposal;private WaterSupportScan water;private ShorelineScan bank;
    private ShorelineScan.Position selected;private int finalStage;private boolean finalSafe=true;
    private long captured,ticks;private UUID cursor;
    private String diagnostic="Automatic fisheries awaiting qualified saved owners";
    public PaperFisheryStartup(PopulationSession session,FisheryShorelinePolicy policy,WaterObserverPolicy waterPolicy,Logger logger){this(session,policy,waterPolicy,logger,System::currentTimeMillis);}
    public PaperFisheryStartup(PopulationSession session,FisheryShorelinePolicy policy,WaterObserverPolicy waterPolicy,Logger logger,LongSupplier clock) {
        this.session=Objects.requireNonNull(session);this.policy=Objects.requireNonNull(policy);this.waterPolicy=Objects.requireNonNull(waterPolicy);this.logger=Objects.requireNonNull(logger);this.clock=Objects.requireNonNull(clock);
    }
    public String diagnostic(){return diagnostic;}
    @Override public void run() {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Fishery startup needs main thread");
        if(!policy.enabled()||!session.status().ready())return;ticks++;
        try {
            if(acceptance!=null){if(!acceptance.isDone())return;acceptance.join();acceptance=null;reset();diagnostic="Automatic fishery started with real protected funds, owner and habitat";return;}
            if(proposal!=null) {
                var world=Bukkit.getWorld(proposal.opportunity().water().evidence().world());if(world==null){reset();diagnostic="Fishery waits: world absent";return;}
                if(selected!=null) {
                    var source=new PaperShorelineSource(world);int used=0;
                    while(finalStage<3&&used++<policy.cellsPerTick()) {
                        var cell=source.cell(new ShorelineScan.Position(selected.x(),selected.y()+finalStage-1,selected.z()));
                        if(cell!=(finalStage==0?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR))finalSafe=false;finalStage++;
                    }
                    if(finalStage<3)return;
                    if(!finalSafe||proposal.opportunity().water().evidence().columns().stream().anyMatch(c->!world.isChunkLoaded(Math.floorDiv(c.x(),16),Math.floorDiv(c.z(),16)))){reset();diagnostic="Fishery waits: bank/supports changed or unloaded; nothing spent";return;}
                    acceptance=session.acceptFisheryInvestment(proposal,new ShorelineEvidence(world.getUID(),selected,captured,bank.assessments()));return;
                }
                if(water!=null){water.advance(policy.cellsPerTick());if(!water.complete())return;if(!water.allSuitable()){reset();diagnostic="Fishery waits for actual open water at saved level/depth";return;}
                    water=null;bank=new ShorelineScan(new PaperShorelineSource(world),proposal.opportunity().water(),policy.maximumRise());return;}
                bank.advance(policy.cellsPerTick());if(!bank.complete())return;
                if(bank.suitable().isEmpty()){reset();diagnostic="Fishery waits for a safe nearby dry bank; no repairs or chunks loaded";return;}
                selected=bank.suitable().getFirst();return;
            }
            if(preparation!=null) {
                if(!preparation.isDone())return;var result=preparation.join();preparation=null;diagnostic=result.explanation();
                if(result.proposal().isEmpty())return;proposal=result.proposal().orElseThrow();var world=Bukkit.getWorld(proposal.opportunity().water().evidence().world());
                if(world==null||proposal.opportunity().water().evidence().columns().stream().anyMatch(c->!world.isChunkLoaded(Math.floorDiv(c.x(),16),Math.floorDiv(c.z(),16)))){reset();diagnostic="First fishery waits for loaded supports; no chunks loaded";return;}captured=clock.getAsLong();water=new WaterSupportScan(new PaperWaterSupportSource(world),proposal.opportunity().water(),world.getMinHeight(),world.getMaxHeight(),waterPolicy.minimumDepth());return;
            }
            if(ticks%policy.intervalTicks()!=1)return;
            var towns=new TreeSet<UUID>(Comparator.comparing(UUID::toString));
            session.projectionView().ifPresent(v->v.population().residents().stream().filter(r->r.status()==Resident.Status.ALIVE).forEach(r->towns.add(r.settlementId())));
            if(towns.size()>1024)throw new IllegalStateException("Fishery town budget exceeded");if(towns.isEmpty())return;
            var next=cursor==null?towns.first():towns.higher(cursor);if(next==null)next=towns.first();cursor=next;preparation=session.prepareFisheryInvestment(next);
        }catch(RuntimeException failure){var cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;reset();preparation=null;acceptance=null;diagnostic="Fishery startup deferred: "+cause.getMessage();if(!(cause instanceof com.blueninjas24.civilizations.database.BusinessStartupGrants.Declined))logger.log(Level.WARNING,"Fishery startup failed; saved state retained",failure);}
    }
    private void reset(){proposal=null;water=null;bank=null;selected=null;finalStage=0;finalSafe=true;}
}
