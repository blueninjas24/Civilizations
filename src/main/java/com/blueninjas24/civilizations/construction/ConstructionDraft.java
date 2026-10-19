package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.util.*;

/** Frozen proposal only: no ownership, permission, reserved stock, work credit or completed building. */
public record ConstructionDraft(UUID id,UUID settlement,UUID world,Position origin,Transform transform,
                                ConstructionTemplate template,long createdMillis) {
    public ConstructionDraft {
        Objects.requireNonNull(id);Objects.requireNonNull(settlement);Objects.requireNonNull(world);Objects.requireNonNull(origin);
        Objects.requireNonNull(transform);Objects.requireNonNull(template);if(createdMillis<0)throw new IllegalArgumentException("Negative draft creation time");
        ConstructionPreview.footprint(template,transform,origin);ConstructionPreview.requiredCells(template,transform,origin);
    }
}
