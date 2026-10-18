package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.event.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

/** Conservative presence fence, not a bulk-edit event integration or origin certificate. */
public final class OreEditorGuard implements Listener {
    public static final Set<String> KNOWN_EDITORS = Set.of("worldedit", "fastasyncworldedit", "voxelsniper");
    private final Set<String> editors;
    private final Consumer<String> gap;
    private final Logger logger;
    private boolean reported;

    public OreEditorGuard(PopulationSession session, Logger logger, Collection<String> additional) {
        this(reason->{session.invalidateOreTracking(reason);session.invalidateForestTracking(reason);}, logger, additional);
    }
    OreEditorGuard(Consumer<String> gap, Logger logger, Collection<String> additional) {
        this.gap = Objects.requireNonNull(gap); this.logger = Objects.requireNonNull(logger);
        var names = new HashSet<>(KNOWN_EDITORS);
        if (additional.size() > 32) throw new IllegalArgumentException("At most 32 additional bulk-editor names");
        for (String name : additional) {
            if (name == null || !name.matches("[A-Za-z0-9_-]{1,64}"))
                throw new IllegalArgumentException("Bulk-editor names must be 1..64 letters, digits, underscore or hyphen");
            names.add(name.toLowerCase(Locale.ROOT));
        }
        editors = Set.copyOf(names);
    }
    /** Inspect installed plugins, even disabled ones: their earlier edits are not certified. */
    public void inspectInstalled(Collection<Plugin> plugins) {
        plugins.stream().map(Plugin::getName).sorted(String.CASE_INSENSITIVE_ORDER).forEach(this::inspect);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnable(PluginEnableEvent event) { inspect(event.getPlugin().getName()); }

    void inspect(String name) {
        if (reported || !editors.contains(name.toLowerCase(Locale.ROOT))) return;
        reported = true;
        String reason = "unsupported bulk editor " + name + "; resource discovery needs reviewed provenance recovery";
        gap.accept(reason);
        logger.warning("New ore and forest discovery paused: " + reason + ". Saved deposits and population remain.");
    }
}
