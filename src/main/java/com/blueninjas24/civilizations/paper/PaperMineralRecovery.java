package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.MineralRecovery;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.function.Consumer;

/** Admin attestation plus two complete bounded loaded-block scans. Never edits or loads blocks. */
public final class PaperMineralRecovery implements CommandExecutor {
    private record Review(UUID token,String actor,PopulationSession.MineralReview snapshot,
                          Map<Position,Boolean> evidence,long expires) {}
    private final JavaPlugin plugin;
    private final PopulationSession session;
    private final Map<String,Review> reviews=new HashMap<>();
    private final Set<String> scanning=new HashSet<>();
    public PaperMineralRecovery(JavaPlugin plugin,PopulationSession session){this.plugin=Objects.requireNonNull(plugin);this.session=Objects.requireNonNull(session);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Resource review must start on server thread");
        if(!allowed(sender))return true;
        String actor=actor(sender);
        reviews.values().removeIf(r->r.expires()<System.currentTimeMillis());
        if(args.length==3&&args[2].equalsIgnoreCase("list")) {
            session.mineralRecoveryCandidates().whenComplete((ids,failure)->dispatch(()->{
                if(!allowed(sender))return;
                if(failure!=null){failed(sender,failure);return;}
                sender.sendMessage("Saved mineral deposit IDs (at most32; review one at a time):");
                ids.stream().limit(32).forEach(id->sender.sendMessage(id.toString()));
                if(ids.size()>32)sender.sendMessage("More deposits exist; this list is bounded. Use saved administrative records for other IDs.");
                if(ids.isEmpty())sender.sendMessage("No saved coordinate-backed mineral deposits.");
            }));return true;
        }
        if(args.length==4&&args[2].equalsIgnoreCase("review")) {
            final UUID deposit;
            try{deposit=UUID.fromString(args[3]);}catch(IllegalArgumentException invalid){sender.sendMessage("Deposit ID must be a UUID.");return true;}
            if(!begin(sender,actor))return true;
            reviews.remove(actor);
            session.prepareMineralRecovery(deposit).whenComplete((snapshot,failure)->dispatch(()->{
                if(failure!=null){scanning.remove(actor);failed(sender,failure);return;}
                scan(sender,actor,snapshot,evidence->{
                    var token=UUID.randomUUID();var review=new Review(token,actor,snapshot,evidence,System.currentTimeMillis()+120_000);
                    reviews.put(actor,review);
                    long loss=snapshot.plan().sources().stream().filter(s->s.excluded()||!evidence.get(s.position())).mapToLong(MineralRecovery.Source::remaining).sum();
                    sender.sendMessage("Complete review: "+snapshot.plan().sources().size()+" saved coordinates; remaining reserve "+snapshot.plan().remaining()+"; confirmed loss "+loss+".");
                    sender.sendMessage("Current blocks cannot prove what changed during missing history. Confirm only if you accept that uncertainty for this deposit.");
                    sender.sendMessage("No refill or protection removal. Unreviewed resources and new discovery remain paused.");
                    sender.sendMessage("Approve within120 seconds: /civ debug recover-ore confirm "+token);
                });
            }));return true;
        }
        if(args.length==4&&args[2].equalsIgnoreCase("confirm")) {
            var review=reviews.get(actor);
            if(review==null||!review.token().toString().equalsIgnoreCase(args[3])||review.expires()<System.currentTimeMillis()) {
                sender.sendMessage("No matching current review. Run /civ debug recover-ore review <deposit-uuid>.");return true;
            }
            if(!begin(sender,actor))return true;
            reviews.remove(actor); // Single use, even when a fresh scan fails.
            long captured=System.currentTimeMillis();
            scan(sender,actor,review.snapshot(),evidence->{
                if(!evidence.equals(review.evidence())){sender.sendMessage("Blocks changed since preview; request a fresh review. Nothing applied.");return;}
                session.approveMineralRecovery(review.token(),actor,review.snapshot(),evidence,captured).whenComplete((receipt,failure)->dispatch(()->{
                    if(!allowed(sender))return;
                    if(failure!=null){failed(sender,failure);return;}
                    sender.sendMessage("Reviewed deposit restored for current capture: "+receipt.deposit()+"; reserve "+receipt.before()+" -> "+receipt.after()+"; lost "+receipt.lost()+". Existing goods/history preserved.");
                }));
            });return true;
        }
        sender.sendMessage("Usage: /civ debug recover-ore list | review <deposit-uuid> | confirm <token>");return true;
    }
    private boolean begin(CommandSender sender,String actor) {
        if(!session.oreCaptureReady()){sender.sendMessage("Current capture is not ready. Restore working capture before reviewing resources.");return false;}
        if(scanning.contains(actor)||scanning.size()>=4||reviews.size()+scanning.size()>=16&&!reviews.containsKey(actor)) {
            sender.sendMessage("Resource review busy; wait for current scans or previews to expire.");return false;
        }
        scanning.add(actor);sender.sendMessage("Scanning saved source blocks without loading chunks or changing the world.");return true;
    }
    private void scan(CommandSender sender,String actor,PopulationSession.MineralReview review,Consumer<Map<Position,Boolean>> done) {
        new BukkitRunnable() {
            int cursor;final Map<Position,Boolean> evidence=new HashMap<>();final long started=System.currentTimeMillis();
            @Override public void run() {
                try {
                    if(!allowed(sender)) { stop();return; }
                    if(!session.oreCaptureReady()||session.oreMutationRevision()!=review.revision()||System.currentTimeMillis()-started>30_000)
                        throw new IllegalStateException("Capture changed or scan expired; request a fresh review");
                    var world=Bukkit.getWorld(review.plan().world());
                    if(world==null)throw new IllegalStateException("Resource world is absent");
                    int end=Math.min(review.plan().sources().size(),cursor+128);
                    while(cursor<end) {
                        var source=review.plan().sources().get(cursor++);var p=source.position();
                        if(p.y()<world.getMinHeight()||p.y()>=world.getMaxHeight()||!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16)))
                            throw new IllegalStateException("All saved source chunks must remain loaded; no loss inferred");
                        evidence.put(p,matches(review.plan().mineral(),world.getBlockAt(p.x(),p.y(),p.z()).getType()));
                    }
                    if(cursor==review.plan().sources().size()) {
                        for(var s:review.plan().sources())if(!world.isChunkLoaded(Math.floorDiv(s.position().x(),16),Math.floorDiv(s.position().z(),16)))
                            throw new IllegalStateException("A source chunk unloaded during review; nothing applied");
                        stop();done.accept(Map.copyOf(evidence));
                    }
                }catch(RuntimeException failure){stop();failed(sender,failure);}
            }
            private void stop(){cancel();scanning.remove(actor);}
        }.runTaskTimer(plugin,1,1);
    }
    static boolean matches(String mineral,Material material) {
        return switch(mineral) {
            case "coal" -> material==Material.COAL_ORE||material==Material.DEEPSLATE_COAL_ORE;
            case "iron" -> material==Material.IRON_ORE||material==Material.DEEPSLATE_IRON_ORE;
            case "stone" -> material==Material.STONE||material==Material.DEEPSLATE;
            case "sand" -> material==Material.SAND||material==Material.RED_SAND;
            default -> throw new IllegalArgumentException("Unsupported saved mineral; no recovery applied");
        };
    }
    private boolean allowed(CommandSender sender) {
        return plugin.isEnabled()&&sender.hasPermission("civilizations.debug")&&(!(sender instanceof Player p)||p.isOnline());
    }
    private static String actor(CommandSender sender){return sender instanceof Player p?p.getUniqueId().toString():"server-console";}
    private void dispatch(Runnable task){if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,task);}
    private void failed(CommandSender sender,Throwable failure) {
        while(failure.getCause()!=null)failure=failure.getCause();
        if(allowed(sender))sender.sendMessage("Resource review not applied: "+failure.getMessage());
    }
}
