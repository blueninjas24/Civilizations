import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.PopulationDebugCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Marked native owner lifecycle/status and persisted interruption marker; no world/entity mutations. */
public final class LivestockRecoveryProbe extends JavaPlugin {
    private PopulationSession owner;private boolean authorized;
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");authorized=true;
        new BukkitRunnable(){int stage,ticks;CompletableFuture<?> work;Path data;UUID world;
            @Override public void run(){try {
                check(++ticks<=600,"Native capture recovery timeout");
                if(stage==0){world=getServer().getWorlds().getFirst().getUID();data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());owner=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),()->1000);work=owner.start(false,Set.of(world));stage=1;return;}
                if(!work.isDone())return;work.join();
                if(stage==1){check(owner.livestockCaptureReady(),"Fresh empty capture trusted");new PopulationDebugCommand(owner).onCommand(getServer().getConsoleSender(),getServer().getPluginCommand("civ"),"civ",new String[]{"debug","status"});
                    var first=new LivestockLosses.Observation(UUID.randomUUID(),UUID.randomUUID(),world,1000,"Explicit unassigned native fixture observation");var second=new LivestockLosses.Observation(UUID.randomUUID(),UUID.randomUUID(),world,1000,"Explicit second fixture observation");
                    owner.stageLivestockLoss(first);owner.stageLivestockLoss(second);check(!owner.livestockCaptureReady(),"All captured observations block readiness before owner queue");
                    work=owner.recordLivestockLoss(first).thenCompose(ignored->{check(!owner.livestockCaptureReady(),"Second queued capture still blocks readiness");return owner.recordLivestockLoss(second);})
                        .thenRun(()->{check(owner.livestockCaptureReady(),"Exact pending captures drained");owner.markLivestockCaptureGap();check(!owner.livestockCaptureReady(),"Gap pauses immediately");})
                        .thenCompose(ignored->owner.flushLivestockLosses(List.of())).thenRun(owner::close);stage=2;return;}
                if(stage==2){owner.close();owner=null;work=CompletableFuture.runAsync(()->verify(data,world));stage=3;return;}
                getLogger().info("LIVESTOCK_RECOVERY_PROBE_PASSED actual Paper owner/start/status, staged multi-observation pending gate, immediate gap gate, owner-callback and external shutdown drain, persisted gap/restart and simulated dirty-stop marker; no actual JVM crash, physical reconciliation or automatic production claim");cancel();getServer().shutdown();
            }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_RECOVERY_PROBE_FAILED",failure);cancel();getServer().shutdown();}}
        }.runTaskTimer(this,20,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_RECOVERY_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void verify(Path data,UUID world) {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var saved=new LivestockCapture(db.getConnection()).load().orElseThrow();check(saved.cleanStop()&&!saved.trusted(),"Drained clean stop retains prior gap");db.close();
            try(var restart=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),()->2000)){restart.start(false,Set.of(world)).join();check(!restart.livestockCaptureReady()&&restart.livestockCaptureDiagnostic().contains("review"),"No silent restart repair");}
            var dirty=data.resolve("dirty-run");var abrupt=new DatabaseManager(dirty.toFile(),getLogger());try{abrupt.initialize();check(new LivestockCapture(abrupt.getConnection()).begin(1).trusted(),"Explicit fresh dirty marker fixture");}finally{abrupt.close();}
            try(var restart=new PopulationSession(dirty.toFile(),getLogger(),PopulationSession.Settings.prototype(),()->3000)){restart.start(false,Set.of(world)).join();check(!restart.livestockCaptureReady(),"Prior dirty marker refuses capture trust");}
            var after=new DatabaseManager(dirty.toFile(),getLogger());try{after.initialize();var state=new LivestockCapture(after.getConnection()).load().orElseThrow();check(state.cleanStop()&&!state.trusted(),"Later clean stop cannot repair interrupted history");try(var s=after.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Foreign keys clean");}}finally{after.close();}
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    @Override public void onDisable(){if(owner!=null)owner.close();getLogger().info("LIVESTOCK_RECOVERY_FIXTURE_RESTORED no helper world/entity changes");}
}
