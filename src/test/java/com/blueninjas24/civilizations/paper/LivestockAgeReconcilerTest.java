package com.blueninjas24.civilizations.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockAgeReconcilerTest {
    UUID business=new UUID(0,1);boolean adult=true,locked;int writes,ageChanges,locks;
    Map<Object,Object> values=new HashMap<>();
    @SuppressWarnings("unchecked") <T>T proxy(Class<T> type,BiFunction<String,Object[],Object> call) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->switch(m.getName()) {
            case "hashCode"->System.identityHashCode(p);case "equals"->p==a[0];case "toString"->"Age fixture "+type.getSimpleName();default->call.apply(m.getName(),a);
        });
    }
    PersistentDataContainer pdc=proxy(PersistentDataContainer.class,(m,a)->switch(m) {
        case "has"->values.containsKey(a[0])&&(a.length==1||a[1]==PersistentDataType.STRING&&values.get(a[0]) instanceof String);
        case "get"->values.get(a[0]);case "set"->{writes++;values.put(a[0],a[2]);yield null;}
        default->throw new AssertionError("Unexpected data mutation "+m);
    });
    Animals animal=proxy(Animals.class,(m,a)->switch(m) {
        case "getPersistentDataContainer"->pdc;case "getAgeLock"->locked;case "setAgeLock"->{assertEquals(true,a[0]);locked=true;locks++;yield null;}
        case "isAdult"->adult;case "setBaby"->{adult=false;ageChanges++;yield null;}case "setAdult"->{adult=true;ageChanges++;yield null;}
        default->throw new AssertionError("No name/leash/AI/gravity/invulnerability mutation allowed: "+m);
    });
    String clock(){return (String)values.get(LivestockAgeReconciler.CLOCK);}
    @Test void initiallyYoungAnimalUsesSavedRemainingPeriodsAndAnOlderViewCannotUndoMaturity() {
        assertTrue(LivestockAgeReconciler.apply(animal,business,0,2));assertFalse(adult);assertTrue(locked);
        LivestockAgeReconciler.apply(animal,business,1,2);assertFalse(adult);LivestockAgeReconciler.apply(animal,business,2,2);assertTrue(adult);
        var saved=clock();int changed=ageChanges;assertFalse(LivestockAgeReconciler.apply(animal,business,1,2));assertTrue(adult);assertEquals(saved,clock());assertEquals(changed,ageChanges);
    }
    @Test void savedInitialAdultLocksWithoutAlteringAnyOtherAnimalProperties() {
        adult=false;assertTrue(LivestockAgeReconciler.apply(animal,business,0,0));assertTrue(adult);assertTrue(locked);assertEquals("v1|"+business+"|0|0",clock());assertEquals(1,ageChanges);
    }
    @Test void identicalPeriodDoesNotRewriteClockAndCanRepairVanillaAgeOrLockDrift() {
        LivestockAgeReconciler.apply(animal,business,1,2);int savedWrites=writes;assertTrue(LivestockAgeReconciler.apply(animal,business,1,2));assertEquals(savedWrites,writes);assertEquals(1,locks);
        adult=true;locked=false;LivestockAgeReconciler.apply(animal,business,1,2);assertFalse(adult);assertTrue(locked);assertEquals(savedWrites,writes);
    }
    @Test void otherBusinessOrChangedMaturityNeverOverwritesItsScopeOrNativeAge() {
        LivestockAgeReconciler.apply(animal,business,1,2);var saved=clock();int previous=writes;
        assertThrows(IllegalStateException.class,()->LivestockAgeReconciler.apply(animal,UUID.randomUUID(),2,2));
        assertThrows(IllegalStateException.class,()->LivestockAgeReconciler.apply(animal,business,2,3));assertEquals(saved,clock());assertFalse(adult);assertEquals(previous,writes);
    }
    @Test void wrongTypesMalformedNoncanonicalAndUnknownVersionsAreRetainedForReview() {
        for(Object raw:List.of((byte)1,"garbage","v2|"+business+"|2|1","v1|"+business+"|02|1","v1|"+business+"|2|+1","v1|"+business+"|2|-1","v1|"+business+"|2|9223372036854775808","v1|0-0-0-0-1|2|1")) {
            values.put(LivestockAgeReconciler.CLOCK,raw);assertThrows(IllegalStateException.class,()->LivestockAgeReconciler.apply(animal,business,1,2));assertEquals(raw,values.get(LivestockAgeReconciler.CLOCK));
        }
        assertEquals(0,writes);assertEquals(0,ageChanges);assertEquals(0,locks);
    }
    @Test void negativeSavedCountersRefuseBeforeReadingOrChangingNativeData() {
        assertThrows(IllegalArgumentException.class,()->LivestockAgeReconciler.apply(animal,business,-1,2));assertThrows(IllegalArgumentException.class,()->LivestockAgeReconciler.apply(animal,business,1,-2));assertTrue(values.isEmpty());assertEquals(0,ageChanges);
    }
    @Test void maximumCountersStayCanonicalAndNeverWrapAround() {
        adult=false;LivestockAgeReconciler.apply(animal,business,Long.MAX_VALUE,Long.MAX_VALUE);assertTrue(adult);var saved=clock();
        assertFalse(LivestockAgeReconciler.apply(animal,business,Long.MAX_VALUE-1,Long.MAX_VALUE));assertEquals(saved,clock());assertTrue(adult);
    }
}
