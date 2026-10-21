package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Read-only owner-queued housing decisions, delivered on the server thread without scanning or loading chunks. */
public final class PaperHousingCandidates extends BukkitRunnable implements CommandExecutor,AutoCloseable {
    public interface Owner {CompletableFuture<List<String>> review(UUID village,UUID world);}
    private record Request(UUID player,UUID world,long started,CompletableFuture<List<String>> result){}
    private final Owner owner;private final Function<UUID,Player> players;private final LongSupplier clock;private final Logger logger;
    private final Map<UUID,Request> waiting=new LinkedHashMap<>();private boolean closed;
    public PaperHousingCandidates(PopulationSession session,List<ConstructionTemplate> templates,HousingProjectPlanner planner,DwellingInspection.Policy policy,Logger logger){
        this(source(session,templates,planner,policy),Bukkit::getPlayer,System::currentTimeMillis,logger);
    }
    private static Owner source(PopulationSession session,List<ConstructionTemplate> templates,HousingProjectPlanner planner,DwellingInspection.Policy policy){
        var frozen=List.copyOf(templates);return(v,w)->session.housingCandidates(v,w,frozen,planner,policy).thenApply(r->r.describe());
    }
    public PaperHousingCandidates(Owner owner,Function<UUID,Player> players,LongSupplier clock,Logger logger){
        this.owner=Objects.requireNonNull(owner);this.players=Objects.requireNonNull(players);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!sender.hasPermission(PaperConstructionPreview.PERMISSION)){sender.sendMessage("You do not have permission to inspect housing plans.");return true;}
        if(!(sender instanceof Player player)){sender.sendMessage("Housing plans require a player in the village's world.");return true;}
        if(args.length!=3||!args[0].equalsIgnoreCase("build")||!args[1].equalsIgnoreCase("housing")){sender.sendMessage("/civ build housing <village UUID>");return true;}
        if(closed){sender.sendMessage("Housing planning is stopping.");return true;}
        UUID village;try{village=UUID.fromString(args[2]);}catch(IllegalArgumentException invalid){sender.sendMessage("Settlement ID must be a UUID.");return true;}
        if(waiting.containsKey(player.getUniqueId())){sender.sendMessage("Your housing report is still being checked.");return true;}
        if(waiting.size()>=4){sender.sendMessage("Housing reports are busy; try again shortly.");return true;}
        UUID world=player.getWorld().getUID();
        waiting.put(player.getUniqueId(),new Request(player.getUniqueId(),world,clock.getAsLong(),owner.review(village,world)));
        sender.sendMessage("Checking saved housing needs and permitted vacancies; no project will be started.");return true;
    }
    @Override public void run(){
        if(closed)return;long time=clock.getAsLong();
        for(var iterator=waiting.values().iterator();iterator.hasNext();){
            var request=iterator.next();var player=players.apply(request.player());
            if(player==null){iterator.remove();continue;}
            if(!player.hasPermission(PaperConstructionPreview.PERMISSION)||!player.getWorld().getUID().equals(request.world())){player.sendMessage("Housing report cancelled: world or permission changed.");iterator.remove();continue;}
            if(time-request.started()>30_000){player.sendMessage("Housing report expired; try again.");iterator.remove();continue;}
            if(!request.result().isDone())continue;
            try{var lines=request.result().join();lines.stream().limit(20).forEach(player::sendMessage);if(lines.size()>20)player.sendMessage("Showing the first20 report lines.");}
            catch(CompletionException|CancellationException failure){
                Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                if(cause instanceof BuildingRegistry.Declined)player.sendMessage("Housing report unavailable: "+cause.getMessage());
                else{player.sendMessage("Housing report unavailable; retry or check the server log.");if(!(cause instanceof CancellationException))logger.log(Level.WARNING,"Saved housing report failed",cause);}
            }
            iterator.remove();
        }
    }
    @Override public void close(){closed=true;waiting.clear();}
}
