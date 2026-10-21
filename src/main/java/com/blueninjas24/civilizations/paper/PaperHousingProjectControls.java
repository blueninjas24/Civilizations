package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.DwellingInspection;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy;
import com.blueninjas24.civilizations.construction.ConstructionSupplyPolicy;
import com.blueninjas24.civilizations.database.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Explicit operator recovery for unused home proposals only. No paid-asset refund, demolition or native scan. */
public final class PaperHousingProjectControls extends BukkitRunnable implements CommandExecutor,AutoCloseable {
    public static final String MANAGE="civilizations.construction.manage";
    public interface Owner {
        CompletableFuture<HousingProjectStatus.Report> status(UUID village,UUID world);
        CompletableFuture<HousingProjectStatus.Item> inspect(UUID draft,UUID world);
        CompletableFuture<HousingProjects.Binding> retire(UUID operation,UUID draft,UUID world,long submitted);
        default CompletableFuture<ConstructionSupplyOrders.VillageFunding> funding(UUID draft,UUID world){return CompletableFuture.failedFuture(new BuildingRegistry.Declined("Construction funding report unavailable"));}
    }
    private enum Mode { STATUS,FUNDING,PREVIEW,SAVING }
    private record Pending(UUID player,UUID world,long started,Mode mode,CompletableFuture<?> work){}
    private static final class Preview {
        final UUID token=UUID.randomUUID(),world,draft;final long expires;Long submitted;
        Preview(UUID world,UUID draft,long expires){this.world=world;this.draft=draft;this.expires=expires;}
    }
    private final Owner owner;private final Function<UUID,Player> players;private final LongSupplier clock;private final Logger logger;
    private final Map<UUID,Pending> waiting=new LinkedHashMap<>();private final Map<UUID,Preview> previews=new HashMap<>();private boolean closed;
    public PaperHousingProjectControls(PopulationSession session,DwellingInspection.Policy policy,Logger logger){
        this(session,policy,ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),logger);
    }
    public PaperHousingProjectControls(PopulationSession session,DwellingInspection.Policy policy,ConstructionBudgetPolicy budget,ConstructionSupplyPolicy supply,Logger logger){
        this(new Owner(){
            public CompletableFuture<HousingProjectStatus.Report> status(UUID v,UUID w){return session.housingProjectStatus(v,w,policy);}
            public CompletableFuture<HousingProjectStatus.Item> inspect(UUID d,UUID w){return session.housingProjectInspection(d,w,policy);}
            public CompletableFuture<HousingProjects.Binding> retire(UUID o,UUID d,UUID w,long at){return session.retireUnusedHousingProject(o,d,w,at);}
            public CompletableFuture<ConstructionSupplyOrders.VillageFunding> funding(UUID d,UUID w){return session.constructionVillageFunding(d,w,budget,supply);}
        },Bukkit::getPlayer,System::currentTimeMillis,logger);
    }
    public PaperHousingProjectControls(Owner owner,Function<UUID,Player> players,LongSupplier clock,Logger logger){
        this.owner=Objects.requireNonNull(owner);this.players=Objects.requireNonNull(players);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Use project controls as a player in the project's world.");return true;}
        String action=args.length>=2?args[1].toLowerCase(Locale.ROOT):"";
        boolean status=action.equals("projects")||action.equals("funding");String permission=status?PaperConstructionPreview.PERMISSION:MANAGE;
        if(!(action.equals("cancelrequest")?(player.hasPermission(MANAGE)||player.hasPermission(PaperConstructionPreview.PERMISSION)):player.hasPermission(permission))){sender.sendMessage("You do not have permission to "+(status?"inspect":"cancel")+" home projects.");return true;}
        if(closed){sender.sendMessage("Project controls are stopping.");return true;}
        UUID actor=player.getUniqueId();long now=clock.getAsLong();var active=waiting.get(actor);
        if(action.equals("cancelrequest")&&args.length==2){
            if(active!=null&&active.mode()==Mode.SAVING){sender.sendMessage("Cancellation was already submitted; its saved result will follow.");return true;}
            waiting.remove(actor);previews.remove(actor);sender.sendMessage("Unsubmitted project request discarded.");return true;
        }
        if(active!=null){sender.sendMessage("Your project request is still being checked or saved.");return true;}
        if(args.length!=3||!Set.of("projects","funding","cancelproject","confirmcancel").contains(action)){usage(sender);return true;}
        UUID id;try{id=UUID.fromString(args[2]);}catch(IllegalArgumentException invalid){sender.sendMessage("Project, village or confirmation ID must be a UUID.");return true;}
        previews.entrySet().removeIf(e->now>e.getValue().expires&&!waiting.containsKey(e.getKey()));
        UUID world=player.getWorld().getUID();
        if(action.equals("confirmcancel")){
            var preview=previews.get(actor);
            if(preview==null||!preview.token.equals(id)||!preview.world.equals(world)||now>preview.expires){sender.sendMessage("Cancellation preview expired or changed; review the project again.");return true;}
            if(waiting.size()>=4){sender.sendMessage("Project controls are busy; retry shortly.");return true;}
            if(preview.submitted==null)preview.submitted=now;
            waiting.put(actor,new Pending(actor,world,now,Mode.SAVING,owner.retire(preview.token,preview.draft,world,preview.submitted)));
            sender.sendMessage("Saving unused-project cancellation; funds, goods, people and blocks will not be deleted.");return true;
        }
        var users=new HashSet<>(waiting.keySet());users.addAll(previews.keySet());
        if(users.size()-(users.contains(actor)?1:0)>=4){sender.sendMessage("Project controls are busy; retry shortly.");return true;}
        previews.remove(actor);
        var future=action.equals("funding")?owner.funding(id,world):status?owner.status(id,world):owner.inspect(id,world);
        waiting.put(actor,new Pending(actor,world,now,action.equals("funding")?Mode.FUNDING:status?Mode.STATUS:Mode.PREVIEW,future));
        sender.sendMessage(action.equals("funding")?"Reading current project materials and protected funds; nothing will be purchased.":status?"Reading retained home projects; no changes will be made.":"Checking whether this home project is unused; nothing cancelled yet.");return true;
    }
    @Override public void run(){
        if(closed)return;long now=clock.getAsLong();
        for(var iterator=waiting.values().iterator();iterator.hasNext();){
            var pending=iterator.next();var player=players.apply(pending.player());boolean saving=pending.mode()==Mode.SAVING;
            if(!saving&&(player==null||!player.getWorld().getUID().equals(pending.world())||!player.hasPermission(pending.mode()==Mode.STATUS||pending.mode()==Mode.FUNDING?PaperConstructionPreview.PERMISSION:MANAGE))){iterator.remove();continue;}
            if(!saving&&now-pending.started()>30_000){player.sendMessage("Project review expired; try again.");iterator.remove();continue;}
            if(!pending.work().isDone())continue;
            try{
                var result=pending.work().join();
                if(saving){
                    previews.remove(pending.player());if(player!=null)player.sendMessage("Unused home project cancelled: "+((HousingProjects.Binding)result).proposal().draft()+". Site/family reservations released; history retained.");
                }else if(pending.mode()==Mode.STATUS){((HousingProjectStatus.Report)result).describe().forEach(player::sendMessage);}
                else if(pending.mode()==Mode.FUNDING){funding(player,(ConstructionSupplyOrders.VillageFunding)result);}
                else{
                    var item=(HousingProjectStatus.Item)result;
                    if(!item.unused()){player.sendMessage("Project cannot use unused cancellation: cancelled, unavailable world, paid supplies, held materials or physical work require review. Nothing changed.");}
                    else{
                        var preview=new Preview(pending.world(),item.family().proposal().draft(),now+60_000);previews.put(pending.player(),preview);
                        player.sendMessage("Unused cancellation preview: "+preview.draft+"; family "+item.family().proposal().household()+". Releases only its site and family reservation; history remains. Confirm within 60 seconds.");
                        player.sendMessage(Component.text("Click to cancel this unused home project").clickEvent(ClickEvent.runCommand("/civ build confirmcancel "+preview.token)));
                    }
                }
            }catch(CompletionException|CancellationException failure){
                Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof ConstructionSupplyOrders.Declined;
                if(player!=null)player.sendMessage("Project request could not finish; saved assets/history retained. "+(expected?cause.getMessage():"Review again or check the server log."));
                if(!(expected||cause instanceof CancellationException))logger.log(Level.WARNING,"Home project request failed; saved history retained",cause);
            }
            iterator.remove();
        }
        previews.entrySet().removeIf(e->{if(waiting.containsKey(e.getKey()))return false;var player=players.apply(e.getKey());
            return now>e.getValue().expires||player==null||!player.hasPermission(MANAGE)||!player.getWorld().getUID().equals(e.getValue().world);});
    }
    private static void funding(Player player,ConstructionSupplyOrders.VillageFunding q){
        player.sendMessage("Construction funding snapshot: "+q.draft()+"; current food period "+q.period()+".");
        player.sendMessage("Emerald value: village "+value(q.cash())+"; protected "+value(q.protectedCash())+"; available "+value(q.availableCash())+"; spent this period "+value(q.periodSpent())+".");
        player.sendMessage("Remaining reference cost: "+(q.remainingReferenceValue()==null?"unknown":value(q.remainingReferenceValue()))+"; shared haul available "+q.haulRemaining()+" units. Whole remaining batch now: "+(q.canOrderAllNow()?"feasible":"waiting")+".");
        for(var m:q.materials().stream().limit(8).toList())player.sendMessage(m.commodity().id()+": still needed "+m.missing()+"; paid "+m.ordered()+" (incoming "+m.inTransit()+"); delivered "+m.delivered()+"; free "+m.ready()+"; held for stages "+m.escrowed()+".");
        if(q.materials().size()>8)player.sendMessage("Showing the first 8 material types; all remain recorded.");
        player.sendMessage(q.explanation());player.sendMessage("Read-only reference report: no supplier selected, no goods reserved and no payment or block authority.");
    }
    private static String value(long hundredths){return java.math.BigDecimal.valueOf(hundredths,2).stripTrailingZeros().toPlainString();}
    private static void usage(CommandSender sender){sender.sendMessage("/civ build projects <village UUID> | funding <project UUID> | cancelproject <project UUID> | confirmcancel <token> | cancelrequest");}
    @Override public void close(){closed=true;waiting.clear();previews.clear();} // Accepted owner saves drain independently.
}
