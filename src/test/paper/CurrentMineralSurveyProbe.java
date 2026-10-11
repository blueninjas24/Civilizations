import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.MineralOpportunities;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Packaged SQL-owner fixture: synthetic observations in its own disposable DB, no world edits/tickets. */
public final class CurrentMineralSurveyProbe extends JavaPlugin {
    private PopulationSession session;
    private final AtomicLong clock=new AtomicLong(1000);
    private UUID world,town;
    private File folder;
    private List<MineralObservation> pair(long time,boolean present) {
        return List.of(new MineralObservation(world,time,true,Commodity.COAL,present?List.of(new VillageObservation.Position(0,40,0)):List.of()),
            new MineralObservation(world,time,true,Commodity.IRON,List.of()));
    }
    private void check(boolean okay,String message){if(!okay)throw new IllegalStateException(message);}
    private PopulationSession owner(){var s=new PopulationSession(folder,getLogger(),PopulationSession.Settings.prototype(),clock::get);s.configureOreTracking();return s;}
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25578 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked localhost disposable server without players required");
            world=getServer().getWorlds().getFirst().getUID();folder=new File(getDataFolder(),"run-"+UUID.randomUUID());session=owner();
            var home=new VillageObservation.Position(0,64,0);var second=new VillageObservation.Position(2,64,0);
            var village=new VillageObservation(world,"Disposable fixture",1000,true,List.of(home,second),List.of(
                new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture adult"),
                new VillageObservation.Person(UUID.randomUUID(),second,true,"Second fixture adult")));
            var run=session.start(false,Set.of(world)).thenCompose(s->session.observe(village)).thenCompose(result->{
                town=session.settlementNames().keySet().iterator().next();return session.surveyMinerals(town,pair(1000,true));
            }).thenCompose(result->session.currentMineralOpportunities(town,100)).thenCompose(review->{
                check(review.status()==MineralOpportunities.Status.CURRENT&&review.candidates().size()==1,"Fresh candidate missing");
                return session.updateLoadedWorlds(Set.of());
            }).thenCompose(result->session.currentMineralOpportunities(town,100)).thenCompose(review->{
                check(review.status()==MineralOpportunities.Status.INACTIVE_WORLD&&review.candidates().isEmpty(),"Absent world exposed a candidate");
                return session.updateLoadedWorlds(Set.of(world));
            }).thenCompose(result->{clock.set(2000);return session.currentMineralOpportunities(town,100);}).thenCompose(review->{
                check(review.status()==MineralOpportunities.Status.STALE&&review.candidates().isEmpty(),"Old evidence remained current");
                return session.surveyMinerals(town,pair(2000,false));
            }).thenCompose(result->session.surveyMinerals(town,pair(1000,true))).thenCompose(result->session.currentMineralOpportunities(town,100)).thenCompose(review->{
                check(review.status()==MineralOpportunities.Status.CURRENT&&review.capturedMillis()==2000&&review.candidates().isEmpty(),"Old scan restored missing ore");
                return session.mineralOpportunities(town,512);
            }).thenAccept(rows->check(rows.size()==1,"Historical observation lost"));
            new BukkitRunnable() {
                CompletableFuture<?> pending=run;int ticks;boolean restarted;
                public void run() {
                    try {
                        if(++ticks>600)throw new IllegalStateException("Owner probe timed out");
                        if(!pending.isDone())return;pending.join();
                        if(!restarted) {
                            session.close();session=owner();restarted=true;
                            pending=session.start(false,Set.of(world)).thenCompose(s->session.currentMineralOpportunities(town,100)).thenAccept(review->
                                check(review.status()==MineralOpportunities.Status.CURRENT&&review.capturedMillis()==2000&&review.candidates().isEmpty(),"Restart restored old candidates"));
                            return;
                        }
                        getLogger().info("NATIVE CURRENT MINERAL SURVEY PASS: packaged owner, world pause, freshness, empty replacement, retained history and restart");
                        cancel();getServer().getPluginManager().disablePlugin(CurrentMineralSurveyProbe.this);
                    } catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE CURRENT MINERAL SURVEY FAIL",failure);getServer().getPluginManager().disablePlugin(CurrentMineralSurveyProbe.this);}
                }
            }.runTaskTimer(this,1,1);
        } catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Current survey fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(session!=null)session.close();}
}
