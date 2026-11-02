package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.industry.BusinessFounding;
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

/** Explicit existing-business donation with a fresh confirmation survey; never title, contents or renovation. */
public final class PaperWorkplaceDonation extends BukkitRunnable implements CommandExecutor,Listener,AutoCloseable {
    public static final int MAX_REQUESTS=4,CELLS_PER_TICK=128;
    public interface Owner{
        CompletableFuture<BusinessFounding> target(UUID business,UUID world);
        CompletableFuture<WorkplaceDonationRequests.Preview> preview(UUID actor,UUID world,UUID business,BuildingLayout layout,WorkplaceInspection.Evidence evidence);
        CompletableFuture<WorkplaceDonation.Receipt> confirm(UUID actor,UUID world,UUID token,WorkplaceInspection.Evidence evidence);
        void cancel(UUID actor);
    }
    private static final class Request{
        final UUID actor,world,business,building;final BuildingLayout.Bounds bounds;final Position entrance;final long started;final CompletableFuture<BusinessFounding> lookup;
        Position marker;BuildingLayout layout;PaperWorkplaceReview.Survey survey;CompletableFuture<WorkplaceDonationRequests.Preview> reviewing;
        WorkplaceDonationRequests.Preview preview;boolean confirming;CompletableFuture<WorkplaceDonation.Receipt> saving;
        Request(UUID actor,UUID world,UUID business,UUID building,BuildingLayout.Bounds bounds,Position entrance,long started,CompletableFuture<BusinessFounding> lookup){this.actor=actor;this.world=world;this.business=business;this.building=building;this.bounds=bounds;this.entrance=entrance;this.started=started;this.lookup=lookup;}
    }
    private final Owner owner;private final Function<Player,BuildingLayout.Bounds> selections;private final Function<UUID,Player> players;
    private final PaperWorkplaceReview.Surveys surveys;private final LongSupplier clock;private final Logger logger;
    private final Map<UUID,Request> requests=new LinkedHashMap<>();private boolean closed;
    public PaperWorkplaceDonation(PopulationSession session,Function<Player,BuildingLayout.Bounds> selections,Logger logger){
        this(session,WorkplaceInspection.Policy.prototype(),selections,logger);
    }
    public PaperWorkplaceDonation(PopulationSession session,WorkplaceInspection.Policy policy,Function<Player,BuildingLayout.Bounds> selections,Logger logger){
        this(new Owner(){
            public CompletableFuture<BusinessFounding> target(UUID business,UUID world){return session.workplaceInspectionTarget(business,world,true);}
            public CompletableFuture<WorkplaceDonationRequests.Preview> preview(UUID actor,UUID world,UUID business,BuildingLayout layout,WorkplaceInspection.Evidence evidence){return session.previewWorkplaceDonation(actor,world,business,layout,evidence,policy,true);}
            public CompletableFuture<WorkplaceDonation.Receipt> confirm(UUID actor,UUID world,UUID token,WorkplaceInspection.Evidence evidence){return session.confirmWorkplaceDonation(actor,world,token,evidence,true,true);}
            public void cancel(UUID actor){session.cancelWorkplaceDonation(actor);}
        },selections,Bukkit::getPlayer,(layout,marker,now)->new PaperWorkplaceSurvey(layout,marker,now)::advance,System::currentTimeMillis,logger);
    }
    public PaperWorkplaceDonation(Owner owner,Function<Player,BuildingLayout.Bounds> selections,Function<UUID,Player> players,PaperWorkplaceReview.Surveys surveys,LongSupplier clock,Logger logger){this.owner=Objects.requireNonNull(owner);this.selections=Objects.requireNonNull(selections);this.players=Objects.requireNonNull(players);this.surveys=Objects.requireNonNull(surveys);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Select and donate an existing workplace in game.");return true;}
        if(closed||!player.hasPermission(PaperHomeDonation.PERMISSION)){player.sendMessage("Workplace donation is unavailable or not permitted.");return true;}
        UUID actor=player.getUniqueId();var request=requests.get(actor);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"";
        try{
            if(action.equals("cancelworkplace")&&args.length==2){if(request!=null&&request.saving!=null)throw new IllegalArgumentException("Submitted donation is finishing; wait for its result");discard(actor);player.sendMessage("Unsubmitted workplace donation cancelled.");return true;}
            if(action.equals("workplace")&&(args.length==3||args.length==4)){
                if(request!=null)throw new IllegalArgumentException("Finish your workplace review or cancelworkplace first");
                if(requests.size()>=MAX_REQUESTS)throw new IllegalArgumentException("Workplace donation reviews busy; retry shortly");
                UUID business=UUID.fromString(args[2]),building=args.length==4?UUID.fromString(args[3]):UUID.randomUUID();var bounds=selections.apply(player);if(bounds.volume()>BuildingLayout.MAX_STRUCTURE_VOLUME)throw new IllegalArgumentException("Workplace exceeds scan volume");
                var here=player.getLocation();var entrance=new Position(here.getBlockX(),here.getBlockY(),here.getBlockZ());if(!bounds.contains(entrance))throw new IllegalArgumentException("Stand at an entrance inside your floor-to-roof selection");
                UUID world=player.getWorld().getUID();requests.put(actor,new Request(actor,world,business,building,bounds,entrance,clock.getAsLong(),owner.target(business,world)));
                player.sendMessage("Reviewing your selected workplace for the existing business. Nothing donated yet.");return true;
            }
            if(action.equals("confirmworkplace")&&args.length==4&&args[3].equalsIgnoreCase("owned")){
                UUID token=UUID.fromString(args[2]);
                if(request==null||request.preview==null||!request.preview.token().equals(token))throw new IllegalArgumentException("No matching reviewed workplace");
                if(request.confirming||request.saving!=null)throw new IllegalArgumentException("Your submitted confirmation is finishing");
                if(!current(player,request)||clock.getAsLong()>=request.preview.expiresMillis())throw new IllegalArgumentException("Selection, world, permission or preview changed; review again");
                request.confirming=true;request.survey=surveys.create(request.layout,request.marker,clock.getAsLong());
                player.sendMessage("Rechecking the frozen workplace before saving your ownership declaration and village-use permission. Wait for confirmation.");return true;
            }
            player.sendMessage("/civ building workplace <business UUID> [reclaimed building UUID] | confirmworkplace <token> owned | cancelworkplace");
        }catch(IllegalArgumentException invalid){player.sendMessage("Workplace donation: "+invalid.getMessage());}
        return true;
    }
    private boolean current(Player player,Request request){return player.hasPermission(PaperHomeDonation.PERMISSION)&&player.getWorld().getUID().equals(request.world)&&selections.apply(player).equals(request.bounds);}
    private void discard(UUID actor){if(requests.remove(actor)!=null)owner.cancel(actor);}
    @Override public void run(){
        if(closed)return;long now=clock.getAsLong();boolean advanced=false;
        for(var request:List.copyOf(requests.values())){
            var player=players.apply(request.actor);
            try{
                if(request.saving!=null){
                    if(!request.saving.isDone())continue;var saved=request.saving.join();requests.remove(request.actor);owner.cancel(request.actor);
                    if(player!=null)player.sendMessage("Workplace donation receipt confirmed: "+saved.consent().building()+". Ownership and contents remain yours; renovations need separate consent.");continue;
                }
                if(player==null||!current(player,request)){discard(request.actor);continue;}
                long deadline=request.preview==null?request.started+30_000:request.preview.expiresMillis();
                if(now<request.started||now>=deadline){player.sendMessage("Workplace donation preview expired; no new confirmation submitted.");discard(request.actor);continue;}
                if(request.layout==null){
                    if(!request.lookup.isDone())continue;var site=request.lookup.join().site();request.marker=new Position(site.x(),site.y(),site.z());
                    if(!site.worldId().equals(request.world)||!request.bounds.contains(request.marker))throw new IllegalArgumentException("Fixed business marker lies outside the selected workplace");
                    request.layout=new BuildingLayout(request.building,site.settlementId(),request.world,BuildingLayout.Kind.WORKPLACE,request.bounds,request.bounds,request.entrance,List.of(),List.of());
                    request.survey=surveys.create(request.layout,request.marker,now);
                }
                if(request.preview!=null&&!request.confirming)continue;
                if(request.reviewing==null||request.confirming){
                    if(advanced)continue;advanced=true;var evidence=request.survey.advance(CELLS_PER_TICK,now);if(evidence.isEmpty())continue;
                    if(request.confirming){request.saving=owner.confirm(request.actor,request.world,request.preview.token(),evidence.orElseThrow());continue;}
                    request.reviewing=owner.preview(request.actor,request.world,request.business,request.layout,evidence.orElseThrow());
                }
                if(!request.reviewing.isDone())continue;request.preview=request.reviewing.join();
                player.sendMessage("Workplace donation preview: existing business "+request.business+"; bounds "+request.bounds+". Confirm only if this building belongs to you. No title, goods or renovation rights transfer.");
                player.sendMessage(Component.text("[Confirm: I own and donate this workplace]").clickEvent(ClickEvent.runCommand("/civ building confirmworkplace "+request.preview.token()+" owned")));
            }catch(CompletionException|CancellationException|IllegalArgumentException failure){
                var cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof IllegalArgumentException||cause instanceof CancellationException;
                if(!expected)logger.log(Level.WARNING,"Workplace donation review/save failed",cause);
                if(player!=null)player.sendMessage("Workplace donation not confirmed: "+(expected?cause.getMessage():"review the server log before retrying"));
                if(request.saving!=null){request.saving=null;request.confirming=false;}else discard(request.actor);
            }
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event){var request=requests.get(event.getPlayer().getUniqueId());if(request!=null&&request.saving==null)discard(request.actor);}
    @Override public void close(){closed=true;for(var request:List.copyOf(requests.values()))if(request.saving==null)owner.cancel(request.actor);requests.clear();}
}
