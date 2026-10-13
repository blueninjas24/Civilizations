package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Editable read-only budgets and recognition; no founding or building rights. */
public record WorkplaceObserverPolicy(boolean enabled,int verticalRadius,int cellsPerTick,int intervalTicks,int horizontalRadiusChunks,Set<WorkplaceObservation.Kind> kinds) {
    public WorkplaceObserverPolicy {
        kinds=Set.copyOf(kinds);
        if(verticalRadius<1||verticalRadius>64||cellsPerTick<1||cellsPerTick>4096||intervalTicks<20||intervalTicks>72000||horizontalRadiusChunks<0||horizontalRadiusChunks>2||kinds.isEmpty())throw new IllegalArgumentException("Invalid workplace observation settings");
    }
    public static WorkplaceObserverPolicy prototype(){return new WorkplaceObserverPolicy(true,16,512,400,1,Set.of(WorkplaceObservation.Kind.values()));}
    public static WorkplaceObserverPolicy disabled(){var p=prototype();return new WorkplaceObserverPolicy(false,p.verticalRadius,p.cellsPerTick,p.intervalTicks,p.horizontalRadiusChunks,p.kinds);}
}
