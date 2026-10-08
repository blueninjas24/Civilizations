package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationDiagnostics;
import com.blueninjas24.civilizations.application.PopulationSession;
import org.bukkit.command.*;
import java.util.List;
import java.util.UUID;

/** Admin inspection only. Runs on the server thread and reads no JDBC connections. */
public final class PopulationDebugCommand implements CommandExecutor, TabCompleter {
    private final PopulationSession session;
    public PopulationDebugCommand(PopulationSession session) { this.session = session; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("civilizations.debug")) {
            sender.sendMessage("You do not have permission to inspect Civilizations."); return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("status")) {
            PopulationDiagnostics.status(session.status(), session.settings()).forEach(sender::sendMessage);
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("settlement")) {
            try {
                UUID id = UUID.fromString(args[2]);
                session.snapshot().ifPresentOrElse(state -> PopulationDiagnostics.settlement(state, id).forEach(sender::sendMessage),
                        () -> sender.sendMessage("Population is still starting; retry shortly."));
            } catch (IllegalArgumentException invalid) { sender.sendMessage("Settlement ID must be a UUID."); }
            return true;
        }
        sender.sendMessage("Usage: /civ debug status | /civ debug settlement <uuid>");
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("civilizations.debug")) return List.of();
        List<String> choices = args.length == 1 ? List.of("debug")
                : args.length == 2 && args[0].equalsIgnoreCase("debug") ? List.of("status", "settlement") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(java.util.Locale.ROOT);
        return choices.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
