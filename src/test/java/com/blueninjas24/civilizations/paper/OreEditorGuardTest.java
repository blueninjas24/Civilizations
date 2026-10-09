package com.blueninjas24.civilizations.paper;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreEditorGuardTest {
    private static Plugin plugin(String name) {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (p,m,a) -> {
            if (m.getName().equals("getName")) return name;
            throw new AssertionError("Guard must not inspect world, enabled state or plugin implementation: " + m.getName());
        });
    }
    private static OreEditorGuard guard(List<String> gaps, String... extra) {
        return new OreEditorGuard(gaps::add, Logger.getLogger("EditorGuardTest"), List.of(extra));
    }
    @Test void installedEditorFencesEvenWithoutEnabledStateAndDeterministicallyReportsOnce() {
        var gaps = new ArrayList<String>(); var guard = guard(gaps);
        guard.inspectInstalled(List.of(plugin("WorldEdit"), plugin("FastAsyncWorldEdit"), plugin("Civilizations")));
        assertEquals(1, gaps.size()); assertTrue(gaps.getFirst().contains("FastAsyncWorldEdit"));
        guard.inspect("WorldEdit");
        assertEquals(1, gaps.size());
    }
    @Test void laterEnableAndCustomDescriptorNamesAreCaseInsensitiveWithoutPrefixMatching() {
        var gaps = new ArrayList<String>(); var guard = guard(gaps, "CustomEditor");
        guard.inspectInstalled(List.of(plugin("WorldEditHelper"), plugin("Civilizations")));
        assertTrue(gaps.isEmpty());
        guard.inspect("customeditor");
        assertEquals(1, gaps.size());
        guard.inspectInstalled(List.of()); // Removal is not evidence that the missing interval is repaired.
        assertEquals(1, gaps.size());
    }
    @Test void everyBuiltInEditorIsGuardedWithNoOptionalSettings() {
        for (String editor : OreEditorGuard.KNOWN_EDITORS) {
            var gaps = new ArrayList<String>(); guard(gaps).inspect(editor);
            assertEquals(1, gaps.size());
        }
        assertThrows(IllegalArgumentException.class, () -> guard(new ArrayList<>(), "bad\nname"));
        assertThrows(IllegalArgumentException.class, () -> new OreEditorGuard(r -> fail(r), Logger.getAnonymousLogger(), Collections.nCopies(33,"Editor")));
    }
    @Test void additionalEditorConfigurationIsImmutableOptionalAndStrict() {
        var config = new YamlConfiguration();
        assertEquals(List.of(), EconomyConfiguration.readAdditionalOreEditors(config));
        config.set("ore-provenance.additional-bulk-editor-plugins", List.of("MyEditor", "Another-Editor"));
        var names = EconomyConfiguration.readAdditionalOreEditors(config);
        assertEquals(List.of("MyEditor", "Another-Editor"), names);
        assertThrows(UnsupportedOperationException.class, () -> names.add("Extra"));
        for (Object invalid : List.of("MyEditor", List.of(42), List.of(""), List.of("bad name"), Collections.nCopies(33,"Editor"))) {
            config.set("ore-provenance.additional-bulk-editor-plugins", invalid);
            assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.readAdditionalOreEditors(config));
        }
        config.set("ore-provenance.additional-bulk-editor-plugins", List.of());
        config.set("ore-provenance.disable-safety", true);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.readAdditionalOreEditors(config));
    }
}
