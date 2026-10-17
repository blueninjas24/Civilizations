package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.PenProtectionLifecycle.Release;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Explicit protection only. No donation, block ownership, adoption, inventory or animal mutation. */
public final class PaperPenControls extends BukkitRunnable implements Listener,CommandExecutor,AutoCloseable {
    public static final String ADMIN="civilizations.pen.admin",RELEASE="civilizations.pen.release";
    public record Site(UUID world,int y,PenScan.Column seed,int signX,int signY,int signZ) {}
    public record Frame(PenScan.Evidence evidence,Set<UUID> animals){public Frame{Objects.requireNonNull(evidence);animals=Set.copyOf(animals);}}
    interface Capture {void advance();boolean complete();Frame frame();}
    interface Owner {
        boolean ready();
        CompletableFuture<PenProtection> protect(PenProtection pen,String worldName);
        CompletableFuture<Optional<PenProtection>> find(UUID pen);
        CompletableFuture<List<UUID>> at(UUID world,int y,int x,int z);
        CompletableFuture<Release> release(Release request,boolean admin);
    }
    interface CaptureFactory {Capture start(Player player,Site site);}
    private record Preview(Site site,PenClaimPreview claim) {}
    private record ReleasePreview(Release request,long expires) {}
    private static final class Job {
        final UUID actor;final Site site;final Capture capture;final Preview previous;final long expires;
        Job(UUID actor,Site site,Capture capture,Preview previous,long expires){this.actor=actor;this.site=site;this.capture=capture;this.previous=previous;this.expires=expires;}
    }
    private record Pending(UUID actor,CompletableFuture<?> future,Consumer<Object> done) {}
    private final Owner owner;private final PenProtectionSettings settings;private final Function<UUID,Player> players;
    private final CaptureFactory captures;private final LongSupplier now;private final Logger logger;
    private final Map<UUID,Preview> previews=new HashMap<>();private final Map<UUID,ReleasePreview> releases=new HashMap<>();
    private final Deque<Job> jobs=new ArrayDeque<>();private final Map<UUID,Pending> pending=new HashMap<>();private boolean closed;

