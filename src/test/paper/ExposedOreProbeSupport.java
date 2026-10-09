import com.blueninjas24.civilizations.industry.OreScan;
import com.blueninjas24.civilizations.paper.PaperOreSource;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Called only after WorkshopProductionProbe's marked disposable-server guard, on the main thread. */
final class ExposedOreProbeSupport {
    record Evidence(int chunkX,int chunkZ,Position placed) {}
    static Evidence verify(World world) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Snapshot fixture requires main thread");
        var loaded = world.getLoadedChunks();
        if (loaded.length == 0) throw new IllegalStateException("No loaded chunk; fixture will not force one");
        var chunk = loaded[0]; int y = world.getMaxHeight()-8;
        var originals = new LinkedHashMap<Block,BlockData>();
        ChunkSnapshot snapshot;
        try {
            var kinds = List.of(Material.COAL_ORE,Material.DEEPSLATE_COAL_ORE,Material.DEEPSLATE_IRON_ORE);
            for (int i=0;i<3;i++) {
                var ore=chunk.getBlock(4+i*4,y,4); var air=chunk.getBlock(5+i*4,y,4);
                var replaced=ore.getState();
                originals.put(ore,ore.getBlockData()); originals.put(air,air.getBlockData());
                ore.setType(kinds.get(i),false); air.setType(Material.AIR,false);
                if(i==0) {
                    var player=(Player)java.lang.reflect.Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()) {
                        case "getWorld"->world;case "getName"->"NativeFixture";case "getUniqueId"->new UUID(0,1);
                        default->throw new AssertionError("Unexpected fixture player access: "+m.getName());
                    });
                    // Synthetic event through the actual registered listener; no client/player inventory is involved.
                    Bukkit.getPluginManager().callEvent(new BlockPlaceEvent(ore,replaced,air,null,player,true,EquipmentSlot.HAND));
                }
            }
            snapshot=chunk.getChunkSnapshot(false,false,false,false);
        } finally {
            originals.forEach((block,data)->block.setBlockData(data,false));
        }
        originals.forEach((block,data)-> {
            if (!block.getBlockData().equals(data)) throw new IllegalStateException("Fixture block restoration failed");
        });
        var source=new PaperOreSource(snapshot,world.getMinHeight(),world.getMaxHeight());
        var excluded=new Position(chunk.getX()*16+8,y,chunk.getZ()*16+4);
        var scan=new OreScan(source,chunk.getX(),chunk.getZ(),y,y+1,Set.of(excluded));
        while(!scan.complete())scan.advance(17);
        var observations=scan.observations(world.getUID(),System.currentTimeMillis());
        if(observations.stream().anyMatch(o->!o.complete()) || observations.get(0).ore().size()!=1
                || observations.get(1).ore().size()!=1 || observations.stream().anyMatch(o->o.ore().contains(excluded)))
            throw new IllegalStateException("Real snapshot exposure/exclusion did not match restored fixture");
        return new Evidence(chunk.getX(),chunk.getZ(),new Position(chunk.getX()*16+4,y,chunk.getZ()*16+4));
    }
}
