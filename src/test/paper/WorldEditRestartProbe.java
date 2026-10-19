import com.blueninjas24.civilizations.application.PopulationSession;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/** Actual process-restart proof in the marked editor fixture; no chunk loads or world mutations. */
public final class WorldEditRestartProbe extends JavaPlugin {
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    ||!getServer().getIp().equals("127.0.0.1")||getServer().getPort()!=25578||!getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked no-player localhost fixture required");
            var plugin=getServer().getPluginManager().getPlugin("Civilizations");
            var field=plugin.getClass().getDeclaredField("population");field.setAccessible(true);
            var owner=(PopulationSession)field.get(plugin);var world=getServer().getWorlds().getFirst().getUID();
            new BukkitRunnable() {
                int ticks;CompletableFuture<Boolean> oldProof;
                @Override public void run() {
                    try {
                        if(++ticks>400)throw new IllegalStateException("Editor restart proof timed out");
                        if(!owner.status().ready())return;
                        if(!owner.oreCaptureReady()||!owner.forestCaptureReady()||owner.oreDiscoveryReady()||owner.forestDiscoveryReady())
                            throw new IllegalStateException("Editor restart must capture current writes without trusting prior terrain");
                        if(oldProof==null) {
                            long observed=System.currentTimeMillis();
                            oldProof=owner.canSurveyResourceChunk("ore",world,2048,-2048,observed)
                                    .thenCombine(owner.canSurveyResourceChunk("forest",world,2048,-2048,observed),(ore,forest)->ore||forest);
                            return;
                        }
                        if(!oldProof.isDone())return;
                        if(oldProof.join())throw new IllegalStateException("Previous boot's terrain certificate survived unverified editor bootstrap");
                        getServer().dispatchCommand(getServer().getConsoleSender(),"civ debug status");
                        getLogger().info("WORLDEDIT_RESTART_PROBE_PASSED actual process restart retained prior terrain history but revoked its authority; current capture ready, historical trust false, normal diagnostics available; no chunk loads/world mutations");
                        cancel();getServer().shutdown();
                    } catch(Throwable failure) {
                        cancel();getLogger().log(java.util.logging.Level.SEVERE,"WORLDEDIT_RESTART_PROBE_FAILED",failure);getServer().shutdown();
                    }
                }
            }.runTaskTimer(this,1,1);
        } catch(Throwable failure) {
            getLogger().log(java.util.logging.Level.SEVERE,"Editor restart fixture refused",failure);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
