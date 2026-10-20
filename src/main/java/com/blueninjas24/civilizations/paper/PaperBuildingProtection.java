package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.BuildingLayout.Bounds;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.BuildingRegistry.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Explicit player-declared building exclusions only: no adoption, world writes or housing activation. */
public final class PaperBuildingProtection extends BukkitRunnable implements CommandExecutor,TabCompleter,Listener,AutoCloseable {
    public static final String MANAGE="civilizations.building.protect",ADMIN="civilizations.building.admin";
    public interface Owner {
        boolean ready();
        CompletableFuture<Exclusion> protect(Exclusion request,String worldName);
        CompletableFuture<Optional<Exclusion>> find(UUID id);
        CompletableFuture<List<UUID>> at(UUID world,int x,int z);
        CompletableFuture<Void> release(UUID operation,Exclusion request,UUID actor,UUID currentWorld,boolean admin,long millis);
    }
    private record Selection(UUID world,Position first,Position second,long touched) {}
    private record Proposal(Exclusion exclusion,long expires) {}
    private static final class Release {
        final UUID operation=UUID.randomUUID();final Exclusion exclusion;final long expires;Long submitted;boolean submittedAdmin;
        Release(Exclusion exclusion,long expires){this.exclusion=exclusion;this.expires=expires;}
    }
    private record Pending(CompletableFuture<?> future,Consumer<Object> done) {}
    private final Owner owner;private final BuildingProtectionSettings settings;private final Function<UUID,Player> players;
    private final LongSupplier now;private final Logger logger;
    private final Map<UUID,Selection> selections=new HashMap<>();private final Map<UUID,Proposal> proposals=new HashMap<>();
    private final Map<UUID,Release> releases=new HashMap<>();private final Map<UUID,Pending> pending=new HashMap<>();
    private boolean closed;
    private CommandExecutor inspection;
    private CommandExecutor homes;
    public void configureInspection(CommandExecutor inspection){this.inspection=Objects.requireNonNull(inspection);}
    public void configureHomes(CommandExecutor homes){this.homes=Objects.requireNonNull(homes);}
    /** Immutable selection only; caller supplies its own current action permission checks. */
    public Bounds selectedHomeBounds(Player player){prune();return bounds(player);}
    public PaperBuildingProtection(PopulationSession session,BuildingProtectionSettings settings,Logger logger) {
        this(new Owner(){public boolean ready(){return session.status().ready();}
            public CompletableFuture<Exclusion> protect(Exclusion request,String name){return session.protectBuilding(request,name,true);}
            public CompletableFuture<Optional<Exclusion>> find(UUID id){return session.buildingExclusion(id);}
            public CompletableFuture<List<UUID>> at(UUID world,int x,int z){return session.buildingExclusionsAt(world,x,z);}
            public CompletableFuture<Void> release(UUID op,Exclusion e,UUID actor,UUID world,boolean admin,long millis){return session.releaseBuildingExclusion(op,e.id(),actor,world,admin,millis);}},
            settings,Bukkit::getPlayer,System::currentTimeMillis,logger);
    }
    public PaperBuildingProtection(Owner owner,BuildingProtectionSettings settings,Function<UUID,Player> players,LongSupplier now,Logger logger) {
        this.owner=Objects.requireNonNull(owner);this.settings=Objects.requireNonNull(settings);this.players=players;this.now=now;this.logger=logger;
    }
    private boolean permitted(Player p){return p.hasPermission(MANAGE)||p.hasPermission(ADMIN);}
    private void say(UUID actor,String text){var player=players.apply(actor);if(player!=null)player.sendMessage(text);}
    private boolean available(Player p) {
        if(closed||!settings.enabled()||!owner.ready()){p.sendMessage("Building protection is disabled or still starting.");return false;}
        if(!permitted(p)){p.sendMessage("You do not have building-protection permission.");return false;}return true;
    }
    private void prune() {
        long time=now.getAsLong();selections.values().removeIf(s->time<s.touched()||time-s.touched()>=settings.selectionSeconds()*1000L);
        proposals.values().removeIf(p->time<p.exclusion().createdMillis()||time>=p.expires());
        releases.values().removeIf(r->time<r.expires-settings.previewSeconds()*1000L||time>=r.expires);
    }
    private boolean room(UUID actor) {
        prune();var all=new HashSet<>(selections.keySet());all.addAll(proposals.keySet());all.addAll(releases.keySet());all.addAll(pending.keySet());
        return all.contains(actor)||all.size()<settings.maximumRequests();
    }
    public static Bounds selectedBounds(Position a,Position b,BuildingProtectionSettings settings) {
        var min=new Position(Math.min(a.x(),b.x()),Math.min(a.y(),b.y()),Math.min(a.z(),b.z()));
        var max=new Position(Math.max(a.x(),b.x()),Math.max(a.y(),b.y()),Math.max(a.z(),b.z()));
        var bounds=new Bounds(min,max);long width=(long)max.x()-min.x()+1,depth=(long)max.z()-min.z()+1;
        if(width>settings.maximumEdge()||depth>settings.maximumEdge()||width*depth>settings.maximumArea())
            throw new IllegalArgumentException("Plot exceeds configured edge/area limits");return bounds;
    }
    private void corner(Player player,boolean first) {
        corner(player,first,null);
    }
    private void corner(Player player,boolean first,Position specified) {
        var id=player.getUniqueId();var w=player.getWorld().getUID();var old=selections.get(id);
        if(old==null||!old.world().equals(w))old=new Selection(w,null,null,now.getAsLong());
        var p=player.getLocation();var position=specified==null?new Position(p.getBlockX(),p.getBlockY(),p.getBlockZ()):specified;
        var selection=new Selection(w,first?position:old.first(),first?old.second():position,now.getAsLong());
        if(selection.first()!=null&&selection.second()!=null)selectedBounds(selection.first(),selection.second(),settings);
        selections.put(id,selection);proposals.remove(id);releases.remove(id);
        player.sendMessage("Plot corner "+(first?1:2)+": "+position.x()+", "+position.y()+", "+position.z()+". Select both, then /civ building protect. Nothing saved yet.");
    }
    private Bounds bounds(Player player) {
        var s=selections.get(player.getUniqueId());
        if(s==null||s.first()==null||s.second()==null||!s.world().equals(player.getWorld().getUID()))throw new IllegalArgumentException("Stand at opposite plot corners and run /civ building corner1 and corner2 first");
        return selectedBounds(s.first(),s.second(),settings);
    }
    private boolean nearby(Player player,Bounds b) {
        var p=player.getLocation();return p.getX()>=b.min().x()-8.0&&p.getX()<=b.max().x()+9.0&&p.getZ()>=b.min().z()-8.0&&p.getZ()<=b.max().z()+9.0;
    }
    private void outline(Player player,Bounds b) {
        for(int n=0;n<=16;n++) {
            double x=b.min().x()+(b.max().x()+1.0-b.min().x())*n/16,z=b.min().z()+(b.max().z()+1.0-b.min().z())*n/16;
            for(double[] point:List.of(new double[]{x,b.min().z()},new double[]{x,b.max().z()+1.0},new double[]{b.min().x(),z},new double[]{b.max().x()+1.0,z}))
                player.spawnParticle(Particle.HAPPY_VILLAGER,point[0],b.min().y()+0.3,point[1],1,0,0,0,0);
        }
    }
    private void preview(Player player) {
        var b=bounds(player);if(!nearby(player,b))throw new IllegalArgumentException("Return beside your selected plot to preview");
        var request=new Exclusion(UUID.randomUUID(),player.getWorld().getUID(),player.getUniqueId(),b,now.getAsLong());
        proposals.put(player.getUniqueId(),new Proposal(request,now.getAsLong()+settings.previewSeconds()*1000L));releases.remove(player.getUniqueId());outline(player,b);
        player.sendMessage("Off-limits preview: X "+b.min().x()+".."+b.max().x()+", Z "+b.min().z()+".."+b.max().z()+"; all heights. No protection saved yet.");
        player.sendMessage("Confirming declares that this plot belongs to you. It excludes Civilizations construction only; no animals, buildings or contents are adopted.");
        player.sendMessage(Component.text("[Confirm: I own this plot]").clickEvent(ClickEvent.runCommand("/civ building confirm "+request.id()+" owned")));
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage("Use building protection in game.");return true;}
        if(args.length>1&&Set.of("home","confirmhome","cancelhome").contains(args[1].toLowerCase(Locale.ROOT))) {
            if(homes!=null)return homes.onCommand(sender,command,label,args);
            player.sendMessage("Home donation controls are unavailable.");return true;
        }
        if(!permitted(player)&&player.hasPermission(PaperHomeDonation.PERMISSION)&&(args.length==2||args.length==5)
                &&Set.of("corner1","corner2").contains(args[1].toLowerCase(Locale.ROOT))) {
            if(closed||!settings.enabled()||!owner.ready()||!room(player.getUniqueId())){player.sendMessage("Building selection is unavailable or busy.");return true;}
            try{corner(player,args[1].equalsIgnoreCase("corner1"),coordinates(args));}catch(IllegalArgumentException invalid){player.sendMessage("Building selection: "+invalid.getMessage());}
            return true;
        }
        if(!available(player))return true;var id=player.getUniqueId();String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"";
        if(action.equals("inspect")) {
            if(inspection!=null)return inspection.onCommand(sender,command,label,args);
            player.sendMessage("Building inspection is unavailable.");return true;
        }
        prune();
        if(action.equals("cancel")&&args.length==2) {
            if(pending.containsKey(id)){player.sendMessage("A submitted save is already finishing; wait for its result.");return true;}
            selections.remove(id);proposals.remove(id);releases.remove(id);player.sendMessage("Building selection and unsubmitted preview cancelled.");return true;
        }
        if(pending.containsKey(id)){player.sendMessage("Your building protection request is still finishing.");return true;}
        if(!room(id)){player.sendMessage("Building protection review busy; retry shortly.");return true;}
        try {
            if((action.equals("corner1")||action.equals("corner2"))&&(args.length==2||args.length==5)){corner(player,action.equals("corner1"),coordinates(args));return true;}
            if((action.equals("preview")||action.equals("protect"))&&args.length==2){preview(player);return true;}
            if(action.equals("confirm")&&args.length==4&&args[3].equalsIgnoreCase("owned")) {
                UUID token=UUID.fromString(args[2]);var proposed=proposals.get(id);
                if(proposed==null||!proposed.exclusion().id().equals(token))throw new IllegalArgumentException("No matching unexpired preview; preview again");
                var request=proposed.exclusion();
                if(!request.world().equals(player.getWorld().getUID())||!nearby(player,request.bounds()))throw new IllegalArgumentException("Return to the selected plot before confirming");
                submit(id,owner.protect(request,player.getWorld().getName()),value->{proposals.remove(id);selections.remove(id);say(id,"Building plot protected: "+((Exclusion)value).id()+". Removing the sign does not release this saved exclusion.");});return true;
            }
            if(action.equals("here")&&args.length==2) {
                var p=player.getLocation();submit(id,owner.at(player.getWorld().getUID(),p.getBlockX(),p.getBlockZ()),value->{
                    @SuppressWarnings("unchecked") var ids=(List<UUID>)value;
                    if(ids.isEmpty())say(id,"No saved active building exclusion here.");else for(var found:ids)say(id,"Building exclusion: "+found+". /civ building release "+found);
                });return true;
            }
            if(action.equals("release")&&args.length==3) {
                var target=UUID.fromString(args[2]);submit(id,owner.find(target),value->{
                    @SuppressWarnings("unchecked") var saved=(Optional<Exclusion>)value;var current=players.apply(id);
                    if(current==null||!permitted(current))return;
                    var e=saved.orElseThrow(()->new IllegalArgumentException("Unknown building exclusion"));
                    if(!e.world().equals(current.getWorld().getUID())||(!e.actor().equals(id)&&!current.hasPermission(ADMIN)))throw new IllegalArgumentException("Only its recorded owner or an administrator in that world can release");
                    var release=new Release(e,now.getAsLong()+settings.previewSeconds()*1000L);releases.put(id,release);proposals.remove(id);
                    say(id,"Release preview for "+e.id()+": removes only this exclusion; buildings, animal ownership and other protections remain.");
                    current.sendMessage(Component.text("[Confirm release]").clickEvent(ClickEvent.runCommand("/civ building confirmrelease "+release.operation)));
                });return true;
            }
            if(action.equals("confirmrelease")&&args.length==3) {
                var token=UUID.fromString(args[2]);var release=releases.get(id);
                if(release==null||!release.operation.equals(token))throw new IllegalArgumentException("No matching unexpired release preview");
                if(!release.exclusion.world().equals(player.getWorld().getUID())||(!release.exclusion.actor().equals(id)&&!player.hasPermission(ADMIN)))throw new IllegalArgumentException("Release authority/world changed; review again");
                if(release.submitted==null){release.submitted=now.getAsLong();release.submittedAdmin=player.hasPermission(ADMIN);}
                submit(id,owner.release(release.operation,release.exclusion,id,player.getWorld().getUID(),release.submittedAdmin,release.submitted),value->{releases.remove(id);say(id,"Building exclusion released. Other saved protection and registered building footprints remain.");});return true;
            }
        }catch(IllegalArgumentException|ArithmeticException invalid){player.sendMessage("Building protection: "+invalid.getMessage());return true;}
        player.sendMessage("/civ building corner1 | corner2 | protect | here | release <ID> | inspect <building ID> | cancel. Previews have clickable confirmation.");return true;
    }
    private static Position coordinates(String[] args){return args.length==5?new Position(Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[4])):null;}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void interact(PlayerInteractEvent event) {
        if(event.getHand()!=EquipmentSlot.HAND||event.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK||event.getClickedBlock()==null)return;
        if(!(event.getClickedBlock().getState() instanceof Sign sign)||!protectionSign(sign))return;
        event.setCancelled(true);var player=event.getPlayer();if(!available(player)||pending.containsKey(player.getUniqueId())||!room(player.getUniqueId()))return;
        try {
            var b=bounds(player);var at=new Position(sign.getX(),sign.getY(),sign.getZ());
            if(!b.contains(at)||!player.getWorld().getUID().equals(sign.getWorld().getUID())||player.getLocation().distanceSquared(sign.getLocation())>64)
                throw new IllegalArgumentException("Use a sign inside your selected plot and stand beside it");preview(player);
        }catch(IllegalArgumentException invalid){player.sendMessage("Building protection: "+invalid.getMessage());}
    }
    static boolean protectionSign(Sign sign) {
        var plain=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
        for(var side:Side.values())if(plain.serialize(sign.getSide(side).line(0)).trim().equalsIgnoreCase("[Civ Building]")
                &&plain.serialize(sign.getSide(side).line(1)).trim().equalsIgnoreCase("protect"))return true;
        return false;
    }
    @EventHandler public void quit(PlayerQuitEvent event){UUID id=event.getPlayer().getUniqueId();selections.remove(id);proposals.remove(id);releases.remove(id);}
    private void submit(UUID actor,CompletableFuture<?> future,Consumer<Object> done){pending.put(actor,new Pending(future,done));}
    @Override public void run() {
        if(closed)return;prune();
        for(var item:List.copyOf(pending.entrySet()))if(item.getValue().future().isDone()) {
            pending.remove(item.getKey());
            try{item.getValue().done().accept(item.getValue().future().join());}
            catch(CompletionException|CancellationException|IllegalArgumentException failure) {
                Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                if(!(cause instanceof Declined)&&!(cause instanceof IllegalArgumentException)&&!(cause instanceof CancellationException))logger.log(Level.WARNING,"Building protection save/review failed",cause);
                say(item.getKey(),"Building protection not confirmed: "+(cause instanceof Declined||cause instanceof IllegalArgumentException?cause.getMessage():"save failed; review the server log before retrying"));
            }
        }
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(args.length==3&&args[1].equalsIgnoreCase("home")&&homes instanceof TabCompleter completer)return completer.onTabComplete(sender,command,alias,args);
        if(args.length!=2)return List.of();var options=new ArrayList<String>();
        if(sender.hasPermission(MANAGE)||sender.hasPermission(ADMIN))options.addAll(List.of("corner1","corner2","preview","protect","here","release","inspect","cancel"));
        if(homes!=null&&sender.hasPermission(PaperHomeDonation.PERMISSION))options.addAll(List.of("corner1","corner2","home","cancelhome"));
        return options.stream().distinct().filter(s->s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
    }
    @Override public void close(){closed=true;selections.clear();proposals.clear();releases.clear();pending.clear();}
}
