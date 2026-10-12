import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Guarded native normal-owner timer probe. Water evidence is explicit fixture data, not a live survey. */
public final class FisheryProbe extends JavaPlugin {
    private Path folder;private UUID world;private PopulationSession session;private ExecutorService executor;private boolean authorized;
    private void require(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    private void setup()throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();try {
            var town=UUID.randomUUID();var person=UUID.randomUUID();var site=UUID.randomUUID();var firm=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Native fishery fixture","Fixture",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(person,"Fixture fisher",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(person,"fishing",10);
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit isolated fixture funds");
            var requirements=new JobRequirements(216,Map.of("fishing",1L));
            db.getBusinessRepository().found(new BusinessFounding(new Business(firm,town,"Fixture fishery",person,person,"fishing",site,2,0),
                    new BusinessSite(site,town,world,0,64,0,10,"Explicit trusted isolated water fixture"),requirements,requirements,500,"Fixture opportunity"));
            db.getIndustryRepository().openStore(firm,100,Map.of(Commodity.FOOD,1L),1000);
            db.getBusinessRepository().hire(UUID.randomUUID(),firm,person,0);
            new FisheryRepository(db.getConnection()).open(site,new FisheryObservation(world,10,true,
                    java.util.stream.IntStream.range(0,40).mapToObj(x->new FisheryObservation.Column(x,0)).toList()),FishingPolicy.prototype());
            var loss=new FisheryRepository(db.getConnection()).habitat(UUID.randomUUID(),site,habitat(20,false));
            require(loss.lost()==240&&loss.after().equals(new FishStock(0,0,0)),"Confirmed dry habitat retires only remaining fish");
        }finally{db.close();}
    }
    private FisheryHabitatObservation habitat(long time,boolean suitable) {
        return new FisheryHabitatObservation(world,time,true,java.util.stream.IntStream.range(0,40)
                .mapToObj(x->new FisheryHabitatObservation.Support(new FisheryObservation.Column(x,0),suitable?
                        FisheryHabitatObservation.State.SUITABLE:FisheryHabitatObservation.State.ABSENT)).toList());
    }
    private void restore()throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();try {
            var fisheries=new FisheryRepository(db.getConnection());UUID site;
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT site_id FROM fishery_resources")) {require(r.next(),"Saved supports required");site=UUID.fromString(r.getString(1));}
            var restored=fisheries.habitat(UUID.randomUUID(),site,habitat(30,true));
            require(restored.after().equals(new FishStock(320,0,0))&&restored.lost()==0,"Restoration changes capacity without instant refill");
            require(fisheries.columns(site).size()==40,"Original support identity retained");
        }finally{db.close();}
    }
    private PopulationSession owner(boolean enabled) {
        var result=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);
        result.configureIndustryRuntime(new IndustryRuntimePolicy(true,1000,1,IndustryPolicy.prototype(),Map.of()));
        result.configureFishing(enabled?FishingPolicy.prototype():FishingPolicy.disabled());return result;
    }
    private List<Long> state()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro");var s=c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            try(var r=s.executeQuery("SELECT COALESCE(f.habitat_capacity,f.capacity),f.available,f.remainder,k.units FROM fishery_resources f JOIN businesses b ON b.site_id=f.site_id JOIN industry_stock k ON k.business_id=b.id AND k.commodity='food'")) {
                require(r.next(),"Fixture stock missing");return List.of(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4));
            }
        }
    }
    private void audit()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro");var s=c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            try(var r=s.executeQuery("PRAGMA user_version")){require(r.getInt(1)==67,"Schema67 required");}
            try(var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Foreign keys healthy");}
            try(var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"SQLite integrity healthy");}
            try(var r=s.executeQuery("SELECT SUM(hundredths) FROM (SELECT hundredths FROM treasuries UNION ALL SELECT hundredths FROM industry_stores)")){require(r.getLong(1)==9500,"Catch/recovery must not create money");}
            try(var r=s.executeQuery("SELECT SUM(caught) FROM fishery_work")){require(r.getLong(1)==state().get(3),"Owned food must equal actual recorded catches");}
            try(var r=s.executeQuery("SELECT COUNT(*),SUM(lost) FROM fishery_habitat_changes")){require(r.getInt(1)==2&&r.getLong(2)==240,"Loss/restoration audit conserved fish");}
        }
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost fixture only");
        authorized=true;world=getServer().getWorlds().getFirst().getUID();folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();
        var init=CompletableFuture.runAsync(()->{try{setup();}catch(Exception error){throw new CompletionException(error);}},executor);
        new BukkitRunnable(){CompletableFuture<?> pending=init;int phase=-3,ticks,pausedAt;List<Long> frozen;
            @Override public void run(){try {
                if(++ticks>600)throw new IllegalStateException("Native fishery timeout");
                if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                if(phase==-3){session=owner(true);pending=session.start(true,Set.of(world));phase++;return;}
                if(phase==-2){pausedAt=ticks;phase++;return;}
                if(phase==-1){if(ticks-pausedAt<60)return;require(state().equals(List.of(0L,0L,0L,0L)),"Dry habitat cannot catch or recover under normal owner timer");
                    session.close();session=null;pending=CompletableFuture.runAsync(()->{try{restore();}catch(Exception error){throw new CompletionException(error);}},executor);phase++;return;}
                if(phase==0){session=owner(true);pending=session.start(true,Set.of(world));phase++;return;}
                if(phase==1){if(state().get(3)<8)return;pending=session.updateLoadedWorlds(Set.of());phase++;return;}
                if(phase==2){frozen=state();pausedAt=ticks;phase++;return;}
                if(phase==3){if(ticks-pausedAt<60)return;require(frozen.equals(state()),"Absent world must pause fish recovery and catches exactly");
                    session.close();session=null;pausedAt=ticks;phase++;return;}
                if(phase==4){if(ticks-pausedAt<40)return;require(frozen.equals(state()),"Server-owner downtime must not recover stock");
                    session=owner(false);pending=session.start(true,Set.of(world));phase++;return;}
                if(phase==5){pausedAt=ticks;phase++;return;}
                if(phase==6){if(ticks-pausedAt<50)return;require(frozen.equals(state()),"Disabled resource policy must retain stock and food after restart");
                    session.close();session=owner(true);pending=session.start(true,Set.of(world));phase++;return;}
                if(phase==7){if(state().get(3)<=frozen.get(3))return;pending=session.updateLoadedWorlds(Set.of());phase++;return;}
                if(phase==8){session.close();session=null;require(state().get(0).equals(frozen.get(0)),"Restart must retain habitat capacity");audit();
                    getLogger().info("NATIVE FISHERY PASS: confirmed dry habitat paused normal timers; restored capacity without refill; conserved loss audit/catches/owned food/cash; absent world, downtime and disabled restart paused stock; resumed gradually; no world changes");
                    cancel();getServer().getPluginManager().disablePlugin(FisheryProbe.this);getServer().shutdown();}
            }catch(Exception error){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FISHERY FAIL",error);getServer().getPluginManager().disablePlugin(FisheryProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception error){getLogger().log(java.util.logging.Level.SEVERE,"Fishery fixture refused",error);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(session!=null)session.close();if(executor!=null)executor.shutdown();}
}
