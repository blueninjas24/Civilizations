package com.blueninjas24.civilizations.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
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
            case "getType"->type;case "getPersistentDataContainer"->pdc;default->throw new AssertionError("Unexpected mutation/identity access: "+m.getName());});
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
}
