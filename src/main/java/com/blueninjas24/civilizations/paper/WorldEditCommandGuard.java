package com.blueninjas24.civilizations.paper;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.PluginDisableEvent;

/** Fences unreviewed WorldEdit command paths before dispatch; it does not cancel the editor command. */
public final class WorldEditCommandGuard implements Listener {
    private final WorldEditResourceCapture capture;

    public WorldEditCommandGuard(WorldEditResourceCapture capture) {
        this.capture=Objects.requireNonNull(capture);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        inspect(event.getMessage());
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        inspect(event.getCommand());
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void onEditorDisable(PluginDisableEvent event) {
        if(event.getPlugin().getName().equalsIgnoreCase("WorldEdit"))
            capture.rejectUnsupportedOperation("WorldEdit disabled during capture; restart required");
    }

    private void inspect(String commandLine) {
        String reason=WorldEditCapturePolicy.unsupportedCommandReason(commandLine);
        if(!reason.isEmpty())capture.rejectUnsupportedOperation(reason);
    }
}
