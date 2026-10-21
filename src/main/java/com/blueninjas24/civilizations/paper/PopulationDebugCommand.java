package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationDiagnostics;
import com.blueninjas24.civilizations.application.PopulationSession;
import org.bukkit.command.*;
import java.util.List;
import java.util.UUID;

/** Admin inspection and explicitly confirmed recovery. Reads no JDBC connections on the server thread. */
public final class PopulationDebugCommand implements CommandExecutor, TabCompleter {
    private final PopulationSession session;
    private final java.util.function.Supplier<String> observation;
    private final java.util.function.Consumer<Runnable> dispatch;
    private CommandExecutor recovery;
    public void configureRecovery(CommandExecutor recovery){this.recovery=java.util.Objects.requireNonNull(recovery);}
    private CommandExecutor forestRecovery;
    public void configureForestRecovery(CommandExecutor recovery){this.forestRecovery=java.util.Objects.requireNonNull(recovery);}
    public PopulationDebugCommand(PopulationSession session) { this(session, () -> "Village observer not started"); }
    public PopulationDebugCommand(PopulationSession session, java.util.function.Supplier<String> observation) {
        this(session,observation,null);
    }
    public PopulationDebugCommand(PopulationSession session,java.util.function.Supplier<String> observation,java.util.function.Consumer<Runnable> dispatch) {
        this.session = java.util.Objects.requireNonNull(session);this.observation=java.util.Objects.requireNonNull(observation);this.dispatch=dispatch;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("civilizations.debug")) {
            sender.sendMessage("You do not have permission to inspect Civilizations."); return true;
        }
        if(args.length==2&&args[0].equalsIgnoreCase("debug")&&args[1].equalsIgnoreCase("villages")) {
            if(!session.status().ready()){sender.sendMessage("Population is still starting; retry shortly.");return true;}
            var names=session.settlementNames();sender.sendMessage("Known villages: "+names.size()+"; showing at most32 saved IDs.");
            names.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).limit(32)
                    .forEach(e->sender.sendMessage(e.getKey()+" : "+e.getValue()));
            if(names.size()>32)sender.sendMessage((names.size()-32)+" more villages omitted; ID completion also accepts a prefix.");
            if(names.isEmpty())sender.sendMessage("No saved villages yet; visit a loaded village with beds and villagers.");
            return true;
        }
        if(args.length>=2&&args[0].equalsIgnoreCase("debug")&&args[1].equalsIgnoreCase("recover-ore")) {
            if(recovery!=null)return recovery.onCommand(sender,command,label,args);
            sender.sendMessage("Resource recovery is not configured.");return true;
        }
        if(args.length>=2&&args[0].equalsIgnoreCase("debug")&&args[1].equalsIgnoreCase("recover-forest")) {
            if(forestRecovery!=null)return forestRecovery.onCommand(sender,command,label,args);
            sender.sendMessage("Forest recovery is not configured.");return true;
        }
        if(args.length==3&&args[0].equalsIgnoreCase("debug")&&args[1].equalsIgnoreCase("workforce")) {
            try{var town=UUID.fromString(args[2]);if(asyncReady(sender))inspect(sender,session.workforceReview(town));}
            catch(IllegalArgumentException invalid){sender.sendMessage("Settlement ID must be a UUID.");}
            return true;
        }
        if(args.length==2&&args[0].equalsIgnoreCase("debug")&&args[1].equalsIgnoreCase("resources")) {
            if(!asyncReady(sender))return true;
            inspect(sender,session.resourceReview());return true;
        }
        if(args.length==2&&args[0].equalsIgnoreCase("debug")&&args[1].equalsIgnoreCase("housing")) {
            sender.sendMessage(session.housingDiagnostic());return true;
        }
        if(args.length==2&&args[0].equalsIgnoreCase("debug")&&args[1].equalsIgnoreCase("construction")) {
            sender.sendMessage(session.constructionDiagnostic());sender.sendMessage(session.homePlanningDiagnostic());return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("status")) {
            PopulationDiagnostics.status(session.status(), session.settings()).forEach(sender::sendMessage);
            sender.sendMessage(session.oreTrackingDiagnostic());
            sender.sendMessage(session.forestTrackingDiagnostic());
            sender.sendMessage(session.industryRuntimeDiagnostic());
            sender.sendMessage(session.livestockCaptureDiagnostic());
            if (session.onlineOnly()) sender.sendMessage("Time policy: server downtime and absent worlds pause; shared calendar is an administrative date, not every world's age.");
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("observation")) {
            sender.sendMessage(observation.get()); return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("settlement")) {
            try {
                UUID id = UUID.fromString(args[2]);
                session.snapshot().ifPresentOrElse(state -> PopulationDiagnostics.settlement(state, id).forEach(sender::sendMessage),
                        () -> sender.sendMessage("Population is still starting; retry shortly."));
            } catch (IllegalArgumentException invalid) { sender.sendMessage("Settlement ID must be a UUID."); }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("explain")) {
            try {
                UUID id = UUID.fromString(args[2]);
                session.projectionView().ifPresentOrElse(view -> PopulationDiagnostics.explain(view, id).forEach(sender::sendMessage),
                        () -> sender.sendMessage("Population is still starting; retry shortly."));
            } catch (IllegalArgumentException invalid) { sender.sendMessage("Settlement ID must be a UUID."); }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("businesses")) {
            try {
                var id=UUID.fromString(args[2]);
                if(!session.status().ready())sender.sendMessage("Population is still starting; retry shortly.");
                else com.blueninjas24.civilizations.application.BusinessDiagnostics.describe(session.businessView(),id).forEach(sender::sendMessage);
            } catch(IllegalArgumentException invalid) { sender.sendMessage("Settlement ID must be a UUID."); }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("economy")) {
            try {
                var id = UUID.fromString(args[2]); var view = session.economyView().get(id);
                if (view == null) sender.sendMessage("Economy awaiting a reliable complete farm scan; unknown is not zero food.");
                else {
                    sender.sendMessage("Economy: completed " + view.account().state().completedPeriods() + " periods, queued " + view.queuedPeriods());
                    sender.sendMessage(view.farmBusiness());
                    sender.sendMessage(view.study());
                    sender.sendMessage("Farm evidence: " + (view.farm().complete() ? "complete" : "partial: economy paused") + "; planted crops " + view.farm().plantedCrops());
                    sender.sendMessage("Delivered food awaiting accounting: " + view.foodAid().pendingDeliveries() + " receipts; " + view.foodAid().nextPeriodUnits() + " units eligible in next period (up to 64 receipts).");
                    sender.sendMessage(view.treasury() == null ? "Treasury: uninitialized; no income or starting money assumed."
                            : "Treasury: " + view.treasury().tradeValue() + " Trade Value (spending budget, not prosperity).");
                    if (view.needs() == null) sender.sendMessage("Household needs: no assessment saved for this period yet.");
                    else {
                        sender.sendMessage(view.needs().explanation());
                        view.needs().homes().stream().limit(20).forEach(home -> sender.sendMessage(home.explanation()));
                        if (view.needs().homes().size() > 20) sender.sendMessage("Showing 20 of " + view.needs().homes().size() + " saved household assessments.");
                    }
                    if (view.wellbeing().isEmpty()) sender.sendMessage("Household wellbeing: no observation for this period (disabled, historical or no occupied homes).");
                    else {
                        view.wellbeing().stream().limit(20).forEach(home -> sender.sendMessage(home.explanation()));
                        if (view.wellbeing().size() > 20) sender.sendMessage("Showing 20 of " + view.wellbeing().size() + " wellbeing observations.");
                    }
                    if (view.latest() != null) for (var report : view.latest().reports()) {
                        sender.sendMessage(report.explanation());
                        session.economySettings().ifPresent(settings -> sender.sendMessage("Indicative Trade Value/unit: " + settings.price(report.commodity(), report.condition())));
                    }
                    sender.sendMessage("Last economy failure: " + (session.economyFailure() == null ? "none" : session.economyFailure()));
                    sender.sendMessage(view.appeal()==null ? "Food appeal: no observation saved for this period yet." : view.appeal().explanation());
                    view.foodImportImpact().ifPresent(impact -> sender.sendMessage(impact.explanation()));
                    if(view.response()!=null){sender.sendMessage(view.response().explanation());view.response().candidates().forEach(c->sender.sendMessage(c.explanation()));}
                    if (view.priority() != null) {
                        sender.sendMessage(view.priority().explanation());
                        view.priority().candidates().forEach(candidate -> sender.sendMessage(candidate.action() + ": feasible " + candidate.feasible()
                                + ", eligible " + candidate.eligible() + ", weight " + candidate.weight() + "; " + candidate.explanation()));
                    }
                }
            } catch (IllegalArgumentException invalid) { sender.sendMessage("Settlement ID must be a UUID."); }
            return true;
        }
        sender.sendMessage("Usage: /civ debug status | housing | construction | observation | villages | resources | workforce <uuid> | settlement <uuid> | explain <uuid> | economy <uuid> | businesses <uuid> | recover-ore | recover-forest");
        return true;
    }

    private boolean asyncReady(CommandSender sender) {
        if(!session.status().ready()){sender.sendMessage("Population is still starting; retry shortly.");return false;}
        if(dispatch==null){sender.sendMessage("Administrative report delivery is not configured.");return false;}
        return true;
    }
    private void inspect(CommandSender sender,java.util.concurrent.CompletableFuture<List<String>> result) {
        sender.sendMessage("Reading saved records; no resources or blocks will be changed.");
        result.whenComplete((lines,failure)->dispatch.accept(()->{
            if(sender instanceof org.bukkit.entity.Player player&&!player.isOnline())return;
            if(!sender.hasPermission("civilizations.debug"))return;
            if(failure!=null)sender.sendMessage("Saved report unavailable; check the server log and retry.");
            else lines.forEach(sender::sendMessage);
        }));
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("civilizations.debug")) return List.of();
        if(args.length==3&&args[0].equalsIgnoreCase("debug")&&java.util.Set.of("workforce","settlement","explain","economy","businesses").contains(args[1].toLowerCase(java.util.Locale.ROOT))) {
            String prefix=args[2].toLowerCase(java.util.Locale.ROOT);
            return session.settlementNames().keySet().stream().map(UUID::toString).filter(id->id.startsWith(prefix)).sorted().limit(32).toList();
        }
        List<String> choices = args.length == 1 ? List.of("debug")
                : args.length == 2 && args[0].equalsIgnoreCase("debug") ? List.of("status", "housing", "construction", "observation", "villages", "resources", "workforce", "settlement", "explain", "economy", "businesses", "recover-ore", "recover-forest") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(java.util.Locale.ROOT);
        return choices.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
