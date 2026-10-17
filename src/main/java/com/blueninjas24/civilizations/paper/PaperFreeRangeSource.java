package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.FreeRangeScan;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.*;
import org.bukkit.block.data.type.*;

/** Conservative local barrier proof. Main-thread only; unavailable chunks never become open space. */
public final class PaperFreeRangeSource implements FreeRangeScan.Source {
    private final World world;
    public PaperFreeRangeSource(World world){this.world=java.util.Objects.requireNonNull(world);}
    public FreeRangeScan.Cell cell(Position p){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Free-range proof requires main thread");
        if(!world.isChunkLoaded(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16))||p.y()-1<world.getMinHeight()||p.y()+2>=world.getMaxHeight())return FreeRangeScan.Cell.UNKNOWN;
        boolean feet=false,head=false;
        for(int y=p.y()-1;y<=p.y()+2;y++){
            var block=world.getBlockAt(p.x(),y,p.z());var data=block.getBlockData();var type=block.getType();
            if(data instanceof Fence||data instanceof Gate||data instanceof Wall||data instanceof Door||data instanceof TrapDoor||type==Material.IRON_BARS)
                return FreeRangeScan.Cell.BARRIER;
            if(y==p.y())feet=type.isSolid();if(y==p.y()+1)head=type.isSolid();
        }
        return feet&&head?FreeRangeScan.Cell.BARRIER:FreeRangeScan.Cell.CLEAR;
    }
}
