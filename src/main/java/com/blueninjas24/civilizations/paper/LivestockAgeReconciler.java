package com.blueninjas24.civilizations.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Shared saved-age authority for known herd UUIDs. Never infer ownership from these tags. */
final class LivestockAgeReconciler {
    static final NamespacedKey CLOCK=Objects.requireNonNull(NamespacedKey.fromString("civilizations:livestock_age_clock"));
    private record Clock(UUID business,long mature,long period) {
        Clock {Objects.requireNonNull(business);if(mature<0||period<0)throw new IllegalArgumentException("Invalid livestock age clock");}
        String encode(){return "v1|"+business+"|"+mature+"|"+period;}
        static Clock decode(String raw) {
            try {
                if(raw==null||raw.length()>160)throw new IllegalArgumentException();var parts=raw.split("\\|",-1);
                if(parts.length!=4||!parts[0].equals("v1"))throw new IllegalArgumentException();
                var clock=new Clock(UUID.fromString(parts[1]),Long.parseLong(parts[2]),Long.parseLong(parts[3]));
                if(!clock.encode().equals(raw))throw new IllegalArgumentException();return clock;
            }catch(IllegalArgumentException invalid){throw new IllegalStateException("Invalid saved native livestock age clock; review required",invalid);}
        }
    }
    /** False means an older in-flight view was ignored. No headcount, goods, movement, name,
     * leash or invulnerability changes. Scope/maturity contradictions are not overwritten. */
    static boolean apply(Animals animal,UUID business,long period,long mature) {
        Objects.requireNonNull(animal);var current=new Clock(business,mature,period);var data=animal.getPersistentDataContainer();Clock before=null;
        if(data.has(CLOCK)) {
            if(!data.has(CLOCK,PersistentDataType.STRING))throw new IllegalStateException("Native livestock age clock has another type; review required");
            before=Clock.decode(data.get(CLOCK,PersistentDataType.STRING));
            if(!before.business().equals(business)||before.mature()!=mature)throw new IllegalStateException("Native livestock age scope/maturity differs from saved herd; review required");
            if(before.period()>period)return false;
        }
        if(!animal.getAgeLock())animal.setAgeLock(true);
        if(period>=mature){if(!animal.isAdult())animal.setAdult();}else if(animal.isAdult())animal.setBaby();
        if(!current.equals(before))data.set(CLOCK,PersistentDataType.STRING,current.encode());return true;
    }
}
