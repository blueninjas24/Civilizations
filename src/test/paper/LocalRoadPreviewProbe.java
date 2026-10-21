import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;

/** Marked temporary loopback only; five fixture road cells restored, no fake player entity. */
public final class LocalRoadPreviewProbe extends JavaPlugin {
    private final Map<Location,String> originals=new LinkedHashMap<>();
    private final List<String> messages=new ArrayList<>();
    private boolean authorized,permission,connected=true;private int stage,ticks;private PaperConstructionPreview preview;
    private World world;private Location actorLocation;private UUID actor=UUID.randomUUID();
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;world=getServer().getWorlds().getFirst();for(var chunk:world.getLoadedChunks())world.addPluginChunkTicket(chunk.getX(),chunk.getZ(),this);
        new BukkitRunnable(){@Override public void run(){try{require(++ticks<600,"Road preview timeout");if(ticks<40)return;step();}catch(Throwable failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_ROAD_PREVIEW_FAIL",failure);restore();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Exception failure){throw new IllegalStateException(failure);}}
    private void step()throws Exception{
        if(stage==0){
            var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");var member=plugin.getClass().getDeclaredField("population");member.setAccessible(true);var population=(PopulationSession)member.get(plugin);if(!population.status().ready())return;
            world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=spawn.getBlockX()+8,z=spawn.getBlockZ();require(world.isChunkLoaded(x>>4,z>>4),"Fixture already loaded");int y=world.getHighestBlockYAt(x,z);
            for(int xx=x-8;xx<=x+8;xx++)for(int zz=z-8;zz<=z+8;zz++)require(world.isChunkLoaded(xx>>4,zz>>4),"Survey frame already loaded");
            for(int dz=-2;dz<=2;dz++){var at=new Location(world,x-8,y,z+dz);var block=at.getBlock();require(block.getType()==Material.GRASS_BLOCK,"Natural fixture ground");originals.put(at,block.getBlockData().getAsString());block.setType(Material.DIRT_PATH,false);}
            actorLocation=new Location(world,x-8+0.5,y+1,z+0.5,-90,0);
            Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(proxy,method,args)->switch(method.getName()){
                case "getUniqueId"->actor;case "getWorld"->world;case "getLocation"->actorLocation.clone();case "hasPermission"->permission;
                case "sendMessage"->{if(args[0] instanceof String s)messages.add(s);else if(args[0] instanceof String[] ss)messages.addAll(List.of(ss));yield null;}
                case "toString"->"Disposable road command actor";case "hashCode"->actor.hashCode();case "equals"->proxy==args[0];default->throw new UnsupportedOperationException(method.getName());});
            preview=new PaperConstructionPreview(PaperConstructionLibrary.load(plugin),population::constructionProtections,id->connected&&id.equals(actor)?player:null);
            preview.onCommand(player,null,"civ",new String[]{"build","access"});require(messages.stream().anyMatch(s->s.contains("permission")),"Read permission enforced");
            permission=true;preview.onCommand(player,null,"civ",new String[]{"build","access"});stage=1;return;
        }
        preview.run();
        if(stage==1&&messages.stream().anyMatch(s->s.contains("Flat local road access: READY"))){
            require(messages.stream().anyMatch(s->s.contains("new surface cells 8")),"Actual eight-cell connection");
            require(messages.stream().anyMatch(s->s.contains("Read-only observation")),"No work authority promised");
            restore();getLogger().info("P4_ROAD_PREVIEW_PASS: actual read-only access command and permission checks, bounded loaded native observations, real SQL protection reader, eight-cell route; original five fixture ground cells restored; no entity, supply, payment or project created.");stage=2;getServer().shutdown();
        }
    }
    private void restore(){if(!authorized)return;for(var entry:originals.entrySet()){var at=entry.getKey();require(at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4),"Cleanup cannot load chunks");at.getBlock().setBlockData(getServer().createBlockData(entry.getValue()),false);}originals.clear();if(world!=null)world.removePluginChunkTickets(this);}
    @Override public void onDisable(){restore();}
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
}
