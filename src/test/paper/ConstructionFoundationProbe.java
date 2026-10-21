import com.blueninjas24.civilizations.construction.ConstructionTemplate;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.*;

/** Native block-state/router acceptance; never writes or reads world blocks or entities. */
public final class ConstructionFoundationProbe extends JavaPlugin {
    private boolean authorized;
    @Override public void onEnable(){
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked private fixture required");
            authorized=true;
            getServer().getScheduler().runTask(this,()->{
                try{verify();getLogger().info("P4_CONSTRUCTION_FOUNDATION_PASS: native orientation, installed templates, routing, permissions, invalid-edit rejection; zero block writes");}
                catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_CONSTRUCTION_FOUNDATION_FAIL",failure);}
                finally{getServer().shutdown();}
            });
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4 probe guard failed",failure);if(authorized)getServer().shutdown();}
    }
    private void verify()throws Exception {
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");require(plugin!=null&&plugin.isEnabled(),"Production plugin active");
        var library=PaperConstructionLibrary.load(plugin);require(library.templates().size()==3,"Three installed original templates");
        require(library.funding().equals(com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.prototype())&&library.land().buildingBuffer()==4
                &&library.labor().equals(com.blueninjas24.civilizations.construction.ConstructionLaborPolicy.prototype())&&library.housing().maximumNewProposals()==1
                &&library.supplies().equals(com.blueninjas24.civilizations.construction.ConstructionSupplyPolicy.prototype()),"Installed construction policy defaults");
        var source=library.templates().get("starter_storehouse");var sourceHash=source.fingerprint();
        for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)) {
            var transform=new Transform(rotation,mirror);var plan=PaperConstructionLibrary.prepare(source,transform);
            require(plan.blockCount()==83&&plan.materials().equals(source.materials()),"Orientation must preserve geometry count and costs");
            var doorway=transform.apply(new Position(2,1,0),source.size());
            var state=plan.stages().get(1).blocks().get(doorway);var door=(Door)getServer().createBlockData(state);
            require(door.getFacing().name().equals(Direction.NORTH.transform(transform).name()),"Native door facing agrees with domain geometry");
            require(door.getHinge()==(mirror?Door.Hinge.RIGHT:Door.Hinge.LEFT),"Native mirror must flip door hinge");
            require(!door.isOpen()&&!door.isPowered(),"Passive native door state retained");
            for(var stage:plan.stages())for(var block:stage.blocks().entrySet())require(plan.size().contains(block.getKey()),"All native placements inside bounds");
        }
        require(source.fingerprint().equals(sourceHash),"Native preparation cannot mutate source blueprint");
        var home=library.templates().get("starter_home");require(home.blockCount()==120,"Original four-bed home geometry");
        for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)) {
            var plan=PaperConstructionLibrary.prepare(home,new Transform(rotation,mirror));var beds=plan.stages().getLast().blocks();int feet=0;
            for(var entry:beds.entrySet()) {
                var bed=(org.bukkit.block.data.type.Bed)getServer().createBlockData(entry.getValue());
                if(bed.getPart()!=org.bukkit.block.data.type.Bed.Part.FOOT)continue;feet++;
                var facing=bed.getFacing();var headAt=entry.getKey().plus(new Position(facing.getModX(),0,facing.getModZ()));
                require(beds.containsKey(headAt),"Rotated bed head follows native facing");
                var head=(org.bukkit.block.data.type.Bed)getServer().createBlockData(beds.get(headAt));
                require(head.getPart()==org.bukkit.block.data.type.Bed.Part.HEAD&&head.getFacing()==facing,"Paired native bed states agree");
            }
            require(feet==4,"Four complete bed pairs in every orientation");
        }
        var beam=new ConstructionTemplate("beam",1,"Beam","Original test",Purpose.PATH,new Size(2,1,1),List.of(new Stage("frame",1,
                Map.of(new com.blueninjas24.civilizations.economy.Commodity("timber"),1L),Map.of(new Position(0,0,0),"minecraft:oak_log[axis=x]"))),List.of());
        var rotated=PaperConstructionLibrary.prepare(beam,new Transform(Rotation.CLOCKWISE_90,false));
        require(((Orientable)getServer().createBlockData(rotated.stages().getFirst().blocks().values().iterator().next())).getAxis()==org.bukkit.Axis.Z,"Native horizontal pillar axis rotates");
        var messages=new ArrayList<String>();var sender=sender(messages,true);var command=plugin.getCommand("civ");
        require(command.execute(sender,"civ",new String[]{"build","templates"}),"Root router accepts catalogue");
        require(messages.stream().anyMatch(s->s.contains("starter_storehouse")),"Catalogue reaches command sender");
        messages.clear();command.execute(sender,"civ",new String[]{"debug","construction"});
        require(messages.size()==3&&messages.stream().anyMatch(s->s.startsWith("Home construction")),"Saved delivery, planning and work diagnostics routed without world writes");
        require(command.tabComplete(sender,"civ",new String[]{"debug","con"}).contains("construction"),"Construction diagnostic completion");
        messages.clear();command.execute(sender(messages,false),"civ",new String[]{"build","templates"});
        require(messages.size()==1&&messages.getFirst().contains("permission"),"Unprivileged sender cannot inspect construction");
        messages.clear();command.execute(sender,"civ",new String[]{"build","preview","starter_storehouse"});
        require(messages.stream().anyMatch(s->s.contains("require a player")),"Console cannot request a world site");
        require(command.tabComplete(sender,"civ",new String[]{"build","preview","starter_"}).contains("starter_storehouse"),"Root tab completion delegates");
        require(command.tabComplete(sender(messages,false),"civ",new String[]{"build","preview",""}).isEmpty(),"No unauthorized template completion");
        var file=plugin.getDataFolder().toPath().resolve("construction/starter_storehouse.yml");var original=Files.readAllBytes(file);
        try{
            Files.writeString(file,new String(original,java.nio.charset.StandardCharsets.UTF_8).replace("minecraft:cobblestone","minecraft:does_not_exist"));
            boolean rejected=false;try{PaperConstructionLibrary.load(plugin);}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected,"Unknown native block state must fail closed");
        }finally{Files.write(file,original);}
        require(PaperConstructionLibrary.load(plugin).templates().get(source.id()).fingerprint().equals(sourceHash),"Edited fixture restored without overwritten custom data");
    }
    private CommandSender sender(List<String> messages,boolean permitted){
        return (CommandSender)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{CommandSender.class},(proxy,method,args)->switch(method.getName()){
            case "hasPermission","isPermissionSet","isOp"->permitted;
            case "sendMessage"->{if(args!=null)for(var arg:args){if(arg instanceof String s)messages.add(s);if(arg instanceof String[] lines)messages.addAll(List.of(lines));}yield null;}
            case "getServer"->getServer();case "getName"->"P4Probe";case "toString"->"P4Probe";
            case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];
            default->throw new UnsupportedOperationException(method.getName());
        });
    }
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
}
