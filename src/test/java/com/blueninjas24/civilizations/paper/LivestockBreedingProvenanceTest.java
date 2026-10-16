package com.blueninjas24.civilizations.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockBreedingProvenanceTest {
    final NamespacedKey key=new NamespacedKey("civilizations","livestock_player_bred");
    record Actor(LivingEntity entity,Map<NamespacedKey,Object> data) {}
    Actor actor(EntityType type) {
        var data=new HashMap<NamespacedKey,Object>();
        var pdc=(PersistentDataContainer)Proxy.newProxyInstance(PersistentDataContainer.class.getClassLoader(),new Class<?>[]{PersistentDataContainer.class},(p,m,a)->switch(m.getName()) {
            case "has"->data.containsKey(a[0]);case "set"->{assertEquals(PersistentDataType.BYTE,a[1]);data.put((NamespacedKey)a[0],a[2]);yield null;}default->throw new AssertionError("Unexpected PDC operation: "+m.getName());});
        var entity=(LivingEntity)Proxy.newProxyInstance(LivingEntity.class.getClassLoader(),new Class<?>[]{LivingEntity.class},(p,m,a)->switch(m.getName()){
            case "getType"->type;case "getPersistentDataContainer"->pdc;case "getEntitySpawnReason"->null;default->throw new AssertionError("Unexpected mutation/identity access: "+m.getName());});
        return new Actor(entity,data);
    }
    Player player(){return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->{throw new AssertionError("Player identity/inventory must not be read: "+m.getName());});}
    EntityBreedEvent birth(Actor child,Actor mother,Actor father,LivingEntity breeder){return new EntityBreedEvent(child.entity(),mother.entity(),father.entity(),breeder,null,3);}
    @Test void playerBredCowAndChickenGetOnlyAnonymousPersistentProtection() {
        for(var type:List.of(EntityType.COW,EntityType.CHICKEN)) {
            var child=actor(type);var mother=actor(type);var father=actor(type);var e=birth(child,mother,father,player());
            new LivestockBreedingProvenance().bred(e);assertEquals(Map.of(key,(byte)1),child.data());assertTrue(mother.data().isEmpty());assertTrue(father.data().isEmpty());assertFalse(e.isCancelled());assertEquals(3,e.getExperience());
        }
    }
    @Test void protectedParentProvenanceContinuesWithoutInventingAnOwnerOrOverwritingOldEvidence() {
        for(boolean maternal:List.of(true,false)) {
            var mother=actor(EntityType.COW);var father=actor(EntityType.COW);(maternal?mother:father).data().put(key,"old protective value");var child=actor(EntityType.COW);
            var e=birth(child,mother,father,null);new LivestockBreedingProvenance().bred(e);assertTrue(LivestockBreedingProvenance.recorded(child.entity()));
            child.data().put(key,"retain historic protection");new LivestockBreedingProvenance().bred(e);assertEquals("retain historic protection",child.data().get(key));
        }
    }
    @Test void unrecordedNaturalBreedingRemainsUnclaimedAndMissingTagIsNotNaturalOriginProof() {
        var child=actor(EntityType.CHICKEN);var e=birth(child,actor(EntityType.CHICKEN),actor(EntityType.CHICKEN),null);new LivestockBreedingProvenance().bred(e);
        assertTrue(child.data().isEmpty());assertFalse(LivestockBreedingProvenance.recorded(child.entity()));assertFalse(e.isCancelled());
    }
    @Test void cancelledAndUnsupportedBirthsDoNotWriteEvidenceOrChangeNormalOutcome() {
        var child=actor(EntityType.COW);var e=birth(child,actor(EntityType.COW),actor(EntityType.COW),player());e.setCancelled(true);new LivestockBreedingProvenance().bred(e);assertTrue(child.data().isEmpty());assertTrue(e.isCancelled());
        var pig=actor(EntityType.PIG);var other=birth(pig,actor(EntityType.PIG),actor(EntityType.PIG),player());new LivestockBreedingProvenance().bred(other);assertTrue(pig.data().isEmpty());assertFalse(other.isCancelled());
    }
    @Test void humanCausedHatchingSpawningAndBreedingMarkOnlySupportedAnimalsWithoutChangingSpawns() {
        for(var reason:List.of(CreatureSpawnEvent.SpawnReason.EGG,CreatureSpawnEvent.SpawnReason.BREEDING,CreatureSpawnEvent.SpawnReason.SPAWNER_EGG,CreatureSpawnEvent.SpawnReason.DISPENSE_EGG,CreatureSpawnEvent.SpawnReason.COMMAND))for(var type:List.of(EntityType.COW,EntityType.CHICKEN)) {
            var actor=actor(type);var e=new CreatureSpawnEvent(actor.entity(),reason);new LivestockBreedingProvenance().spawned(e);assertFalse(e.isCancelled());assertTrue(LivestockBreedingProvenance.recorded(actor.entity()));assertEquals(1,actor.data().size());
            var before=Map.copyOf(actor.data());new LivestockBreedingProvenance().spawned(e);assertEquals(before,actor.data());
        }
    }
    @Test void cancelledUnsupportedNaturalAndUncertainSpawnsAreNotInventedAsPlayerOwned() {
        var listener=new LivestockBreedingProvenance();var cancelled=actor(EntityType.CHICKEN);var e=new CreatureSpawnEvent(cancelled.entity(),CreatureSpawnEvent.SpawnReason.EGG);e.setCancelled(true);listener.spawned(e);assertTrue(cancelled.data().isEmpty());assertTrue(e.isCancelled());
        for(var reason:List.of(CreatureSpawnEvent.SpawnReason.NATURAL,CreatureSpawnEvent.SpawnReason.CUSTOM,CreatureSpawnEvent.SpawnReason.DEFAULT)) {
            var animal=actor(EntityType.COW);var event=new CreatureSpawnEvent(animal.entity(),reason);listener.spawned(event);assertTrue(animal.data().isEmpty());assertFalse(event.isCancelled());
        }
        var pig=actor(EntityType.PIG);listener.spawned(new CreatureSpawnEvent(pig.entity(),CreatureSpawnEvent.SpawnReason.COMMAND));assertTrue(pig.data().isEmpty());
    }
    @Test void oldOrMalformedHumanOriginEvidenceStaysProtectiveAndIsNotOverwritten() {
        var animal=actor(EntityType.CHICKEN);var origin=new NamespacedKey("civilizations","livestock_human_origin");animal.data().put(origin,"legacy value");
        new LivestockBreedingProvenance().spawned(new CreatureSpawnEvent(animal.entity(),CreatureSpawnEvent.SpawnReason.EGG));assertEquals("legacy value",animal.data().get(origin));assertTrue(LivestockBreedingProvenance.recorded(animal.entity()));
    }
}
