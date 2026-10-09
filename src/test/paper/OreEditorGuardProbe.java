import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.paper.OreEditorGuard;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Disposable test-only plugin. Its actual PluginEnableEvent acts as a configured editor. */
public final class OreEditorGuardProbe extends JavaPlugin {
    private PopulationSession session;
    private CompletableFuture<?> ready;
    @Override public void onEnable() {
        try {
            var root = getServer().getWorldContainer().toPath().toRealPath();
            if (!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25577 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked localhost disposable server without players required");
            var folder = new java.io.File(getDataFolder(), "run-" + UUID.randomUUID());
            session = new PopulationSession(folder, getLogger(), PopulationSession.Settings.prototype(), System::currentTimeMillis);
            session.configureOreTracking();
            var guard = new OreEditorGuard(session, getLogger(), List.of(getName()));
            getServer().getPluginManager().registerEvents(guard, this);
            ready = session.start(false);
            new BukkitRunnable() {
                int ticks;
                @Override public void run() {
                    try {
                        if (++ticks > 200) throw new IllegalStateException("Editor probe timed out");
                        if (!ready.isDone()) return;
                        ready.join();
                        if (session.oreDiscoveryReady() || !session.oreTrackingDiagnostic().contains(getName()))
                            throw new IllegalStateException("Native enable did not fence discovery: " + session.oreTrackingDiagnostic());
                        session.close(); session = null;
                        var reopened = new PopulationSession(folder, getLogger(), PopulationSession.Settings.prototype(), System::currentTimeMillis);
                        session = reopened; reopened.configureOreTracking(); ready = reopened.start(false);
                        cancel();
                        new BukkitRunnable() {
                            int attempts;
                            @Override public void run() {
                                try {
                                    if (++attempts > 200) throw new IllegalStateException("Reopen timed out");
                                    if (!ready.isDone()) return;
                                    ready.join();
                                    if (reopened.oreDiscoveryReady()) throw new IllegalStateException("Removed editor silently restored trust");
                                    getLogger().info("NATIVE ORE EDITOR GUARD PASS: actual configured PluginEnableEvent fenced discovery; clean reopen retained gap; separate empty DB, no world edits");
                                    cancel(); getServer().getPluginManager().disablePlugin(OreEditorGuardProbe.this);
                                } catch (RuntimeException failure) { cancel(); fail(failure); }
                            }
                        }.runTaskTimer(OreEditorGuardProbe.this,1,1);
                    } catch (RuntimeException failure) { cancel(); fail(failure); }
                }
            }.runTaskTimer(this,1,1);
        } catch (Exception failure) { fail(failure); }
    }
    private void fail(Exception failure) {
        getLogger().log(java.util.logging.Level.SEVERE,"NATIVE ORE EDITOR GUARD FAIL",failure);
        getServer().getPluginManager().disablePlugin(this);
    }
    @Override public void onDisable() { if (session != null) { session.close(); session = null; } }
}
