package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.ForestRecovery;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.function.BiConsumer;

/** Explicit forest recovery from complete loaded snapshots, including canopy and protected coordinates. */
public final class PaperForestRecovery implements CommandExecutor {
    private record Review(UUID token,PopulationSession.ForestReview snapshot,Map<Position,ForestRecovery.Verdict> evidence,long expires) {}
    private final JavaPlugin plugin;private final PopulationSession session;private final ForestScan.Geometry geometry;
    private final Map<String,Review> reviews=new HashMap<>();private final Set<String> scanning=new HashSet<>();
    public PaperForestRecovery(JavaPlugin plugin,PopulationSession session,ForestScan.Geometry geometry){this.plugin=Objects.requireNonNull(plugin);this.session=Objects.requireNonNull(session);this.geometry=Objects.requireNonNull(geometry);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Forest review requires main thread");
        if(!allowed(sender))return true;String actor=actor(sender);
        reviews.values().removeIf(r->r.expires()<System.currentTimeMillis());
        if(args.length==3&&args[2].equalsIgnoreCase("list")) {
            session.forestRecoveryCandidates().whenComplete((ids,failure)->dispatch(()->{
                if(!allowed(sender))return;if(failure!=null){failed(sender,failure);return;}
                sender.sendMessage("Saved forest site IDs (at most32):");ids.stream().limit(32).forEach(id->sender.sendMessage(id.toString()));
                if(ids.size()>32)sender.sendMessage("More saved sites exist; this list is bounded.");
                if(ids.isEmpty())sender.sendMessage("No saved forestry resources.");
            }));return true;
        }
        if(args.length==4&&args[2].equalsIgnoreCase("review")) {
            UUID site;try{site=UUID.fromString(args[3]);}catch(IllegalArgumentException invalid){sender.sendMessage("Forest site ID must be a UUID.");return true;}
            if(!begin(sender,actor))return true;reviews.remove(actor);
            session.prepareForestRecovery(site).whenComplete((snapshot,failure)->dispatch(()->{
                if(failure!=null){scanning.remove(actor);failed(sender,failure);return;}
                scan(sender,actor,snapshot,(evidence,captured)->{
                    var after=ForestRecovery.preview(snapshot.plan(),evidence);var before=snapshot.plan().resource().stock();var token=UUID.randomUUID();
                    reviews.put(actor,new Review(token,snapshot,evidence,System.currentTimeMillis()+120_000));
                    sender.sendMessage("Complete forest review: "+snapshot.plan().supports().size()+" supports; capacity "+before.capacity()+" -> "+after.capacity()+"; standing timber "+before.available()+" -> "+after.available()+".");
                    sender.sendMessage("Accept historical uncertainty for this saved forest only. No refill, protection removal or new discovery.");
                    sender.sendMessage("Approve within120 seconds: /civ debug recover-forest confirm "+token);
                });
            }));return true;
        }
        if(args.length==4&&args[2].equalsIgnoreCase("confirm")) {
            var review=reviews.get(actor);
            if(review==null||!review.token().toString().equalsIgnoreCase(args[3])||review.expires()<System.currentTimeMillis()) {
                sender.sendMessage("No current matching forest preview; request a fresh review.");return true;
            }
            if(!begin(sender,actor))return true;reviews.remove(actor);
            scan(sender,actor,review.snapshot(),(evidence,captured)->{
                if(!evidence.equals(review.evidence())){sender.sendMessage("Forest changed since preview; nothing applied. Review again.");return;}
                session.approveForestRecovery(review.token(),actor,review.snapshot(),evidence,captured).whenComplete((receipt,failure)->dispatch(()->{
                    if(!allowed(sender))return;if(failure!=null){failed(sender,failure);return;}
                    sender.sendMessage("Reviewed forest restored for current capture: "+receipt.site()+"; capacity "+receipt.capacity()+"; standing timber "+receipt.available()+"; lost "+receipt.lost()+". Goods/history preserved.");
                }));
            });return true;
        }
        sender.sendMessage("Usage: /civ debug recover-forest list | review <site-uuid> | confirm <token>");return true;
    }
    private boolean begin(CommandSender sender,String actor) {
        if(!session.forestCaptureReady()){sender.sendMessage("Current forest capture is unavailable; restore it before reviewing.");return false;}
        if(scanning.contains(actor)||scanning.size()>=2||reviews.size()+scanning.size()>=16&&!reviews.containsKey(actor)) {
            sender.sendMessage("Forest review busy; wait for current scans/previews.");return false;
        }
        scanning.add(actor);sender.sendMessage("Reviewing loaded forest supports and natural canopy; no chunks will be loaded or blocks changed.");return true;
    }
    private void scan(CommandSender sender,String actor,PopulationSession.ForestReview review,BiConsumer<Map<Position,ForestRecovery.Verdict>,Long> done) {
        try {
            var plan=review.plan();var world=Bukkit.getWorld(plan.resource().world());if(world==null)throw new IllegalStateException("Forest world absent");
            int radius=geometry.horizontalCanopyRadius();var positions=plan.supports();
            int minX=Math.subtractExact(positions.stream().mapToInt(Position::x).min().orElseThrow(),radius),maxX=Math.addExact(positions.stream().mapToInt(Position::x).max().orElseThrow(),radius+1);
            int minZ=Math.subtractExact(positions.stream().mapToInt(Position::z).min().orElseThrow(),radius),maxZ=Math.addExact(positions.stream().mapToInt(Position::z).max().orElseThrow(),radius+1);
            int minY=Math.max(world.getMinHeight(),Math.subtractExact(positions.stream().mapToInt(Position::y).min().orElseThrow(),1));
            int maxY=Math.min(world.getMaxHeight(),Math.addExact(positions.stream().mapToInt(Position::y).max().orElseThrow(),geometry.minimumVerticalLogs()+geometry.verticalCanopyRadius()+2));
            var bounds=new ForestScan.Bounds(minX,minY,minZ,maxX,maxY,maxZ);
            if((long)(maxX-minX)*(maxY-minY)*(maxZ-minZ)>200_000)throw new IllegalStateException("Saved forest exceeds complete scan budget");
            long captured=System.currentTimeMillis();var snapshots=new HashMap<OreSurveyArea.Chunk,ChunkSnapshot>();
            for(int x=Math.floorDiv(minX,16);x<=Math.floorDiv(maxX-1,16);x++)for(int z=Math.floorDiv(minZ,16);z<=Math.floorDiv(maxZ-1,16);z++) {
                if(snapshots.size()==9||!world.isChunkLoaded(x,z))throw new IllegalStateException("Every canopy/source chunk must already be loaded; no loss inferred");
                snapshots.put(new OreSurveyArea.Chunk(x,z),world.getChunkAt(x,z).getChunkSnapshot(false,false,false,false));
            }
            var source=new PaperForestSource(snapshots,world.getMinHeight(),world.getMaxHeight());
            for(var p:positions)if(source.kind(p.x(),minY,p.z())==ForestScan.Kind.LOG||source.kind(p.x(),maxY-1,p.z())==ForestScan.Kind.LOG)
                throw new IllegalStateException("Tree extends beyond review bounds; no incomplete habitat verdict applied");
            var excluded=new HashSet<>(plan.excluded());excluded.addAll(plan.retired());
            var scan=new ForestScan(source,bounds,excluded,geometry);
            new BukkitRunnable(){@Override public void run(){try {
                if(!allowed(sender)){stop();return;}
                if(!session.forestCaptureReady()||session.forestMutationRevision()!=review.revision()||System.currentTimeMillis()-captured>30_000)
                    throw new IllegalStateException("Forest capture changed or expired; review again");
                if(Bukkit.getWorld(world.getUID())!=world||snapshots.keySet().stream().anyMatch(p->!world.isChunkLoaded(p.x(),p.z())))
                    throw new IllegalStateException("World/canopy chunk unloaded; no loss inferred");
                scan.advance(4096);if(!scan.complete())return;
                var observed=scan.observation(world.getUID(),captured);
                if(!observed.complete())throw new IllegalStateException("Forest geometry incomplete; nothing applied");
                var suitable=Set.copyOf(observed.trunks());var evidence=new HashMap<Position,ForestRecovery.Verdict>();
                for(var p:positions){var kind=source.kind(p.x(),p.y(),p.z());if(kind==ForestScan.Kind.UNKNOWN)throw new IllegalStateException("Unknown source coordinate");
                    evidence.put(p,new ForestRecovery.Verdict(kind==ForestScan.Kind.LOG,suitable.contains(p)));}
                stop();done.accept(Map.copyOf(evidence),captured);
            }catch(RuntimeException failure){stop();failed(sender,failure);}}
                private void stop(){cancel();scanning.remove(actor);}
            }.runTaskTimer(plugin,1,1);
        }catch(RuntimeException failure){scanning.remove(actor);failed(sender,failure);}
    }
    private boolean allowed(CommandSender sender){return plugin.isEnabled()&&sender.hasPermission("civilizations.debug")&&(!(sender instanceof Player p)||p.isOnline());}
    private static String actor(CommandSender sender){return sender instanceof Player p?p.getUniqueId().toString():"server-console";}
    private void dispatch(Runnable action){if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,action);}
    private void failed(CommandSender sender,Throwable failure){while(failure.getCause()!=null)failure=failure.getCause();if(allowed(sender))sender.sendMessage("Forest review not applied: "+failure.getMessage());}
}
