import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.PaperWaterSource;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.Files;

/** Guarded full-height native snapshot proof. No block/entity changes, writes or production chunk loading. */
public final class WaterSourceProbe extends JavaPlugin {
    private boolean authorized;
    private void require(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost fixture only");
        authorized=true;
        getServer().getScheduler().runTask(this,()->{try {
            require(Bukkit.isPrimaryThread(),"Capture must run on main thread");var world=getServer().getWorlds().getFirst();
            var loaded=world.getLoadedChunks();boolean fixtureTicket=loaded.length==0;
            Chunk chunk;ChunkSnapshot snapshot;
            if(fixtureTicket)world.addPluginChunkTicket(0,0,this);
            try{chunk=fixtureTicket?world.getChunkAt(0,0):loaded[0];snapshot=chunk.getChunkSnapshot(false,false,false,false);}
            finally{if(fixtureTicket)world.removePluginChunkTicket(0,0,this);}
            var minimum=world.getMinHeight();var maximum=world.getMaxHeight();var captured=System.currentTimeMillis();
            var scan=new WaterScan(new PaperWaterSource(snapshot,minimum,maximum),chunk.getX(),chunk.getZ(),minimum,maximum,2);
            new BukkitRunnable(){int ticks,reads;
                @Override public void run(){try {
                    require(++ticks<=4096,"Bounded native water timeout");int used=scan.advance(512);require(used<=512,"Snapshot read budget");reads+=used;
                    if(!scan.complete())return;var evidence=scan.observation(world.getUID(),captured);
                    require(evidence.complete()&&evidence.columns().size()==256,"Complete actual snapshot evidence");
                    require(reads==(maximum-minimum)*256,"Every absolute-Y cell read once");
                    for(var column:evidence.columns()) {
                        int x=Math.floorMod(column.position().x(),16),z=Math.floorMod(column.position().z(),16),y=maximum-1;
                        while(y>=minimum&&snapshot.getBlockType(x,y,z).isAir())y--;
                        int surface=y,depth=0;while(y>=minimum&&snapshot.getBlockType(x,y,z)==Material.WATER){depth++;y--;}
                        boolean suitable=depth>=2;
                        require((column.state()==FisheryHabitatObservation.State.SUITABLE)==suitable,"Independent surface/depth reference");
                        if(suitable)require(column.surfaceY()==surface,"Actual surface elevation retained");
                    }
                    getLogger().info("NATIVE WATER SOURCE PASS: full-height actual snapshot matched 256 independent column checks; reads="+reads+
                            ", suitable="+evidence.suitableColumns().size()+"; 512-cell tick budget; no block/entity changes; fixture-only capture ticket released");
                    cancel();getServer().getPluginManager().disablePlugin(WaterSourceProbe.this);getServer().shutdown();
                }catch(Exception error){cancel();fail(error);}}
            }.runTaskTimer(this,1,1);
        }catch(Exception error){fail(error);}});
    }catch(Exception error){fail(error);}}
    private void fail(Exception error){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WATER SOURCE FAIL",error);
        getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}
}