    public PaperPenControls(PopulationSession session,PenProtectionSettings settings,Logger logger){
        this(session,settings,logger,Bukkit::getPlayer);
    }
    PaperPenControls(PopulationSession session,PenProtectionSettings settings,Logger logger,Function<UUID,Player> players){
        this(new Owner(){public boolean ready(){return session.status().ready();}
            public CompletableFuture<PenProtection> protect(PenProtection pen,String worldName){return session.protectPen(pen,true,worldName);}
            public CompletableFuture<Optional<PenProtection>> find(UUID pen){return session.protectedPen(pen);}
            public CompletableFuture<List<UUID>> at(UUID world,int y,int x,int z){return session.protectedPensAt(world,y,x,z);}
            public CompletableFuture<Release> release(Release request,boolean admin){return session.releasePen(request,admin);}},
            settings,players,(player,site)->new PaperPenCapture(player.getWorld(),site,settings),System::currentTimeMillis,logger);
    }
    PaperPenControls(Owner owner,PenProtectionSettings settings,Function<UUID,Player> players,CaptureFactory captures,LongSupplier now,Logger logger){
        this.owner=owner;this.settings=settings;this.players=players;this.captures=captures;this.now=now;this.logger=logger;
    }
    private void say(UUID player,String text){var p=players.apply(player);if(p!=null)p.sendMessage(text);}
    private boolean available(Player player){
        if(closed||!settings.enabled()||!owner.ready()){player.sendMessage("Pen protection is disabled or still starting.");return false;}
        return true;
    }
    private boolean occupied(UUID player){return pending.containsKey(player)||jobs.stream().anyMatch(j->j.actor.equals(player));}
    private boolean room(UUID player){
        var all=new HashSet<>(previews.keySet());all.addAll(releases.keySet());all.addAll(pending.keySet());for(var job:jobs)all.add(job.actor);
        return all.contains(player)||all.size()<settings.maximumRequests();
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void interact(PlayerInteractEvent event){
        if(event.getHand()!=EquipmentSlot.HAND||event.getAction()!=Action.RIGHT_CLICK_BLOCK||event.getClickedBlock()==null)return;
        if(!(event.getClickedBlock().getState() instanceof Sign sign))return;
        if(!protectionSign(sign)||!event.getClickedBlock().getWorld().getUID().equals(event.getPlayer().getWorld().getUID()))return;
        event.setCancelled(true);
        preview(event.getPlayer(),sign.getX(),sign.getY(),sign.getZ());
    }
    static boolean protectionSign(org.bukkit.block.BlockState state){
        if(!(state instanceof Sign sign))return false;
        var plain=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
        return plain.serialize(sign.getSide(Side.FRONT).line(0)).trim().equalsIgnoreCase("[Civ Protect]")
                ||plain.serialize(sign.getSide(Side.BACK).line(0)).trim().equalsIgnoreCase("[Civ Protect]");
    }
    /** Sign interaction entry, also used by the guarded native fixture. */
    public void preview(Player player,int signX,int signY,int signZ){
        if(!available(player))return;
        if(!player.hasPermission(ADMIN)){player.sendMessage("An admin must resolve this pen's ownership. A sign alone grants no claim.");return;}
        var id=player.getUniqueId();if(occupied(id)||!room(id)){player.sendMessage("Pen review busy; try again shortly.");return;}
        var p=player.getLocation();
        if(p.getWorld()==null||Math.abs((long)p.getBlockX()-signX)>6||Math.abs((long)p.getBlockZ()-signZ)>6||Math.abs((long)p.getBlockY()-signY)>2){player.sendMessage("Stand inside the pen beside its protection sign.");return;}
        var site=new Site(p.getWorld().getUID(),p.getBlockY(),new PenScan.Column(p.getBlockX(),p.getBlockZ()),signX,signY,signZ);
        previews.remove(id);releases.remove(id);enqueue(player,site,null);
    }
    private void enqueue(Player player,Site site,Preview previous){
        try{jobs.addLast(new Job(player.getUniqueId(),site,captures.start(player,site),previous,Math.addExact(now.getAsLong(),30_000)));
            player.sendMessage(previous==null?"Checking this loaded pen. Nothing is claimed yet.":"Rechecking the pen and animals before protection.");}
        catch(RuntimeException failure){player.sendMessage("Cannot inspect this pen: "+failure.getMessage());}
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Use pen controls in game.");return true;}
        if(!available(player))return true;
        var id=player.getUniqueId();String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"";
        if(action.equals("cancel")&&args.length==2){
            previews.remove(id);releases.remove(id);jobs.removeIf(j->j.actor.equals(id));
            player.sendMessage(pending.containsKey(id)?"A submitted save is already finishing; wait for its result.":"Pen preview cancelled.");return true;
        }
        if(occupied(id)){player.sendMessage("Your pen request is still finishing.");return true;}
        if(action.equals("here")&&args.length==2){
            if(!player.hasPermission(RELEASE)&&!player.hasPermission(ADMIN)){player.sendMessage("You cannot inspect pen protection.");return true;}
            if(!room(id)){player.sendMessage("Pen review busy; try again shortly.");return true;}
            var p=player.getLocation();var world=p.getWorld();if(world==null)return true;
            submit(id,owner.at(world.getUID(),p.getBlockY(),p.getBlockX(),p.getBlockZ()),value->{
                @SuppressWarnings("unchecked") var found=(List<UUID>)value;
                say(id,found.isEmpty()?"No saved active pen at that position.":"Saved protection here: "+found+". Use an exact ID for /civ pen release <ID>.");
            });return true;
        }
        if(action.equals("confirm")&&args.length==2){
            var preview=previews.get(id);
            if(preview==null){player.sendMessage("Right-click a [Civ Protect] sign from inside the pen first.");return true;}
            if(!player.hasPermission(ADMIN)||now.getAsLong()>=preview.claim.expiresMillis()||!samePlace(player,preview.site)){
                previews.remove(id);player.sendMessage("Pen permission, position or preview changed; request a new preview.");return true;}
            previews.remove(id);enqueue(player,preview.site,preview);return true;
        }
        if(action.equals("release")&&args.length==3){
            if(!player.hasPermission(RELEASE)&&!player.hasPermission(ADMIN)){player.sendMessage("You cannot release pen protection.");return true;}
            if(!room(id)){player.sendMessage("Pen review busy; try again shortly.");return true;}
            try{UUID pen=UUID.fromString(args[2]);long expires=now.getAsLong()+settings.previewSeconds()*1000L;
                submit(id,owner.find(pen),value->{
                    @SuppressWarnings("unchecked") var saved=(Optional<PenProtection>)value;
                    var current=players.apply(id);
                    if(current==null||now.getAsLong()>=expires)return;
                    if(saved.isEmpty()||(!current.hasPermission(ADMIN)&&!saved.orElseThrow().owner().equals(id))){say(id,"Unknown pen or you are not its recorded owner.");return;}
                    if(!current.hasPermission(ADMIN)&&!current.hasPermission(RELEASE)){say(id,"Pen release permission changed.");return;}
                    previews.remove(id);releases.put(id,new ReleasePreview(new Release(UUID.randomUUID(),pen,id,now.getAsLong(),"Explicit in-game area release"),expires));
                    say(id,"Release area "+pen+"? Animal ownership and history will stay protected. Use /civ pen confirmrelease.");
                });
            }catch(IllegalArgumentException failure){player.sendMessage("Pen ID must be a UUID.");}return true;
        }
        if(action.equals("confirmrelease")&&args.length==2){
            var preview=releases.remove(id);
            if(preview==null||now.getAsLong()>=preview.expires||(!player.hasPermission(ADMIN)&&!player.hasPermission(RELEASE))){player.sendMessage("Release preview expired or permission changed; request it again.");return true;}
            submit(id,owner.release(preview.request,player.hasPermission(ADMIN)),value->say(id,"Pen area released. Animal ownership and history retained."));return true;
        }
        player.sendMessage("Right-click a [Civ Protect] sign inside a pen, then /civ pen confirm. /civ pen here shows saved IDs. Release: /civ pen release <pen UUID>, then confirmrelease. /civ pen cancel cancels a preview.");return true;
    }
    private boolean samePlace(Player player,Site site){var p=player.getLocation();return p.getWorld()!=null&&p.getWorld().getUID().equals(site.world)&&p.getBlockY()==site.y
            &&Math.abs((long)p.getBlockX()-site.signX)<=6&&Math.abs((long)p.getBlockZ()-site.signZ)<=6;}
    private void submit(UUID actor,CompletableFuture<?> future,Consumer<Object> done){pending.put(actor,new Pending(actor,future,done));}
    @Override public void run(){
        if(closed)return;long time=now.getAsLong();previews.entrySet().removeIf(e->time>=e.getValue().claim.expiresMillis());releases.entrySet().removeIf(e->time>=e.getValue().expires);
        for(var p:List.copyOf(pending.values()))if(p.future.isDone()){
            pending.remove(p.actor);
            try{p.done.accept(p.future.join());}catch(RuntimeException failure){
                var cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                say(p.actor,"Pen request was not confirmed: "+cause.getMessage()+". Use /civ pen here before retrying an uncertain save.");
                if(!(cause instanceof com.blueninjas24.civilizations.database.PenProtectionLifecycle.Declined))logger.log(Level.WARNING,"Pen request failed",cause);
            }
        }
        var job=jobs.pollFirst();if(job==null)return;var player=players.apply(job.actor);
        if(player==null||!player.hasPermission(ADMIN)||!samePlace(player,job.site)||time>=job.expires){say(job.actor,"Pen review expired or permission/position changed; try again.");return;}
        try{
            job.capture.advance();if(!job.capture.complete()){jobs.addLast(job);return;}
            var frame=job.capture.frame();
            if(frame.evidence.state()!=PenScan.State.ENCLOSED){say(job.actor,"No complete supported pen: "+frame.evidence.state()+". Check fences, flat footing and loaded chunks.");return;}
            // Domain validation also checks sign adjacency and connected finite geometry.
            var proposed=new PenProtection(UUID.randomUUID(),job.site.world,job.actor,job.site.y,job.site.signX,job.site.signY,job.site.signZ,job.site.seed,frame.evidence,frame.animals,time);
            if(job.previous==null){
                var claim=new PenClaimPreview(job.actor,job.site.world,job.site.y,time+settings.previewSeconds()*1000L,frame.evidence,frame.animals);
                previews.put(job.actor,new Preview(job.site,claim));
                say(job.actor,"Pen preview: "+frame.evidence.interior().size()+" floor columns; "+frame.animals.size()+" cows/chickens. Owner: you. Nothing claimed yet. /civ pen confirm within "+settings.previewSeconds()+" seconds.");
                var outline=frame.evidence.boundary().stream().sorted(Comparator.comparingInt(PenScan.Column::x).thenComparingInt(PenScan.Column::z)).toList();
                int stride=Math.max(1,(outline.size()+127)/128);
                for(int i=0;i<outline.size();i+=stride){var point=outline.get(i);player.spawnParticle(Particle.HAPPY_VILLAGER,point.x()+0.5,job.site.y+0.5,point.z()+0.5,1,0,0,0,0);}
            }else{
                var check=job.previous.claim.confirm(job.actor,job.site.world,job.site.y,time,player.hasPermission(ADMIN),frame.evidence,frame.animals);
                if(check!=PenClaimPreview.Result.READY){say(job.actor,"Pen or animal roster changed ("+check+"); request a new preview.");return;}
                submit(job.actor,owner.protect(proposed,player.getWorld().getName()),value->say(job.actor,"Pen protected: "+((PenProtection)value).id()+". Animal ownership survives escapes and broken signs. This does not claim building blocks."));
            }
        }catch(RuntimeException failure){say(job.actor,"Pen inspection refused: "+failure.getMessage());}
    }
    @Override public void close(){closed=true;jobs.clear();previews.clear();releases.clear();pending.clear();}
}
