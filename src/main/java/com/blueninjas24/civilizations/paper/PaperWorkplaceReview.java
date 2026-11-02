package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import com.blueninjas24.civilizations.industry.BusinessFounding;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Operator-only read-only workplace check; no donation, reservation, capacity or native changes. */
public final class PaperWorkplaceReview extends BukkitRunnable implements CommandExecutor,AutoCloseable {
    public static final int MAX_REQUESTS=4,CELLS_PER_TICK=128;
    public interface Owner{
        CompletableFuture<BusinessFounding> target(UUID business,UUID world,boolean permitted);
        CompletableFuture<WorkplaceInspection.Assessment> inspect(UUID business,BuildingLayout layout,WorkplaceInspection.Evidence evidence);
    }
    public interface Survey{Optional<WorkplaceInspection.Evidence> advance(int budget,long now);}
    @FunctionalInterface public interface Surveys{Survey create(BuildingLayout layout,Position marker,long now);}
    private static final class Request{
        final UUID actor,world,business;final BuildingLayout.Bounds bounds;final Position entrance;final long started;
        final CompletableFuture<BusinessFounding> target;BuildingLayout layout;Survey survey;CompletableFuture<WorkplaceInspection.Assessment> result;
        Request(UUID actor,UUID world,UUID business,BuildingLayout.Bounds bounds,Position entrance,long started,CompletableFuture<BusinessFounding> target){this.actor=actor;this.world=world;this.business=business;this.bounds=bounds;this.entrance=entrance;this.started=started;this.target=target;}
    }
    private final Owner owner;private final Function<Player,BuildingLayout.Bounds> selections;private final Function<UUID,Player> players;
    private final Surveys surveys;private final LongSupplier clock;private final Logger logger;private final Map<UUID,Request> requests=new LinkedHashMap<>();private boolean closed;
    public PaperWorkplaceReview(PopulationSession session,Function<Player,BuildingLayout.Bounds> selections,Logger logger){
        this(session,WorkplaceInspection.Policy.prototype(),selections,logger);
    }
    public PaperWorkplaceReview(PopulationSession session,WorkplaceInspection.Policy policy,Function<Player,BuildingLayout.Bounds> selections,Logger logger){
        this(new Owner(){
            public CompletableFuture<BusinessFounding> target(UUID business,UUID world,boolean permitted){return session.workplaceInspectionTarget(business,world,permitted);}
            public CompletableFuture<WorkplaceInspection.Assessment> inspect(UUID business,BuildingLayout layout,WorkplaceInspection.Evidence evidence){return session.inspectBusinessPremises(business,layout,evidence,policy);}
        },selections,Bukkit::getPlayer,(layout,marker,now)->new PaperWorkplaceSurvey(layout,marker,now)::advance,System::currentTimeMillis,logger);
    }
    public PaperWorkplaceReview(Owner owner,Function<Player,BuildingLayout.Bounds> selections,Function<UUID,Player> players,Surveys surveys,LongSupplier clock,Logger logger){this.owner=Objects.requireNonNull(owner);this.selections=Objects.requireNonNull(selections);this.players=Objects.requireNonNull(players);this.surveys=Objects.requireNonNull(surveys);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Check a selected workplace in game.");return true;}
        if(closed||!player.hasPermission(PaperBuildingProtection.ADMIN)){player.sendMessage("Workplace checks require building administrator permission.");return true;}
        try{
            if(args.length!=3)throw new IllegalArgumentException("/civ building workplacecheck <business UUID>");
            UUID business=UUID.fromString(args[2]),actor=player.getUniqueId(),world=player.getWorld().getUID();
            if(requests.containsKey(actor))throw new IllegalArgumentException("Your workplace check is already pending");
            if(requests.size()>=MAX_REQUESTS)throw new IllegalArgumentException("Workplace checks busy; retry shortly");
            var bounds=selections.apply(player);if(bounds.volume()>BuildingLayout.MAX_STRUCTURE_VOLUME)throw new IllegalArgumentException("Selected workplace exceeds scan volume");
            var here=player.getLocation();var entrance=new Position(here.getBlockX(),here.getBlockY(),here.getBlockZ());
            if(!bounds.contains(entrance))throw new IllegalArgumentException("Stand at an entrance inside the selected floor-to-roof bounds");
            requests.put(actor,new Request(actor,world,business,bounds,entrance,clock.getAsLong(),owner.target(business,world,true)));
            player.sendMessage("Checking selected workplace access and shelter. This grants no use, ownership, contents access or construction permission.");
        }catch(IllegalArgumentException invalid){player.sendMessage("Workplace check: "+invalid.getMessage());}
        return true;
    }
    @Override public void run(){
        if(closed)return;long now=clock.getAsLong();boolean advanced=false;
        for(var request:List.copyOf(requests.values())){
            var player=players.apply(request.actor);
            try{
                if(player==null){requests.remove(request.actor);continue;}
                if(!player.hasPermission(PaperBuildingProtection.ADMIN)||!player.getWorld().getUID().equals(request.world)||!selections.apply(player).equals(request.bounds))throw new IllegalArgumentException("Authority, world or selection changed; check again");
                if(now<request.started||now-request.started>=30_000)throw new IllegalArgumentException("Workplace check expired; no saved state changed");
                if(request.survey==null){
                    if(!request.target.isDone())continue;var founding=request.target.join();var site=founding.site();var marker=new Position(site.x(),site.y(),site.z());
                    if(!site.worldId().equals(request.world)||!request.bounds.contains(marker))throw new IllegalArgumentException("The saved fixed workplace is outside this selection/world");
                    request.layout=new BuildingLayout(UUID.randomUUID(),site.settlementId(),request.world,BuildingLayout.Kind.WORKPLACE,request.bounds,request.bounds,request.entrance,List.of(),List.of());
                    request.survey=surveys.create(request.layout,marker,now);
                }
                if(request.result==null){
                    if(advanced)continue;advanced=true;var evidence=request.survey.advance(CELLS_PER_TICK,now);if(evidence.isEmpty())continue;
                    request.result=owner.inspect(request.business,request.layout,evidence.orElseThrow());
                }
                if(!request.result.isDone())continue;var assessment=request.result.join();requests.remove(request.actor);
                player.sendMessage(assessment.suitable()?"Workplace access check passed; reachable work position "+assessment.workPosition().orElseThrow()+".":"Workplace access check refused: "+assessment.issues());
                player.sendMessage("Read-only result. No building registered, donated, reserved or renovated; no business capacity changed.");
            }catch(CompletionException|CancellationException|IllegalArgumentException failure){
                requests.remove(request.actor);var cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof IllegalArgumentException||cause instanceof CancellationException;
                if(!expected)logger.log(Level.WARNING,"Workplace diagnostic failed",cause);
                if(player!=null)player.sendMessage("Workplace check unavailable: "+(expected?cause.getMessage():"review the server log; saved state unchanged"));
            }
        }
    }
    @Override public void close(){closed=true;requests.clear();}
}
