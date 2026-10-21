package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
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

/** Explicit donor/admin repossession with a separate expiring household-impact confirmation. */
public final class PaperHomeReclamation extends BukkitRunnable implements CommandExecutor,Listener,AutoCloseable {
    public static final String PERMISSION="civilizations.building.reclaim";
    public interface Owner {
        CompletableFuture<BuildingConsentRequests.Preview> preview(UUID player,UUID world,UUID building,boolean permitted,boolean admin);
        CompletableFuture<BuildingRegistry.Consent> confirm(UUID player,UUID world,UUID token,boolean permitted,boolean admin);
        void cancel(UUID player);
    }
    private static final class Request {
        final UUID actor,world;final long started;final CompletableFuture<BuildingConsentRequests.Preview> loading;
        BuildingConsentRequests.Preview preview;CompletableFuture<BuildingRegistry.Consent> saving;
        Request(UUID actor,UUID world,long started,CompletableFuture<BuildingConsentRequests.Preview> loading){this.actor=actor;this.world=world;this.started=started;this.loading=loading;}
    }
    private final Owner owner;private final Function<UUID,Player> players;private final LongSupplier clock;private final Logger logger;
    private final Map<UUID,Request> requests=new LinkedHashMap<>();private boolean closed;
    public PaperHomeReclamation(PopulationSession session,Logger logger) {
        this(new Owner(){
            public CompletableFuture<BuildingConsentRequests.Preview> preview(UUID p,UUID w,UUID b,boolean allowed,boolean admin){return session.previewBuildingReclamation(p,w,b,allowed,admin);}
            public CompletableFuture<BuildingRegistry.Consent> confirm(UUID p,UUID w,UUID t,boolean allowed,boolean admin){return session.confirmBuildingConsent(p,w,t,false,false,allowed,admin);}
            public void cancel(UUID player){session.cancelBuildingConsent(player);}
        },Bukkit::getPlayer,System::currentTimeMillis,logger);
    }
    public PaperHomeReclamation(Owner owner,Function<UUID,Player> players,LongSupplier clock,Logger logger){this.owner=Objects.requireNonNull(owner);this.players=Objects.requireNonNull(players);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);}
    private boolean allowed(Player p){return p.hasPermission(PERMISSION)||p.hasPermission(PaperBuildingProtection.ADMIN);}
    private boolean current(Player p,Request r){return allowed(p)&&p.getWorld().getUID().equals(r.world);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player p)){sender.sendMessage("Reclaim a donated home in its saved world, in game.");return true;}
        if(closed||!allowed(p)){p.sendMessage("Building reclamation is unavailable or not permitted.");return true;}
        UUID actor=p.getUniqueId();var r=requests.get(actor);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"";
        try {
            if(action.equals("cancelreclaim")&&args.length==2){
                if(r!=null&&r.saving!=null){p.sendMessage("Your accepted reclamation save is finishing.");return true;}
                discard(actor);p.sendMessage("Unsubmitted reclamation cancelled.");return true;
            }
            if(action.equals("reclaim")&&args.length==3) {
                if(r!=null){p.sendMessage("Finish or cancel your current reclamation first.");return true;}
                if(requests.size()>=4){p.sendMessage("Building reclamation busy; retry shortly.");return true;}
                UUID building=UUID.fromString(args[2]),world=p.getWorld().getUID();
                requests.put(actor,new Request(actor,world,clock.getAsLong(),owner.preview(actor,world,building,p.hasPermission(PERMISSION),p.hasPermission(PaperBuildingProtection.ADMIN))));
                p.sendMessage("Reviewing donation ownership and affected saved households. Nothing reclaimed yet.");return true;
            }
            if(action.equals("confirmreclaim")&&args.length==3) {
                UUID token=UUID.fromString(args[2]);
                if(r==null||r.preview==null||!r.preview.token().equals(token))throw new IllegalArgumentException("No matching reclamation preview");
                if(r.saving!=null){p.sendMessage("Your accepted reclamation save is finishing.");return true;}
                if(!current(p,r)||clock.getAsLong()>=r.preview.expiresMillis())throw new IllegalArgumentException("World, permission or preview changed; review again");
                r.saving=owner.confirm(actor,r.world,token,p.hasPermission(PERMISSION),p.hasPermission(PaperBuildingProtection.ADMIN));
                p.sendMessage("Saving the confirmed withdrawal of village use. Wait for confirmation.");return true;
            }
        }catch(IllegalArgumentException invalid){p.sendMessage("Reclamation: "+invalid.getMessage());return true;}
        p.sendMessage("/civ building reclaim <building UUID> | confirmreclaim <token> | cancelreclaim");return true;
    }
    private void discard(UUID actor){if(requests.remove(actor)!=null)owner.cancel(actor);}
    @Override public void run() {
        if(closed)return;long now=clock.getAsLong();
        for(var r:List.copyOf(requests.values())) {
            var p=players.apply(r.actor);
            try {
                if(r.saving!=null) {
                    if(!r.saving.isDone())continue;var receipt=r.saving.join();requests.remove(r.actor);
                    if(p!=null)p.sendMessage("Village use withdrawn: "+receipt.building()+". Building and residents preserved; affected families need other suitable housing. The saved footprint remains protected.");continue;
                }
                if(p==null||!current(p,r)){discard(r.actor);continue;}
                long deadline=r.preview==null?r.started+30_000:r.preview.expiresMillis();
                if(now<r.started||now>=deadline){p.sendMessage("Reclamation preview expired; no withdrawal confirmed.");discard(r.actor);continue;}
                if(r.preview!=null||!r.loading.isDone())continue;r.preview=r.loading.join();
                if(r.preview.layout().kind()!=com.blueninjas24.civilizations.construction.BuildingLayout.Kind.HOUSING)throw new IllegalArgumentException("This control currently supports housing only");
                p.sendMessage("Reclaim building "+r.preview.building()+": "+r.preview.housingAssignments().size()+" assigned households lose their unit; "+r.preview.legacyHousingFamilies().size()+" other legacy families lose contributing bed capacity. Residents, blocks, contents and history remain. Rehousing needs a suitable permitted vacancy.");
                p.sendMessage(Component.text("[Confirm withdrawal of village use]").clickEvent(ClickEvent.runCommand("/civ building confirmreclaim "+r.preview.token())));
            }catch(CompletionException|CancellationException|IllegalArgumentException failure) {
                Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof CancellationException||cause instanceof IllegalArgumentException;
                if(!expected)logger.log(Level.WARNING,"Building reclamation failed",cause);
                if(p!=null)p.sendMessage("Reclamation not confirmed: "+(expected?cause.getMessage():"inspect the server log before retrying"));
                if(r.saving!=null&&!expected)r.saving=null;else discard(r.actor);
            }
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event){var r=requests.get(event.getPlayer().getUniqueId());if(r!=null&&r.saving==null)discard(r.actor);}
    @Override public void close(){closed=true;for(var r:List.copyOf(requests.values()))if(r.saving==null)owner.cancel(r.actor);requests.clear();}
}
