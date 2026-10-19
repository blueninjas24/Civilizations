package com.blueninjas24.civilizations.paper;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.bukkit.plugin.Plugin;

/** Narrow compatibility boundary for the one audited WorldEdit Bukkit runtime. */
public final class WorldEditCapturePolicy {
    public static final String SUPPORTED_PLUGIN_NAME = "WorldEdit";
    public static final String SUPPORTED_PLUGIN_VERSION = "7.4.4+7546-f9e033f";

    // Only these command paths have been exercised through the synchronous extent hook.
    private static final Set<String> CAPTURED_COMMANDS = Set.of("set", "undo");
    private static final Set<String> NON_MUTATING_COMMANDS = Set.of(
            "pos1", "pos2", "hpos1", "hpos2", "wand", "sel", "desel", "expand", "contract", "shift",
            "inset", "outset", "chunkinfo", "listchunks", "count", "size", "help", "limit", "drawsel");

    private WorldEditCapturePolicy() {}

    public static boolean supportsPlugin(Plugin plugin, boolean fastAsyncWorldEditPresent) {
        if (plugin == null || fastAsyncWorldEditPresent || !plugin.isEnabled()) return false;
        return SUPPORTED_PLUGIN_NAME.equalsIgnoreCase(plugin.getName())
                && SUPPORTED_PLUGIN_VERSION.equals(plugin.getPluginMeta().getVersion());
    }

    /** Returns a fail-closed reason for an unreviewed WorldEdit command, or empty for an audited command. */
    public static String unsupportedCommandReason(String commandLine) {
        if (commandLine == null) return "WorldEdit command text unavailable";
        String command = commandLine.trim();
        if (command.startsWith("//")) {
            String tail = command.substring(2).trim();
            if (tail.isEmpty()) return "WorldEdit command could not be classified";
            String verb = firstWord(tail).toLowerCase(Locale.ROOT);
            if (CAPTURED_COMMANDS.contains(verb) || NON_MUTATING_COMMANDS.contains(verb)) return "";
            return "unsupported WorldEdit command //" + verb;
        }

        // Namespaced command dispatch is not an audited WorldEdit command form.
        String first = firstWord(command.startsWith("/") ? command.substring(1).trim() : command);
        if (first.toLowerCase(Locale.ROOT).startsWith("worldedit:"))
            return "namespaced WorldEdit command is outside the audited command form";
        return "";
    }

    /** WorldEdit's own command bus also covers aliases and plugin-dispatched commands. */
    public static String unsupportedEditorCommandReason(String arguments) {
        if(arguments==null)return "WorldEdit command text unavailable";
        String command=arguments.trim();
        while(command.startsWith("/"))command=command.substring(1);
        String verb=firstWord(command).toLowerCase(Locale.ROOT);
        return CAPTURED_COMMANDS.contains(verb)||NON_MUTATING_COMMANDS.contains(verb)
                ? "" : "WorldEdit operation outside the verified synchronous scope";
    }

    private static String firstWord(String value) {
        Objects.requireNonNull(value);
        int whitespace = 0;
        while (whitespace < value.length() && !Character.isWhitespace(value.charAt(whitespace))) whitespace++;
        return value.substring(0, whitespace);
    }
}
