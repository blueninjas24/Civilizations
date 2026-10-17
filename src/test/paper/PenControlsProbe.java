import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;

/** Marked empty loopback only. Actual blocks/cow + synthetic player interactions, private SQL. */
public final class PenControlsProbe extends JavaPlugin {
    private final Map<Block,BlockData> originals=new LinkedHashMap<>();
    private final List<String> messages=new ArrayList<>();private UUID actor=UUID.randomUUID(),pen;
    private boolean authorized,admin=true;private World world;private Chunk chunk;private Cow cow;private Block signBlock;
    private Player player;private Location location;private Path data;private PopulationSession session;private PaperPenControls controls;
    private CompletableFuture<?> work;private int stage,ticks;private int x,y,z;
    private void check(boolean value,String text){if(!value)throw new IllegalStateException(text);}
    private void stopControls(){if(controls!=null){controls.close();controls=null;}if(session!=null){session.close();session=null;}}
    private void restore(){stopControls();if(cow!=null){cow.remove();cow=null;}for(var item:originals.entrySet())item.getKey().setBlockData(item.getValue(),false);originals.clear();if(chunk!=null)chunk.removePluginChunkTicket(this);}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked no-player fixture required");authorized=true;
        getServer().getScheduler().runTaskLater(this,()->{try{prepare();getServer().getScheduler().runTaskTimer(this,()->{
            try{check(++ticks<600&&getServer().getOnlinePlayers().isEmpty(),"Bounded empty fixture required stage="+stage);step();}
            catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"PEN_CONTROLS_PROBE_FAILED stage="+stage,failure);restore();getServer().shutdown();}
        },1,1);}catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"PEN_CONTROLS_PROBE_FAILED",failure);restore();getServer().shutdown();}},60);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"PEN_CONTROLS_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void prepare()throws Exception{
        world=getServer().getWorlds().getFirst();var loaded=world.getLoadedChunks();
        if(loaded.length>0)chunk=loaded[0];else{
            var spawn=world.getSpawnLocation();int cx=Math.floorDiv(spawn.getBlockX(),16),cz=Math.floorDiv(spawn.getBlockZ(),16);
            check(world.isChunkGenerated(cx,cz)&&world.loadChunk(cx,cz,false),"Existing generated fixture chunk required");chunk=world.getChunkAt(cx,cz);
        }
        chunk.addPluginChunkTicket(this); // Explicit disposable fixture only; production never loads chunks.
        x=chunk.getX()*16+8;z=chunk.getZ()*16+8;y=world.getMaxHeight()-16;
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-1;dy<=1;dy++){
            var b=world.getBlockAt(x+dx,y+dy,z+dz);check(b.getType()==Material.AIR,"Empty sky fixture required");originals.put(b,b.getBlockData().clone());
        }
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)world.getBlockAt(x+dx,y-1,z+dz).setType(Material.OAK_PLANKS,false);
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)==2||Math.abs(dz)==2)world.getBlockAt(x+dx,y,z+dz).setType(Material.OAK_FENCE,true);
        signBlock=world.getBlockAt(x+3,y,z);signBlock.setType(Material.OAK_SIGN,false);var sign=(Sign)signBlock.getState();sign.getSide(Side.FRONT).line(0,Component.text("[Civ Protect]"));check(sign.update(true,false),"Native sign update");
        cow=world.spawn(new Location(world,x+0.5,y,z+0.5),Cow.class,c->{c.setAI(false);c.setGravity(false);c.setPersistent(true);});
        location=new Location(world,x+0.5,y,z+0.5);
        player=(Player)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
            case "getUniqueId"->actor;case "getLocation"->location.clone();case "getWorld"->world;
            case "hasPermission"->a[0].equals(PaperPenControls.ADMIN)?admin:true;
            case "sendMessage"->{messages.add((String)a[0]);getLogger().info("Synthetic pen user: "+a[0]);yield null;}
            case "spawnParticle"->null;default->throw new AssertionError(m.getName());});
        data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());check(!Files.exists(data),"Fresh private SQL required");start();
    }
    private void start(){session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);work=session.start(false,Set.of(world.getUID()));}
    private void adapters()throws Exception{
        // Test-only native player lookup; production uses Bukkit's actual online players.
        var constructor=PaperPenControls.class.getDeclaredConstructor(PopulationSession.class,PenProtectionSettings.class,Logger.class,Function.class);constructor.setAccessible(true);
        controls=constructor.newInstance(session,new PenProtectionSettings(true,8,64,2,64,8,2,60),getLogger(),(Function<UUID,Player>)id->id.equals(actor)?player:null);
    }
    private void click(){var event=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,null,signBlock,BlockFace.WEST,EquipmentSlot.HAND);controls.interact(event);check(event.isCancelled(),"Protection sign must route explicitly");}
    private void command(String... args){controls.onCommand(player,null,"civ",args);}
    private boolean has(String prefix){return messages.stream().anyMatch(s->s.startsWith(prefix));}
    private void step()throws Exception{
        if(controls!=null)controls.run();
        switch(stage){
            case 0->{if(!work.isDone())return;work.join();adapters();admin=false;click();check(has("An admin must"),"Unknown pen requires admin");admin=true;messages.clear();click();stage=1;}
            case 1->{if(!has("Pen preview:"))return;check(messages.stream().anyMatch(s->s.contains("9 floor columns; 1 cows/chickens")),"Actual geometry/animal count");work=session.protectedPensAt(world.getUID(),y,x,z);stage=2;}
            case 2->{if(!work.isDone())return;check(((List<?>)work.join()).isEmpty(),"Preview must be read-only");command("pen","confirm");stage=3;}
            case 3->{if(!has("Pen protected:"))return;var message=messages.stream().filter(s->s.startsWith("Pen protected:")).findFirst().orElseThrow();pen=UUID.fromString(message.substring("Pen protected: ".length()).split("\\.")[0]);
                check(!cow.hasAI()&&!cow.hasGravity(),"Protection cannot change animal movement");stopControls();start();stage=4;}
            case 4->{if(!work.isDone())return;work.join();adapters();messages.clear();command("pen","here");stage=5;}
            case 5->{if(!has("Saved protection here:"))return;check(messages.getLast().contains(pen.toString()),"Saved pen survives restart");command("pen","release",pen.toString());stage=6;}
            case 6->{if(!has("Release area "))return;command("pen","confirmrelease");stage=7;}
            case 7->{if(!has("Pen area released."))return;stopControls();
                var db=new DatabaseManager(data.toFile(),getLogger());try{db.initialize();check(new PenProtectionRepository(db.getConnection()).load(pen).orElseThrow().animals().equals(Set.of(cow.getUniqueId())),"Historical roster retained");
                    check(new PenProtectionRepository(db.getConnection()).at(world.getUID(),y,x,z).isEmpty(),"Area released");check(new AnimalOwnershipRepository(db.getConnection()).load(cow.getUniqueId()).orElseThrow().player().equals(actor),"Animal owner retained");}finally{db.close();}
                restore();getLogger().info("PEN_CONTROLS_PROBE_PASSED native sign event, admin gate, actual nine-column pen/one cow, explicit recapture confirmation, private SQL, controller/owner restart, here lookup, two-step release, intact animal owner/history; synthetic player, not human-client proof");getLogger().info("PEN_CONTROLS_FIXTURE_RESTORED");stage=99;getServer().shutdown();}
        }
    }
    @Override public void onDisable(){restore();}
}
