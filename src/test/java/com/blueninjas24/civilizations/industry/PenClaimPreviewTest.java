package com.blueninjas24.civilizations.industry;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PenClaimPreviewTest {
    final UUID actor = new UUID(0, 1), world = new UUID(0, 2), animal = new UUID(0, 3);
    final PenScan.Evidence pen = new PenScan.Evidence(PenScan.State.ENCLOSED,
            Set.of(new PenScan.Column(0, 0)), Set.of(new PenScan.Column(1, 0)));
    PenClaimPreview preview() { return new PenClaimPreview(actor, world, 64, 1000, pen, Set.of(animal)); }
    @Test void sameAuthorizedActorCanConfirmExactFreshFootprintAndAnimals() {
        assertEquals(PenClaimPreview.Result.READY, preview().confirm(actor, world, 64, 999, true, pen, Set.of(animal)));
    }
    @Test void anotherPlayerOrWorldCannotConfirmThePreview() {
        assertEquals(PenClaimPreview.Result.WRONG_ACTOR, preview().confirm(world, world, 64, 900, true, pen, Set.of(animal)));
        assertEquals(PenClaimPreview.Result.WRONG_WORLD_OR_LEVEL, preview().confirm(actor, actor, 64, 900, true, pen, Set.of(animal)));
        assertEquals(PenClaimPreview.Result.WRONG_WORLD_OR_LEVEL, preview().confirm(actor, world, 65, 900, true, pen, Set.of(animal)));
    }
    @Test void expiredOrRevokedPermissionNeedsANewPreview() {
        assertEquals(PenClaimPreview.Result.EXPIRED, preview().confirm(actor, world, 64, 1000, true, pen, Set.of(animal)));
        assertEquals(PenClaimPreview.Result.NOT_AUTHORIZED, preview().confirm(actor, world, 64, 900, false, pen, Set.of(animal)));
    }
    @Test void brokenFenceOrDifferentAnimalsCannotUseOldConfirmation() {
        var unknown = new PenScan.Evidence(PenScan.State.UNKNOWN, Set.of(), Set.of());
        assertEquals(PenClaimPreview.Result.EVIDENCE_CHANGED, preview().confirm(actor, world, 64, 900, true, unknown, Set.of(animal)));
        assertEquals(PenClaimPreview.Result.EVIDENCE_CHANGED, preview().confirm(actor, world, 64, 900, true, pen, Set.of()));
    }
    @Test void unknownGeometryCannotBePreviewedAndMembershipIsImmutable() {
        assertThrows(IllegalArgumentException.class, () -> new PenClaimPreview(actor, world, 64, 1000,
                new PenScan.Evidence(PenScan.State.UNKNOWN, Set.of(), Set.of()), Set.of()));
        var animals = new HashSet<>(Set.of(animal));
        var preview = new PenClaimPreview(actor, world, 64, 1000, pen, animals);
        animals.clear(); assertEquals(Set.of(animal), preview.animals());
    }
}
