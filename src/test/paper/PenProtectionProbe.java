import com.blueninjas24.civilizations.industry.PenScan;
import com.blueninjas24.civilizations.paper.PaperPenSource;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Gate;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Files;
import java.util.*;

/** Marked disposable localhost only; temporary sky pen is restored before shutdown. No animals. */
public final class PenProtectionProbe extends JavaPlugin {
    private final Map<Block,BlockData> originals=new LinkedHashMap<>();
    private boolean authorized;
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private void restore(){for(var e:originals.entrySet())e.getKey().setBlockData(e.getValue(),false);originals.clear();}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");authorized=true;
        getServer().getScheduler().runTaskLater(this,()->{try {
            var world=getServer().getWorlds().getFirst();var chunks=world.getLoadedChunks();
            // Explicit fixture setup only. The production source never calls World or loads chunks.
            var chunk=chunks.length>0?chunks[0]:world.getChunkAt(0,0);
            int x=chunk.getX()*16+8,z=chunk.getZ()*16+8,y=world.getMaxHeight()-16;
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-1;dy<=1;dy++) {
                var block=world.getBlockAt(x+dx,y+dy,z+dz);check(block.getType()==Material.AIR,"Empty test sky required");originals.put(block,block.getBlockData().clone());
            }
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)world.getBlockAt(x+dx,y-1,z+dz).setType(Material.OAK_PLANKS,false);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)==2||Math.abs(dz)==2)world.getBlockAt(x+dx,y,z+dz).setType(Material.OAK_FENCE,true);
            var entrance=world.getBlockAt(x,y,z-2);var gate=(Gate)Bukkit.createBlockData(Material.OAK_FENCE_GATE);gate.setFacing(BlockFace.NORTH);gate.setOpen(false);entrance.setBlockData(gate,true);
            check(scan(world,chunk,x,y,z).state()==PenScan.State.ENCLOSED,"Closed connected fence/gate pen required");
            gate.setOpen(true);entrance.setBlockData(gate,true);var open=scan(world,chunk,x,y,z);check(open.state()==PenScan.State.ENCLOSED&&open.interior().size()==9,"Open gate must retain nine-column structural pen");
            world.getBlockAt(x+2,y,z).setType(Material.AIR,true);check(scan(world,chunk,x,y,z).state()!=PenScan.State.ENCLOSED,"Missing fence must reject pen");
            var unknown=new PaperPenSource(world.getUID(),y,world.getMinHeight(),world.getMaxHeight(),Map.of());check(unknown.cell(new PenScan.Column(x,z))==PenScan.Cell.UNKNOWN,"Missing snapshot is unknown");
            restore();getLogger().info("PEN_PROTECTION_PROBE_PASSED closed/open gates, exact footprint, missing fence, unknown snapshot; temporary blocks restored; no animals; one chunk explicitly prepared by fixture only");
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"PEN_PROTECTION_PROBE_FAILED",failure);restore();}finally{getServer().shutdown();}},100L);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"PEN_PROTECTION_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private PenScan.Evidence scan(World world,Chunk chunk,int x,int y,int z){
        check(world.isChunkLoaded(chunk.getX(),chunk.getZ()),"Chunk must still be loaded");
        var source=new PaperPenSource(world.getUID(),y,world.getMinHeight(),world.getMaxHeight(),Map.of(new PaperPenSource.Chunk(chunk.getX(),chunk.getZ()),new PaperPenSource.Captured(world.getUID(),chunk.getChunkSnapshot())));
        var scan=new PenScan(source,new PenScan.Column(x,z),new PenScan.Limits(4,32));while(!scan.complete())scan.advance(8);return scan.evidence();
    }
    @Override public void onDisable(){restore();}
}
