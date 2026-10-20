import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.ConstructionPreview;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.BuildingRegistry.Exclusion;
import com.blueninjas24.civilizations.paper.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.permissions.PermissionDefault;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual sign + synthetic player + private SQL owner, only in marked empty disposable loopback worlds. */
public final class BuildingProtectionProbe extends JavaPlugin {
    private PopulationSession session;private PaperBuildingProtection controls;private CivilizationsCommand router;
    private World world;private Block signBlock;private BlockData original;private Chunk ticket;
    private final UUID actor=UUID.randomUUID();private final List<Component> buttons=new ArrayList<>();
    private Player player;private Location location;private boolean allowed=true,authorized;private int stage,ticks,particles,x,y,z;
    private CompletableFuture<?> work;private UUID saved;private String confirmRelease;
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty private fixture required");authorized=true;
        getServer().getScheduler().runTaskLater(this,()->{try{prepare();new BukkitRunnable(){@Override public void run(){try {
            check(++ticks<1000&&getServer().getOnlinePlayers().isEmpty(),"Bounded empty native fixture stage="+stage);
            if(controls!=null)controls.run();if(work!=null&&!work.isDone())return;step();
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_BUILDING_PROTECTION_FAIL stage="+stage,failure);cleanup();cancel();getServer().shutdown();}}}.runTaskTimer(this,1,1);
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_BUILDING_PROTECTION_FAIL",failure);cleanup();getServer().shutdown();}},1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"Building protection guard failed",failure);if(authorized)getServer().shutdown();}}
    private void prepare()throws Exception {
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");check(plugin!=null&&plugin.isEnabled(),"Production plugin active");
        check(BuildingProtectionSettings.read(plugin.getConfig()).equals(BuildingProtectionSettings.prototype()),"Installed editable settings defaults");
        check(getServer().getPluginManager().getPermission(PaperBuildingProtection.MANAGE).getDefault()==PermissionDefault.TRUE,"Ordinary-player permission default");
        check(getServer().getPluginManager().getPermission(PaperBuildingProtection.ADMIN).getDefault()==PermissionDefault.OP,"Admin release override stays operator-only");
        var output=new ArrayList<String>();var console=(CommandSender)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{CommandSender.class},(p,m,a)->switch(m.getName()){
            case "hasPermission","isPermissionSet","isOp"->true;case "sendMessage"->{for(var item:a)if(item instanceof String s)output.add(s);yield null;}
            case "getServer"->getServer();case "getName"->"BuildingProbe";default->throw new UnsupportedOperationException(m.getName());});
        plugin.getCommand("civ").execute(console,"civ",new String[]{"building","protect"});
        check(output.stream().anyMatch(s->s.contains("in game")),"Production root routes building commands separately");
        check(plugin.getCommand("civ").tabComplete(console,"civ",new String[]{"building","cor"}).contains("corner1"),"Production tab completion delegates");
        world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();x=spawn.getBlockX()+4;z=spawn.getBlockZ()+4;
        check(world.isChunkLoaded(x>>4,z>>4),"Already-loaded test sign chunk");ticket=world.getChunkAt(x>>4,z>>4);ticket.addPluginChunkTicket(this);
        y=world.getHighestBlockYAt(x,z)+1;signBlock=world.getBlockAt(x,y,z);check(signBlock.getType().isAir(),"Fixture sign replaces only air");original=signBlock.getBlockData();
        signBlock.setType(Material.OAK_SIGN,false);var sign=(Sign)signBlock.getState();sign.getSide(Side.FRONT).line(0,Component.text("[Civ Building]"));sign.getSide(Side.FRONT).line(1,Component.text("protect"));check(sign.update(true,false),"Native protection sign text");
        player=(Player)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
            case "getUniqueId"->actor;case "getLocation"->location;case "getWorld"->world;case "hasPermission"->a[0].equals(PaperBuildingProtection.MANAGE)&&allowed;
            case "sendMessage"->{for(var item:a)if(item instanceof Component c)buttons.add(c);yield null;}
            case "spawnParticle"->{particles++;yield null;}case "getServer"->getServer();case "getName"->"SyntheticBuildingOwner";case "isOp"->false;
            default->throw new UnsupportedOperationException(m.getName());});
        session=new PopulationSession(getDataFolder().toPath().resolve("native-building-history").toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);
        work=session.start(false,Set.of(world.getUID()));
    }
    private void step()throws Exception {
        Object result=work==null?null:work.join();
        switch(stage) {
            case 0->{controls=new PaperBuildingProtection(new PaperBuildingProtection.Owner(){
                public boolean ready(){return session.status().ready();}
                public CompletableFuture<Exclusion> protect(Exclusion e,String name){return session.protectBuilding(e,name,true);}
                public CompletableFuture<Optional<Exclusion>> find(UUID id){return session.buildingExclusion(id);}
                public CompletableFuture<List<UUID>> at(UUID w,int xx,int zz){return session.buildingExclusionsAt(w,xx,zz);}
                public CompletableFuture<Void> release(UUID op,Exclusion e,UUID who,UUID w,boolean admin,long millis){return session.releaseBuildingExclusion(op,e.id(),who,w,admin,millis);}
            },BuildingProtectionSettings.prototype(),id->id.equals(actor)?player:null,System::currentTimeMillis,getLogger());
                router=new CivilizationsCommand(null,null,null,null,null,null,null,controls);
                location=new Location(world,x,y,z);command("building","corner1");location=new Location(world,x+4,y+3,z+4);command("building","corner2");location=new Location(world,x+1,y,z+1);
                var event=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,null,signBlock,BlockFace.UP,EquipmentSlot.HAND);controls.interact(event);
                check(event.isCancelled()&&!buttons.isEmpty()&&particles==68,"Actual sign requests a bounded clickable outlined preview");
                String payload=payload();saved=UUID.fromString(payload.split(" ")[3]);work=session.buildingExclusionsAt(world.getUID(),x+1,z+1);stage=1;}
            case 1->{check(((List<?>)result).isEmpty(),"Sign/preview alone grants nothing");execute(payload());work=session.buildingExclusionsAt(world.getUID(),x+1,z+1);stage=2;}
            case 2->{check(result.equals(List.of(saved)),"Confirmed non-admin plot persisted");signBlock.setType(Material.AIR,false);work=session.buildingExclusionsAt(world.getUID(),x+1,z+1);stage=3;}
            case 3->{check(result.equals(List.of(saved)),"Native sign removal retains saved protection");work=session.constructionProtections(world.getUID(),new ConstructionPreview.Bounds(new Position(x,y-10,z),new Position(x+4,y+10,z+4)));stage=4;}
            case 4->{check(((Set<?>)result).size()==25,"All25 selected columns protect every construction height");command("building","release",saved.toString());work=session.buildingExclusion(saved);stage=5;}
            case 5->{confirmRelease=payload();check(confirmRelease.contains("confirmrelease"),"Release requires separate exact confirmation");allowed=false;execute(confirmRelease);work=session.buildingExclusionsAt(world.getUID(),x+1,z+1);stage=6;}
            case 6->{check(result.equals(List.of(saved)),"Revoked player permission blocks release");allowed=true;execute(confirmRelease);work=session.buildingExclusionsAt(world.getUID(),x+1,z+1);stage=7;}
            case 7->{check(((List<?>)result).isEmpty(),"Confirmed owner release removes only active exclusion");work=session.buildingExclusion(saved);stage=8;}
            case 8->{check(((Optional<?>)result).isPresent(),"Original exclusion receipt retained");cleanup();getLogger().info("P4_BUILDING_PROTECTION_PASS: installed settings/permissions/routing; native sign and outlined non-admin preview; explicit save only; sign removal retains25 protected columns; revoked release refused; confirmed owner release retains history; original sign cell restored; no adoption or construction");stage=9;getServer().shutdown();}
            default->{}
        }
    }
    private String payload(){return ((ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload()).value();}
    private void execute(String payload){var parts=payload.split(" ");check(parts[0].equals("/civ"),"Expected exact civ confirmation");command(Arrays.copyOfRange(parts,1,parts.length));}
    private void command(String...args){router.onCommand(player,null,"civ",args);}
    private void cleanup(){if(controls!=null){controls.close();controls=null;}if(session!=null){session.close();session=null;}
        if(original!=null){check(world.isChunkLoaded(signBlock.getX()>>4,signBlock.getZ()>>4),"Cleanup never loads a chunk");signBlock.setBlockData(original,false);check(signBlock.getBlockData().getAsString().equals(original.getAsString()),"Original cell restored");original=null;}
        if(ticket!=null){ticket.removePluginChunkTicket(this);ticket=null;}}
    @Override public void onDisable(){if(authorized)cleanup();}
    private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
