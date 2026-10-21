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

/** Separate donor renovation grants and donor/admin revocations. Never starts a project or changes blocks. */
public final class PaperRenovationConsent extends BukkitRunnable implements CommandExecutor,Listener,AutoCloseable {
    public static final String PERMISSION="civilizations.building.renovate";
    public interface Owner {
        CompletableFuture<RenovationConsentRequests.Preview> preview(UUID player,UUID world,UUID building,com.blueninjas24.civilizations.database.BuildingRenovations.Action action,boolean permitted,boolean admin);
        CompletableFuture<com.blueninjas24.civilizations.database.BuildingRenovations.Receipt> confirm(UUID player,UUID world,UUID token,boolean permitted,boolean admin);
        void cancel(UUID player);
    }
    private static final class Request {
        final UUID actor,world;final long started;final CompletableFuture<RenovationConsentRequests.Preview> loading;
        RenovationConsentRequests.Preview preview;CompletableFuture<com.blueninjas24.civilizations.database.BuildingRenovations.Receipt> saving;
        Request(UUID actor,UUID world,long started,CompletableFuture<RenovationConsentRequests.Preview> loading){this.actor=actor;this.world=world;this.started=started;this.loading=loading;}
    }
    private final Owner owner;private final Function<UUID,Player> players;private final LongSupplier clock;private final Logger logger;
    private final Map<UUID,Request> requests=new LinkedHashMap<>();private boolean closed;
    public PaperRenovationConsent(PopulationSession session,Logger logger) {
        this(new Owner(){
            public CompletableFuture<RenovationConsentRequests.Preview> preview(UUID p,UUID w,UUID b,com.blueninjas24.civilizations.database.BuildingRenovations.Action action,boolean allowed,boolean admin){return session.previewRenovation(p,w,b,action,allowed,admin);}
            public CompletableFuture<com.blueninjas24.civilizations.database.BuildingRenovations.Receipt> confirm(UUID p,UUID w,UUID t,boolean allowed,boolean admin){return session.confirmRenovation(p,w,t,allowed,admin);}
            public void cancel(UUID player){session.cancelRenovation(player);}
        },Bukkit::getPlayer,System::currentTimeMillis,logger);
    }
    public PaperRenovationConsent(Owner owner,Function<UUID,Player> players,LongSupplier clock,Logger logger){this.owner=Objects.requireNonNull(owner);this.players=Objects.requireNonNull(players);this.clock=Objects.requireNonNull(clock);this.logger=Objects.requireNonNull(logger);}
    private boolean allowed(Player p){return p.hasPermission(PERMISSION)||p.hasPermission(PaperBuildingProtection.ADMIN);}
    private boolean current(Player p,Request r){return allowed(p)&&p.getWorld().getUID().equals(r.world);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player p)){sender.sendMessage("Review renovation consent in the building's saved world, in game.");return true;}
        if(closed||!allowed(p)){p.sendMessage("Renovation consent is unavailable or not permitted.");return true;}
        UUID actor=p.getUniqueId();var r=requests.get(actor);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"";
        try {
            if(action.equals("cancelrenovation")&&args.length==2){
                if(r!=null&&r.saving!=null){p.sendMessage("Your accepted renovation consent save is finishing.");return true;}
                discard(actor);p.sendMessage("Unsubmitted renovation consent cancelled.");return true;
            }
            if((action.equals("allowrenovation")||action.equals("denyrenovation"))&&args.length==3) {
                if(r!=null){p.sendMessage("Finish or cancel your current renovation consent first.");return true;}
                if(requests.size()>=4){p.sendMessage("Renovation consent busy; retry shortly.");return true;}
                UUID building=UUID.fromString(args[2]),world=p.getWorld().getUID();
                requests.put(actor,new Request(actor,world,clock.getAsLong(),owner.preview(actor,world,building,action.equals("allowrenovation")?com.blueninjas24.civilizations.database.BuildingRenovations.Action.GRANT:com.blueninjas24.civilizations.database.BuildingRenovations.Action.REVOKE,p.hasPermission(PERMISSION),p.hasPermission(PaperBuildingProtection.ADMIN))));
                p.sendMessage("Reviewing the recorded donor, current donation and separate renovation consent. No permission saved yet.");return true;
            }
            if(action.equals("confirmrenovation")&&args.length==3) {
                UUID token=UUID.fromString(args[2]);
                if(r==null||r.preview==null||!r.preview.token().equals(token))throw new IllegalArgumentException("No matching renovation consent preview");
                if(r.saving!=null){p.sendMessage("Your accepted renovation consent save is finishing.");return true;}
                if(!current(p,r)||clock.getAsLong()>=r.preview.expiresMillis())throw new IllegalArgumentException("World, permission or preview changed; review again");
                r.saving=owner.confirm(actor,r.world,token,p.hasPermission(PERMISSION),p.hasPermission(PaperBuildingProtection.ADMIN));
                p.sendMessage("Saving your separate renovation decision. Wait for confirmation.");return true;
            }
        }catch(IllegalArgumentException invalid){p.sendMessage("Renovation consent: "+invalid.getMessage());return true;}
        p.sendMessage("/civ building allowrenovation <building UUID> | denyrenovation <building UUID> | confirmrenovation <token> | cancelrenovation");return true;
    }
    private void discard(UUID actor){if(requests.remove(actor)!=null)owner.cancel(actor);}
    @Override public void run() {
        if(closed)return;long now=clock.getAsLong();
        for(var r:List.copyOf(requests.values())) {
            var p=players.apply(r.actor);
            try {
                if(r.saving!=null) {
                    if(!r.saving.isDone())continue;var receipt=r.saving.join();requests.remove(r.actor);
                    if(p!=null)p.sendMessage("Renovation decision saved: "+receipt.request().action()+" for "+receipt.request().building()+". Donation/use stays separate; no project started or blocks changed. Current donation and safe funded work are still required.");continue;
                }
                if(p==null||!current(p,r)){discard(r.actor);continue;}
                long deadline=r.preview==null?r.started+30_000:r.preview.expiresMillis();
                if(now<r.started||now>=deadline){p.sendMessage("Renovation consent preview expired; no renovation decision confirmed.");discard(r.actor);continue;}
                if(r.preview!=null||!r.loading.isDone())continue;r.preview=r.loading.join();
                var bounds=r.preview.layout().structure();
                p.sendMessage("Separate renovation preview: "+r.preview.terms().action()+" for building "+r.preview.layout().id()+". Structure: "+bounds+". This changes future renovation consent only; no building work, title, contents or housing changes now.");
                p.sendMessage("Only the recorded donor can allow renovations. Reclamation/re-donation invalidates old grants; denyrenovation withdraws this separate permission.");
                p.sendMessage(Component.text("[Confirm separate renovation decision]").clickEvent(ClickEvent.runCommand("/civ building confirmrenovation "+r.preview.token())));
            }catch(CompletionException|CancellationException|IllegalArgumentException failure) {
                Throwable cause=failure instanceof CompletionException&&failure.getCause()!=null?failure.getCause():failure;
                boolean expected=cause instanceof BuildingRegistry.Declined||cause instanceof CancellationException||cause instanceof IllegalArgumentException;
                if(!expected)logger.log(Level.WARNING,"Renovation consent failed",cause);
                if(p!=null)p.sendMessage("Renovation consent not confirmed: "+(expected?cause.getMessage():"inspect the server log before retrying"));
                if(r.saving!=null&&!expected)r.saving=null;else discard(r.actor);
            }
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event){var r=requests.get(event.getPlayer().getUniqueId());if(r!=null&&r.saving==null)discard(r.actor);}
    @Override public void close(){closed=true;for(var r:List.copyOf(requests.values()))if(r.saving==null)owner.cancel(r.actor);requests.clear();}
}
