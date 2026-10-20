package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.BuildingLayout;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Explicit one-home permission donation. Does not activate capacity, move residents or transfer title. */
public final class PaperHomeDonation extends BukkitRunnable implements CommandExecutor,TabCompleter,Listener,AutoCloseable {
    public static final String PERMISSION="civilizations.building.donate";
    public static final int MAX_REQUESTS=4,CELLS_PER_TICK=128;
    public interface Owner {
        CompletableFuture<PopulationSession.HomeDonationPreview> preview(UUID actor,UUID world,UUID building,UUID village,
                BuildingLayout.Bounds bounds,Position entrance,DwellingInspection.Observation observation);
        CompletableFuture<BuildingRegistry.Consent> confirm(UUID actor,UUID world,UUID token);
        void cancel(UUID actor);
        default List<String> villages(){return List.of();}
    }
    @FunctionalInterface public interface Surveys {PaperDwellingReview.Survey create(UUID world,BuildingLayout.Bounds bounds,long now);}
    private static final class Request {
        final UUID actor,world,village,building=UUID.randomUUID();final BuildingLayout.Bounds bounds;final Position entrance;final long started;
        final PaperDwellingReview.Survey survey;
        CompletableFuture<PopulationSession.HomeDonationPreview> reviewing;PopulationSession.HomeDonationPreview preview;
        CompletableFuture<BuildingRegistry.Consent> saving;
        Request(UUID actor,UUID world,UUID village,BuildingLayout.Bounds bounds,Position entrance,long started,PaperDwellingReview.Survey survey){
            this.actor=actor;this.world=world;this.village=village;this.bounds=bounds;this.entrance=entrance;this.started=started;this.survey=survey;}
    }
    private final Owner owner;private final Function<Player,BuildingLayout.Bounds> selections;private final Function<UUID,Player> players;
    private final Surveys surveys;private final LongSupplier clock;private final Logger logger;
    private final LinkedHashMap<UUID,Request> requests=new LinkedHashMap<>();private boolean closed;
    public PaperHomeDonation(PopulationSession session,DwellingInspection.Policy policy,Function<Player,BuildingLayout.Bounds> selections,Logger logger) {
        this(new Owner(){
            public CompletableFuture<PopulationSession.HomeDonationPreview> preview(UUID actor,UUID world,UUID building,UUID village,BuildingLayout.Bounds bounds,Position entry,DwellingInspection.Observation observation){return session.previewSingleHomeDonation(actor,world,building,village,bounds,bounds,entry,observation,policy,true);}
            public CompletableFuture<BuildingRegistry.Consent> confirm(UUID actor,UUID world,UUID token){return session.confirmBuildingConsent(actor,world,token,true,true,false,false);}
            public void cancel(UUID actor){session.cancelBuildingConsent(actor);}
            public List<String> villages(){return session.settlementNames().keySet().stream().map(UUID::toString).sorted().limit(32).toList();}
        },selections,Bukkit::getPlayer,(world,bounds,now)->PaperDwellingSurvey.forHomeDeclaration(world,bounds,now)::advance,System::currentTimeMillis,logger);
    }
    public PaperHomeDonation(Owner owner,Function<Player,BuildingLayout.Bounds> selections,Function<UUID,Player> players,Surveys surveys,LongSupplier clock,Logger logger){
        this.owner=Objects.requireNonNull(owner);this.selections=Objects.requireNonNull(selections);this.players=Objects.requireNonNull(players);
        this.surveys=Objects.requireNonNull(surveys);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage("Select and donate a home in game.");return true;}
        if(closed||!player.hasPermission(PERMISSION)){player.sendMessage("Home donation is unavailable or not permitted.");return true;}
        UUID actor=player.getUniqueId();var request=requests.get(actor);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"";
        try {
            if(action.equals("cancelhome")&&args.length==2){
                if(request!=null&&request.saving!=null){player.sendMessage("The submitted donation is already finishing; wait for its result.");return true;}
                discard(actor);player.sendMessage("Unsubmitted home donation cancelled.");return true;
            }
            if(action.equals("home")&&args.length==3) {
                if(request!=null){player.sendMessage("Your home review is still open; finish it or /civ building cancelhome.");return true;}
                if(requests.size()>=MAX_REQUESTS){player.sendMessage("Home reviews busy; retry shortly.");return true;}
                UUID village=UUID.fromString(args[2]);var bounds=selections.apply(player);
                if(bounds.volume()>BuildingLayout.MAX_STRUCTURE_VOLUME)throw new IllegalArgumentException("Home selection exceeds scan volume limit");
                var here=player.getLocation();var entrance=new Position(here.getBlockX(),here.getBlockY(),here.getBlockZ());
                if(!bounds.contains(entrance))throw new IllegalArgumentException("Stand at the home's entrance inside your selected floor-to-roof bounds");
                long now=clock.getAsLong();UUID world=player.getWorld().getUID();
                requests.put(actor,new Request(actor,world,village,bounds,entrance,now,surveys.create(world,bounds,now)));
                player.sendMessage("Reviewing one family home, including all selected beds. Include floor and roof in both corners. Nothing donated yet.");return true;
            }
            if(action.equals("confirmhome")&&args.length==4&&args[3].equalsIgnoreCase("owned")) {
                UUID token=UUID.fromString(args[2]);
                if(request==null||request.preview==null||!request.preview.consent().token().equals(token))throw new IllegalArgumentException("No matching reviewed home; start a new home review");
                if(request.saving!=null){player.sendMessage("Your submitted home donation is finishing.");return true;}
                if(!current(player,request)||clock.getAsLong()>=request.preview.consent().expiresMillis())throw new IllegalArgumentException("Home selection, world or preview changed; cancel and review again");
                request.saving=owner.confirm(actor,request.world,token);player.sendMessage("Saving your explicit ownership declaration and village-use permission. Wait for confirmation.");return true;
            }
        }catch(IllegalArgumentException invalid){player.sendMessage("Home donation: "+invalid.getMessage());return true;}
        player.sendMessage("/civ building home <village UUID> | cancelhome. Select floor-to-roof corners first; stand at the entrance. Apartments are not inferred.");return true;
    }
    private boolean current(Player player,Request r) {
        return player.hasPermission(PERMISSION)&&player.getWorld().getUID().equals(r.world)&&r.bounds.equals(selections.apply(player));
    }
    private void discard(UUID actor){if(requests.remove(actor)!=null)owner.cancel(actor);}
    @Override public void run() {
        if(closed)return;long now=clock.getAsLong();boolean advanced=false;
        for(var r:List.copyOf(requests.values())) {
            var player=players.apply(r.actor);
            try {
                // An accepted SQL save drains even after disconnect or permission loss.
                if(r.saving!=null) {
                    if(!r.saving.isDone())continue;
                    var receipt=r.saving.join();requests.remove(r.actor);
                    if(player!=null)player.sendMessage("Home donation permission saved: "+receipt.building()+". Ownership is retained; household assignment is still under development. No residents moved.");
                    continue;
                }
                if(player==null||!current(player,r)){discard(r.actor);continue;}
                long deadline=r.preview==null?r.started+30_000:r.preview.consent().expiresMillis();
                if(now<r.started||now>=deadline){player.sendMessage("Home review expired; no new donation confirmed.");discard(r.actor);continue;}
                if(r.preview!=null)continue;
                if(r.reviewing==null) {
                    if(advanced)continue;advanced=true;
                    var frame=r.survey.advance(CELLS_PER_TICK,now);if(frame.isEmpty())continue;
                    r.reviewing=owner.preview(r.actor,r.world,r.building,r.village,r.bounds,r.entrance,frame.orElseThrow());
                }
                if(!r.reviewing.isDone())continue;
                r.preview=r.reviewing.join();
                player.sendMessage("Home donation preview: "+r.preview.review().assessment().accessibleBeds()+" reviewed beds in ONE family home, X "+r.bounds.min().x()+".."+r.bounds.max().x()+", Y "+r.bounds.min().y()+".."+r.bounds.max().y()+", Z "+r.bounds.min().z()+".."+r.bounds.max().z()+". Village: "+r.village);
                player.sendMessage("Confirm only if you own this building. This saves village-use permission, not title, inventory or occupied housing capacity.");
                player.sendMessage(Component.text("[Confirm: I own and donate this home]").clickEvent(ClickEvent.runCommand("/civ building confirmhome "+r.preview.consent().token()+" owned")));
            }catch(CompletionException|CancellationException|IllegalArgumentException failure) {
                Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof IllegalArgumentException||cause instanceof CancellationException;
                if(!expected)logger.log(Level.WARNING,"Home donation review/save failed",cause);
                if(player!=null)player.sendMessage("Home donation not confirmed: "+(expected?cause.getMessage():"save failed; inspect the server log before retrying"));
                if(r.saving!=null)r.saving=null; // Frozen owner preview permits an explicit exact retry after a save failure.
                else discard(r.actor);
            }
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event){var r=requests.get(event.getPlayer().getUniqueId());if(r!=null&&r.saving==null)discard(r.actor);}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String label,String[] args) {
        return !closed&&sender.hasPermission(PERMISSION)&&args.length==3&&args[1].equalsIgnoreCase("home")?
                owner.villages().stream().limit(32).filter(s->s.startsWith(args[2].toLowerCase(Locale.ROOT))).toList():List.of();
    }
    @Override public void close(){closed=true;for(var r:List.copyOf(requests.values()))if(r.saving==null)owner.cancel(r.actor);requests.clear();}
}
