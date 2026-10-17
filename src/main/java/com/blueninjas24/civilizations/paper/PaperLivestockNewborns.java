package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import com.blueninjas24.civilizations.industry.LivestockHerd;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.logging.*;

/** Loaded, nearby projections of paid saved births. No adoption, replacement, block changes or chunk loads. */
public final class PaperLivestockNewborns extends BukkitRunnable implements AutoCloseable {
    static final NamespacedKey BIRTH=Objects.requireNonNull(NamespacedKey.fromString("civilizations:livestock_birth"));
    static final NamespacedKey PENDING=Objects.requireNonNull(NamespacedKey.fromString("civilizations:livestock_birth_pending"));
    private final LivestockProjectionCycle cycle;private final Logger logger;
    private Animals prepared;
    public PaperLivestockNewborns(PopulationSession session,int nearbyRadius,BooleanSupplier enabled,Logger logger) {
        this(session,enabled,location->location.getWorld().getPlayers().stream().anyMatch(p->p.getLocation().distanceSquared(location)<=(double)nearbyRadius*nearbyRadius),logger);
        if(nearbyRadius<8||nearbyRadius>128)throw new IllegalArgumentException("Invalid newborn nearby radius");
    }
    /** Isolated fixture can provide observer positions without fake players; placement checks still apply. */
    public PaperLivestockNewborns(PopulationSession session,BooleanSupplier enabled,Predicate<Location> nearby,Logger logger) {
        this.logger=Objects.requireNonNull(logger);Objects.requireNonNull(enabled);Objects.requireNonNull(nearby);
        cycle=new LivestockProjectionCycle(new LivestockProjectionCycle.Owner() {
            public boolean ready(){return session.status().ready();}
            public CompletableFuture<List<Job>> page(UUID after,int limit){return session.livestockBirthJobs(after,limit);}
            public CompletableFuture<Projection> claim(Claim claim){return session.claimLivestockBirth(claim);}
            public CompletableFuture<Boolean> activate(UUID birth,UUID entity){return session.activateLivestockBirth(birth,entity);}
            public CompletableFuture<Boolean> failedUnspawned(UUID birth,UUID entity){return session.failUnspawnedLivestockBirth(birth,entity);}
        },new LivestockProjectionCycle.Bodies() {
            public boolean existing(Job job){return body(job,job.projection().orElseThrow().claim().entity()).isPresent();}
            public void apply(Job job){body(job,job.projection().orElseThrow().claim().entity()).ifPresent(animal->applyAge(animal,job));}
            public void release(Job job,UUID entity) {
                body(job,entity).ifPresent(animal->{
                    applyAge(animal,job);var pending=animal.getPersistentDataContainer().get(PENDING,PersistentDataType.STRING);
                    if(pending!=null) {
                        if(!pending.equals(job.birth().id().toString()))throw new IllegalStateException("Pending livestock birth tag conflicts with saved identity");
                        animal.setAI(true);animal.setGravity(true);animal.getPersistentDataContainer().remove(PENDING);
                    }
                });
            }
            public Optional<Claim> prepare(Job job) {
                if(!enabled.getAsBoolean()||!session.livestockCaptureReady())return Optional.empty();var world=Bukkit.getWorld(job.world());if(world==null)return Optional.empty();
                var location=safeLocation(world,job.home());if(location==null||!nearby.test(location))return Optional.empty();
                prepared=job.species()==LivestockHerd.Species.COW?world.createEntity(location,Cow.class):world.createEntity(location,Chicken.class);
                applyAge(prepared,job);prepared.setPersistent(true);prepared.setAI(false);prepared.setGravity(false);
                prepared.getPersistentDataContainer().set(PENDING,PersistentDataType.STRING,job.birth().id().toString());
                if(prepared.isValid())throw new IllegalStateException("Prepared livestock already entered world before claim");
                return Optional.of(new Claim(job.birth().id(),prepared.getUniqueId(),job.world(),job.species(),location.getBlockX(),location.getBlockY(),location.getBlockZ(),System.currentTimeMillis()));
            }
            public LivestockProjectionCycle.Added add(Job job,Claim claim) {
                if(prepared==null||!prepared.getUniqueId().equals(claim.entity()))return LivestockProjectionCycle.Added.UNCERTAIN;
                if(prepared.isValid())return prepared.isDead()?LivestockProjectionCycle.Added.UNCERTAIN:LivestockProjectionCycle.Added.ADDED;
                var world=Bukkit.getWorld(claim.world());var location=prepared.getLocation();
                if(world==null||!enabled.getAsBoolean()||location.getWorld()==null||!world.getUID().equals(location.getWorld().getUID())
                        ||!clear(location)||!nearby.test(location))return LivestockProjectionCycle.Added.NEVER_ATTEMPTED;
                world.addEntity(prepared);
                return prepared.isValid()&&!prepared.isDead()?LivestockProjectionCycle.Added.ADDED:LivestockProjectionCycle.Added.UNCERTAIN;
            }
            public void forget(Claim claim){if(prepared!=null&&prepared.getUniqueId().equals(claim.entity()))prepared=null;}
        },System::currentTimeMillis);
    }
    private static Optional<Animals> body(Job job,UUID id) {
        var entity=Bukkit.getEntity(id);if(entity==null||!entity.isValid()||entity.isDead())return Optional.empty();
        if(!(entity instanceof Animals animal)||(job.species()==LivestockHerd.Species.COW?entity.getType()!=EntityType.COW:entity.getType()!=EntityType.CHICKEN))
            throw new IllegalStateException("Saved livestock body has another species");
        var tag=animal.getPersistentDataContainer().get(BIRTH,PersistentDataType.STRING);
        if(tag!=null&&!tag.equals(job.birth().id().toString()))throw new IllegalStateException("Livestock body tag conflicts with saved birth");
        return Optional.of(animal);
    }
    static void applyAge(Animals animal,Job job) {
        animal.setAgeLock(true);animal.getPersistentDataContainer().set(BIRTH,PersistentDataType.STRING,job.birth().id().toString());
        if(job.herdPeriod()>=job.birth().maturePeriod()){if(!animal.isAdult())animal.setAdult();}
        else if(animal.isAdult())animal.setBaby();
    }
    static Location safeLocation(World world,Position home) {
        return safeLocation(world,home,Material::isOccluding,Material::isAir);
    }
    /** Native registry predicates remain in production; fixtures supply their immutable block evidence. */
    static Location safeLocation(World world,Position home,Predicate<Material> support,Predicate<Material> air) {
        for(int dy:new int[]{0,1,-1})for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            long x=(long)home.x()+dx,z=(long)home.z()+dz,y=(long)home.y()+dy;
            if(x<Integer.MIN_VALUE||x>Integer.MAX_VALUE||z<Integer.MIN_VALUE||z>Integer.MAX_VALUE||y<Integer.MIN_VALUE||y>Integer.MAX_VALUE)continue;
            var location=new Location(world,x+0.5,y,z+0.5);if(clear(location,support,air))return location;
        }return null;
    }
    static boolean clear(Location location) {
        return clear(location,Material::isOccluding,Material::isAir);
    }
    static boolean clear(Location location,Predicate<Material> support,Predicate<Material> air) {
        var world=location.getWorld();int x=location.getBlockX(),y=location.getBlockY(),z=location.getBlockZ();
        if(world==null||y<=world.getMinHeight()||y+1L>=world.getMaxHeight()
                ||!world.isChunkLoaded(Math.floorDiv(x,16),Math.floorDiv(z,16))
                ||!world.getChunkAt(Math.floorDiv(x,16),Math.floorDiv(z,16)).isEntitiesLoaded()
                ||!world.getWorldBorder().isInside(location))return false;
        var floor=world.getBlockAt(x,y-1,z).getType();
        if(!support.test(floor)||floor==Material.MAGMA_BLOCK||floor==Material.CACTUS
                ||!air.test(world.getBlockAt(x,y,z).getType())||!air.test(world.getBlockAt(x,y+1,z).getType()))return false;
        return world.getNearbyEntities(new org.bukkit.util.BoundingBox(x+0.1,y,z+0.1,x+0.9,y+1.5,z+0.9),e->e instanceof LivingEntity&&!e.isDead()).isEmpty();
    }
    @Override public void run(){
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Newborn projection requires Paper main thread");
        if(prepared!=null&&Bukkit.getWorld(prepared.getWorld().getUID())==null) {
            try{cycle.abandonUnattempted();}finally{prepared=null;} // Never retain a native handle to an unloaded world.
        }
        cycle.tick();
    }
    public String diagnostic(){return cycle.diagnostic();}
    @Override public void close(){cycle.close();cycle.shutdown().whenComplete((accepted,failure)->{if(failure!=null)logger.log(Level.SEVERE,"Unspawned newborn shutdown claim save failed; saved claim retained",failure);});prepared=null;}
}
