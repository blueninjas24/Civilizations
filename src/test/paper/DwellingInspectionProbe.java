import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;

/** Isolated actual block-observation check, not earned construction, donation or housing activation. */
public final class DwellingInspectionProbe extends JavaPlugin {
    private final Map<Position,String> originals=new TreeMap<>();
    private World world;private BuildingLayout layout;private PaperDwellingSurvey survey;
    private Position broken,roof;private String bedState;private int phase,ticks;private boolean authorized;
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    && getServer().getIp().equals("127.0.0.1") && getServer().getPort()==25578
                    && getServer().getOnlinePlayers().isEmpty(),"Marked empty private fixture required");
            authorized=true;
            new BukkitRunnable(){@Override public void run(){try {
                require(getServer().getOnlinePlayers().isEmpty(),"No player can enter fixture");
                require(++ticks<1200,"Bounded native dwelling check");
                if(phase==0){prepare();return;}
                var snapshot=survey.advance(32,System.currentTimeMillis());if(snapshot.isEmpty())return;
                var observation=snapshot.orElseThrow();
                if(phase==1) {
                    var result=DwellingInspection.inspect(layout,observation,System.currentTimeMillis(),5000);
                    require(observation.complete() && result.accessibleBeds()==4,"Four real beds and closed wooden-door access: "+result.issues()+"/"+result.units());
                    broken=layout.units().getFirst().beds().getFirst().head();
                    bedState=world.getBlockAt(broken.x(),broken.y(),broken.z()).getBlockData().getAsString();
                    world.getBlockAt(broken.x(),broken.y(),broken.z()).setType(Material.AIR,false);
                    survey=new PaperDwellingSurvey(layout,System.currentTimeMillis());phase=2;
                }else if(phase==2) {
                    var result=DwellingInspection.inspect(layout,observation,System.currentTimeMillis(),5000);
                    require(result.accessibleBeds()==0 && result.units().get(layout.units().getFirst().id()).issues().contains(DwellingInspection.Issue.BED_CHANGED),"Missing real bed half rejects the declared unit");
                    world.getBlockAt(broken.x(),broken.y(),broken.z()).setBlockData(getServer().createBlockData(bedState),false);
                    var foot=layout.units().getFirst().beds().getFirst().foot();roof=foot.plus(new Position(0,2,0));
                    world.getBlockAt(roof.x(),roof.y(),roof.z()).setType(Material.AIR,false);
                    survey=new PaperDwellingSurvey(layout,System.currentTimeMillis());phase=3;
                }else if(phase==3) {
                    var result=DwellingInspection.inspect(layout,observation,System.currentTimeMillis(),5000);
                    require(result.accessibleBeds()==0 && result.units().get(layout.units().getFirst().id()).issues().contains(DwellingInspection.Issue.BED_UNCOVERED),"Missing roof over one bed half rejects default housing");
                    require(DwellingInspection.inspect(layout,observation,System.currentTimeMillis(),5000,new DwellingInspection.Policy(false)).accessibleBeds()==4,"Explicit outdoor policy retains bed/access checks");
                    world.getBlockAt(roof.x(),roof.y(),roof.z()).setType(Material.GLASS,false);
                    survey=new PaperDwellingSurvey(layout,System.currentTimeMillis());phase=4;
                }else if(phase==4) {
                    require(DwellingInspection.inspect(layout,observation,System.currentTimeMillis(),5000).accessibleBeds()==4,"Glass roof provides native cover");
                    world.getBlockAt(roof.x(),roof.y(),roof.z()).setBlockData(getServer().createBlockData("minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"),false);
                    survey=new PaperDwellingSurvey(layout,System.currentTimeMillis());phase=5;
                }else if(phase==5) {
                    require(DwellingInspection.inspect(layout,observation,System.currentTimeMillis(),5000).accessibleBeds()==4,"Stair roof provides cover without becoming a traversable full-block route");
                    world.getBlockAt(roof.x(),roof.y(),roof.z()).setBlockData(getServer().createBlockData("minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=true]"),false);
                    survey=new PaperDwellingSurvey(layout,System.currentTimeMillis());phase=6;
                }else if(phase==6) {
                    require(DwellingInspection.inspect(layout,observation,System.currentTimeMillis(),5000).accessibleBeds()==0,"Waterlogged roof is not safe cover");
                    world.getBlockAt(roof.x(),roof.y(),roof.z()).setType(Material.OAK_PLANKS,false);
                    survey=PaperDwellingSurvey.forHomeDeclaration(world.getUID(),layout.structure(),System.currentTimeMillis());phase=7;
                }else if(phase==7) {
                    var declaration=HomeDeclaration.singleHousehold(layout.id(),layout.settlement(),world.getUID(),layout.plot(),layout.structure(),layout.entrance(),observation,System.currentTimeMillis(),DwellingInspection.Policy.prototype());
                    require(declaration.passes()&&declaration.layout().units().size()==1&&declaration.assessment().accessibleBeds()==4,"Native selected region becomes one reviewed family home, not four apartments");
                    require(declaration.layout().units().getFirst().id().equals(layout.units().getFirst().id()),"Stable declaration identity agrees with authored home identity");
                    var offset=new Position(16000,0,16000);var far=translated(layout,offset);
                    require(!world.isChunkLoaded(far.structure().min().x()>>4,far.structure().min().z()>>4),"Remote target starts unloaded");
                    var unknown=new PaperDwellingSurvey(far,System.currentTimeMillis()).advance(256,System.currentTimeMillis()).orElseThrow();
                    require(!unknown.complete() && unknown.cells().isEmpty(),"Unloaded observations are incomplete, never apparent air");
                    require(!world.isChunkLoaded(far.structure().min().x()>>4,far.structure().min().z()>>4),"Survey never force-loads remote target");
                    var absent=new BuildingLayout(UUID.randomUUID(),layout.settlement(),UUID.randomUUID(),layout.kind(),layout.plot(),layout.structure(),layout.entrance(),layout.shared(),layout.units());
                    require(!new PaperDwellingSurvey(absent,System.currentTimeMillis()).advance(64,System.currentTimeMillis()).orElseThrow().complete(),"Absent world stays incomplete");
                    restore();getLogger().info("P4_DWELLING_INSPECTION_PASS: four actual bed pairs/closed wooden door; broken bed and missing roof rejected; explicit outdoor override; native glass/stair cover; waterlogged roof refused; bounded region becomes one stable reviewed family-home declaration; absent/unloaded worlds incomplete; original144 cells restored; no consent/occupancy/world-loading authority");
                    cancel();getServer().shutdown();
                }
            }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_DWELLING_INSPECTION_FAIL",failure);try{restore();}finally{cancel();getServer().shutdown();}}}}.runTaskTimer(this,1,1);
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"Dwelling fixture guard failed",failure);if(authorized)getServer().shutdown();}
    }
    private void prepare()throws Exception {
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");require(plugin!=null && plugin.isEnabled(),"Production plugin enabled");
        world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=spawn.getBlockX()+4,z=spawn.getBlockZ()+4;
        for(int cx=x>>4;cx<=(x+5)>>4;cx++)for(int cz=z>>4;cz<=(z+5)>>4;cz++) {
            require(world.isChunkLoaded(cx,cz),"Only already-loaded fixture chunks");world.addPluginChunkTicket(cx,cz,this);
        }
        var origin=new Position(x,world.getHighestBlockYAt(x,z)+1,z);
        var plan=PaperConstructionLibrary.load(plugin).templates().get("starter_home");
        var max=origin.plus(new Position(plan.size().x()-1,plan.size().y()-1,plan.size().z()-1));
        var bounds=new Bounds(origin,max);
        var draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),origin,Transform.identity(),plan,System.currentTimeMillis());
        layout=HomeBuildingLayout.fromDraft(draft,bounds);
        for(var p:DwellingInspection.requiredCells(layout))originals.put(p,world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString());
        require(originals.size()==144,"Bounded complete native home volume");
        for(var p:originals.keySet())world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.AIR,false);
        for(var stage:plan.stages())for(var cell:stage.blocks().entrySet()) {
            var p=origin.plus(cell.getKey());world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(cell.getValue()),false);
        }
        survey=new PaperDwellingSurvey(layout,System.currentTimeMillis());phase=1;
    }
    private static BuildingLayout translated(BuildingLayout old,Position delta) {
        var units=old.units().stream().map(u->new Unit(u.id(),u.entrance().plus(delta),u.spaces().stream().map(b->move(b,delta)).toList(),
                u.beds().stream().map(b->new BuildingLayout.Bed(b.foot().plus(delta),b.facing())).toList())).toList();
        return new BuildingLayout(UUID.randomUUID(),old.settlement(),old.world(),old.kind(),move(old.plot(),delta),move(old.structure(),delta),
                old.entrance().plus(delta),old.shared().stream().map(b->move(b,delta)).toList(),units);
    }
    private static Bounds move(Bounds b,Position delta){return new Bounds(b.min().plus(delta),b.max().plus(delta));}
    private void restore() {
        if(!authorized || world==null)return;
        for(var entry:originals.entrySet()) {
            var p=entry.getKey();require(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Restore never loads chunks");
            world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(entry.getValue()),false);
        }
        world.removePluginChunkTickets(this);originals.clear();
    }
    @Override public void onDisable(){restore();}
    private static void require(boolean condition,String explanation){if(!condition)throw new IllegalStateException(explanation);}
}
