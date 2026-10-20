package com.blueninjas24.civilizations.construction;

import java.util.Objects;

/** Recovery of a previously committed intent; this supplies no permission, materials or work credit. */
public final class ConstructionRecovery {
    public enum Action { WAIT,WRITE,ACKNOWLEDGE,CONFLICT }
    private ConstructionRecovery(){}
    public static Action inspect(String before,String after,String observed,boolean worldAvailable,boolean chunkLoaded,
                                 boolean freshAuthority,boolean entityClear) {
        Objects.requireNonNull(before);Objects.requireNonNull(after);
        if(!worldAvailable||!chunkLoaded||!freshAuthority||!entityClear)return Action.WAIT;
        Objects.requireNonNull(observed);
        if(observed.equals(after))return Action.ACKNOWLEDGE;
        if(observed.equals(before))return Action.WRITE;
        return Action.CONFLICT;
    }
}
