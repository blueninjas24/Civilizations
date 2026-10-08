package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationDiagnostics;
import com.blueninjas24.civilizations.application.PopulationSession;
import org.bukkit.command.*;
import java.util.List;
import java.util.UUID;

/** Admin inspection only. Runs on the server thread and reads no JDBC connections. */
public final class PopulationDebugCommand implements CommandExecutor, TabCompleter {
    private final PopulationSession session;
    private final java.util.function.Supplier<String> observation;
    public PopulationDebugCommand(PopulationSession session) { this(session, () -> "Village observer not started"); }
    public PopulationDebugCommand(PopulationSession session, java.util.function.Supplier<String> observation) {
        this.session = session; this.observation = observation;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("civilizations.debug")) {
            sender.sendMessage("You do not have permission to inspect Civilizations."); return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("status")) {
            PopulationDiagnostics.status(session.status(), session.settings()).forEach(sender::sendMessage);
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
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("economy")) {
            try {
                var id = UUID.fromString(args[2]); var view = session.economyView().get(id);
                if (view == null) sender.sendMessage("Economy awaiting a reliable complete farm scan; unknown is not zero food.");
                else {
                    sender.sendMessage("Economy: completed " + view.account().state().completedPeriods() + " periods, queued " + view.queuedPeriods());
                    sender.sendMessage("Farm evidence: " + (view.farm().complete() ? "complete" : "partial: economy paused") + "; planted crops " + view.farm().plantedCrops());
                    sender.sendMessage(view.treasury() == null ? "Treasury: uninitialized; no income or starting money assumed."
                            : "Treasury: " + view.treasury().tradeValue() + " Trade Value (spending budget, not prosperity).");
                    if (view.needs() == null) sender.sendMessage("Household needs: no assessment saved for this period yet.");
                    else {
                        sender.sendMessage(view.needs().explanation());
                        view.needs().homes().stream().limit(20).forEach(home -> sender.sendMessage(home.explanation()));
                        if (view.needs().homes().size() > 20) sender.sendMessage("Showing 20 of " + view.needs().homes().size() + " saved household assessments.");
                    }
                    if (view.latest() != null) for (var report : view.latest().reports()) {
                        sender.sendMessage(report.explanation());
                        session.economySettings().ifPresent(settings -> sender.sendMessage("Indicative Trade Value/unit: " + settings.price(report.commodity(), report.condition())));
                    }
                    sender.sendMessage("Last economy failure: " + (session.economyFailure() == null ? "none" : session.economyFailure()));
                    if (view.priority() != null) {
                        sender.sendMessage(view.priority().explanation());
                        view.priority().candidates().forEach(candidate -> sender.sendMessage(candidate.action() + ": feasible " + candidate.feasible()
                                + ", eligible " + candidate.eligible() + ", weight " + candidate.weight() + "; " + candidate.explanation()));
                    }
                }
            } catch (IllegalArgumentException invalid) { sender.sendMessage("Settlement ID must be a UUID."); }
            return true;
        }
        sender.sendMessage("Usage: /civ debug status | observation | settlement <uuid> | explain <uuid> | economy <uuid>");
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("civilizations.debug")) return List.of();
        List<String> choices = args.length == 1 ? List.of("debug")
                : args.length == 2 && args[0].equalsIgnoreCase("debug") ? List.of("status", "observation", "settlement", "explain", "economy") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(java.util.Locale.ROOT);
        return choices.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
