package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.BuildingLayout;
import com.blueninjas24.civilizations.construction.DwellingInspection;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/** Bounded, read-only diagnostics for saved housing; never consent or usable capacity. */
public final class PaperDwellingReview extends BukkitRunnable implements CommandExecutor,AutoCloseable {
    // The other128 cells belong to PaperHomeDonation: total live housing reads <=256/tick.
    public static final int MAX_REQUESTS=4,CELLS_PER_TICK=128;
    public interface Owner {
        CompletableFuture<BuildingRegistry.Entry> find(UUID building,UUID player,boolean administrator,UUID world);
        CompletableFuture<DwellingInspection.Assessment> inspect(BuildingLayout layout,DwellingInspection.Observation observation,long now);
    }
    public interface Survey { Optional<DwellingInspection.Observation> advance(int budget,long now); }
    private static final class Request {
        final UUID actor,world;final long started;final CompletableFuture<BuildingRegistry.Entry> lookup;
        BuildingRegistry.Entry entry;Survey survey;CompletableFuture<DwellingInspection.Assessment> result;
        Request(UUID actor,UUID world,long started,CompletableFuture<BuildingRegistry.Entry> lookup){this.actor=actor;this.world=world;this.started=started;this.lookup=lookup;}
    }
    private final Owner owner;private final Function<UUID,Player> players;
    private final BiFunction<BuildingLayout,Long,Survey> surveys;private final LongSupplier clock;private final Logger logger;
    private final LinkedHashMap<UUID,Request> requests=new LinkedHashMap<>();private boolean closed;
    public PaperDwellingReview(PopulationSession session,Logger logger) {
        this(session,DwellingInspection.Policy.prototype(),logger);
    }
    public PaperDwellingReview(PopulationSession session,DwellingInspection.Policy policy,Logger logger) {
        this(new Owner(){
            public CompletableFuture<BuildingRegistry.Entry> find(UUID id,UUID actor,boolean admin,UUID world){return session.buildingInspectionLayout(id,actor,admin,world);}
            public CompletableFuture<DwellingInspection.Assessment> inspect(BuildingLayout layout,DwellingInspection.Observation observation,long now){return session.inspectBuildingBeds(layout,observation,now,policy);}
        },Bukkit::getPlayer,(layout,now)->new PaperDwellingSurvey(layout,now)::advance,System::currentTimeMillis,logger);
    }
    public PaperDwellingReview(Owner owner,Function<UUID,Player> players,BiFunction<BuildingLayout,Long,Survey> surveys,LongSupplier clock,Logger logger) {
        this.owner=Objects.requireNonNull(owner);this.players=Objects.requireNonNull(players);this.surveys=Objects.requireNonNull(surveys);
        this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);
    }
    private boolean allowed(Player p){return p.hasPermission(PaperBuildingProtection.MANAGE)||p.hasPermission(PaperBuildingProtection.ADMIN);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage("Inspect housing in game, in its saved world.");return true;}
        if(closed||!allowed(player)){player.sendMessage("Building inspection is unavailable or not permitted.");return true;}
        if(args.length!=3){player.sendMessage("/civ building inspect <building UUID> (saved housing only; read-only)");return true;}
        UUID id;
        try{id=UUID.fromString(args[2]);}catch(IllegalArgumentException invalid){player.sendMessage("Building ID must be a UUID.");return true;}
        UUID actor=player.getUniqueId(),world=player.getWorld().getUID();
        if(requests.containsKey(actor)){player.sendMessage("Your building inspection is still running.");return true;}
        if(requests.size()>=MAX_REQUESTS){player.sendMessage("Building inspection busy; retry shortly.");return true;}
        requests.put(actor,new Request(actor,world,clock.getAsLong(),owner.find(id,actor,player.hasPermission(PaperBuildingProtection.ADMIN),world)));
        player.sendMessage("Checking saved housing beds and access. No residents, blocks or capacity will change.");return true;
    }
    @Override public void run() {
        if(closed)return;boolean advanced=false;long now=clock.getAsLong();
        for(var r:List.copyOf(requests.values())) {
            var player=players.apply(r.actor);
            if(player==null||!allowed(player)||!player.getWorld().getUID().equals(r.world)) {requests.remove(r.actor);continue;}
            if(now<r.started||now-r.started>30_000){player.sendMessage("Building inspection expired; no housing state changed.");requests.remove(r.actor);continue;}
            try {
                if(r.entry==null) {
                    if(!r.lookup.isDone())continue;
                    r.entry=r.lookup.join();
                }
                // Recheck current authority after lookup and before every survey/result tick.
                if(!r.entry.donor().equals(r.actor)&&!player.hasPermission(PaperBuildingProtection.ADMIN)) {
                    player.sendMessage("Building inspection authority changed; no housing state changed.");requests.remove(r.actor);continue;
                }
                if(r.result==null) {
                    if(advanced)continue;advanced=true;
                    if(r.survey==null)r.survey=surveys.apply(r.entry.layout(),now);
                    var observation=r.survey.advance(CELLS_PER_TICK,now);
                    if(observation.isEmpty())continue;
                    r.result=owner.inspect(r.entry.layout(),observation.orElseThrow(),now);
                }
                if(!r.result.isDone())continue;
                var assessment=r.result.join();
                if(now<assessment.capturedMillis()||now-assessment.capturedMillis()>30_000) {
                    player.sendMessage("Building inspection evidence expired; inspect again.");requests.remove(r.actor);continue;
                }
                player.sendMessage("Housing bed/access/cover inspection: "+r.entry.layout().id()+". Covered beds required: "+assessment.policy().requireCoveredBeds()+". Read-only; consent, occupancy and usable capacity are separate checks.");
                if(!assessment.issues().isEmpty())player.sendMessage("Unverified building evidence: "+issues(assessment.issues())+". Previous housing state is unchanged.");
                else {
                    player.sendMessage("Accessible declared beds: "+assessment.accessibleBeds()+" across "+assessment.units().size()+" declared units.");
                    for(var unit:new TreeMap<>(assessment.units()).values().stream().limit(8).toList())player.sendMessage("Unit "+unit.unit()+": "+unit.accessibleBeds()+" accessible beds; "+(unit.issues().isEmpty()?"bed/access checks passed":issues(unit.issues())));
                    if(assessment.units().size()>8)player.sendMessage("Showing the first8 units; "+(assessment.units().size()-8)+" more declared units omitted.");
                }
                requests.remove(r.actor);
            }catch(CompletionException|CancellationException|IllegalArgumentException failure) {
                Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof IllegalArgumentException||cause instanceof CancellationException;
                if(!expected)logger.log(Level.WARNING,"Building inspection failed",cause);
                player.sendMessage("Building inspection unavailable: "+(expected?cause.getMessage():"review the server log")+". No housing state changed.");requests.remove(r.actor);
            }
        }
    }
    private static String issues(Set<DwellingInspection.Issue> issues) {
        return issues.stream().sorted().map(i->switch(i){
            case WRONG_WORLD->"world differs";case LAYOUT_CHANGED->"layout changed";case STALE_OR_FUTURE->"evidence expired";
            case INCOMPLETE->"world/chunk/cell evidence incomplete";case ENTRANCE_UNSAFE->"entrance blocked or unsafe";
            case BED_CHANGED->"bed pair changed";case BED_UNSUPPORTED->"bed lacks safe support";case BED_OBSTRUCTED->"bed headroom blocked";
            case BED_UNCOVERED->"bed lacks overhead cover inside declared structure";
            case UNDECLARED_PRIVATE_BED->"extra undeclared bed";case UNIT_UNREACHABLE->"unit entrance unreachable";
            case BED_UNREACHABLE->"bed unreachable";case PATH_BUDGET->"access review budget exhausted";
        }).collect(java.util.stream.Collectors.joining(", "));
    }
    @Override public void close(){closed=true;requests.clear();}
}
