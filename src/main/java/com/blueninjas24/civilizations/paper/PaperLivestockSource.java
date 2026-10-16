package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.*;
import org.bukkit.entity.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Bounded main-thread capture of one already loaded entity chunk. No loading, claims, SQL or animal mutation. */
public final class PaperLivestockSource {
    private final BooleanSupplier mainThread;
    public PaperLivestockSource(){this(Bukkit::isPrimaryThread);}
    PaperLivestockSource(BooleanSupplier mainThread){this.mainThread=Objects.requireNonNull(mainThread);}
    public LivestockObservation capture(Chunk chunk,UUID expectedWorld,long now,int entityLimit,int animalLimit) {
        if(!mainThread.getAsBoolean())throw new IllegalStateException("Livestock entity capture requires Paper main thread");
        Objects.requireNonNull(chunk);Objects.requireNonNull(expectedWorld);
        if(now<0||entityLimit<1||entityLimit>4096||animalLimit<1||animalLimit>256||animalLimit>entityLimit)throw new IllegalArgumentException("Invalid livestock capture budget");
        var world=chunk.getWorld();int x=chunk.getX(),z=chunk.getZ();
        if(!expectedWorld.equals(world.getUID()))throw new IllegalArgumentException("Livestock chunk belongs to another permanent world");
        if(!chunk.isLoaded()||!world.isChunkLoaded(x,z)||!chunk.isEntitiesLoaded())return unknown(expectedWorld,x,z,now,"Entity chunk unavailable; not loaded for observation");
        var entities=chunk.getEntities();if(entities.length>entityLimit)return unknown(expectedWorld,x,z,now,"Entity review budget exceeded; no partial roster");
        var animals=new TreeMap<UUID,LivestockObservation.Animal>();
        for(var entity:entities) {
            var type=entity.getType();if(type!=EntityType.COW&&type!=EntityType.CHICKEN)continue;
            if(!(entity instanceof Animals animal))return unknown(expectedWorld,x,z,now,"Unsupported cow/chicken entity implementation; no partial roster");
            if(!entity.isValid()||entity.isDead())continue;
            var location=entity.getLocation();
            if(location.getWorld()==null||!expectedWorld.equals(location.getWorld().getUID())||!expectedWorld.equals(entity.getWorld().getUID())
                    ||!Double.isFinite(location.getX())||!Double.isFinite(location.getY())||!Double.isFinite(location.getZ())
                    ||location.getX()<Integer.MIN_VALUE||location.getX()>=Integer.MAX_VALUE||location.getZ()<Integer.MIN_VALUE||location.getZ()>=Integer.MAX_VALUE
                    ||location.getY()<world.getMinHeight()||location.getY()>=world.getMaxHeight())return unknown(expectedWorld,x,z,now,"Inconsistent animal location; no partial roster");
            var position=new Position(location.getBlockX(),location.getBlockY(),location.getBlockZ());
            if(Math.floorDiv(position.x(),16)!=x||Math.floorDiv(position.z(),16)!=z)return unknown(expectedWorld,x,z,now,"Animal crossed observed chunk; retry fresh evidence");
            var origin=entity.getEntitySpawnReason();
            var observed=new LivestockObservation.Animal(entity.getUniqueId(),type==EntityType.COW?LivestockHerd.Species.COW:LivestockHerd.Species.CHICKEN,position,animal.getAge(),entity.getCustomName()!=null,animal.isLeashed(),LivestockBreedingProvenance.recorded(animal),origin==null?"UNKNOWN":origin.name());
            if(animals.putIfAbsent(observed.id(),observed)!=null||animals.size()>animalLimit)return unknown(expectedWorld,x,z,now,"Animal identity/review budget invalid; no partial roster");
        }
        return new LivestockObservation(expectedWorld,x,z,now,true,List.copyOf(animals.values()),"Visible cow/chicken identities, native origin and age/name/leash/protective human-origin evidence; ownership, habitat and authority require separate checks; missing tags or unknown origin do not prove natural origin");
    }
    private static LivestockObservation unknown(UUID world,int x,int z,long now,String reason){return new LivestockObservation(world,x,z,now,false,List.of(),reason);}
}
