package com.blueninjas24.civilizations.industry;

import org.junit.jupiter.api.Test;
import static com.blueninjas24.civilizations.industry.LivestockAdoption.*;
import static org.junit.jupiter.api.Assertions.*;

class LivestockAdoptionTest {
    Evidence evidence(Ownership owner, Habitat habitat, boolean complete, long duration) {
        return new Evidence(owner, habitat, false, false, false, complete, duration);
    }
    @Test void freeRangeAnimalsNeedContinuousReliableObservation() {
        assertEquals(Decision.WAIT, assess(evidence(Ownership.UNCLAIMED, Habitat.FREE_RANGE, true, 999), 1000));
        assertEquals(Decision.ADOPT, assess(evidence(Ownership.UNCLAIMED, Habitat.FREE_RANGE, true, 1000), 1000));
        assertEquals(Decision.WAIT, assess(evidence(Ownership.UNCLAIMED, Habitat.FREE_RANGE, false, 1000), 1000));
    }
    @Test void unknownExistingPensAndUnloadedEvidenceStayProtected() {
        assertEquals(Decision.PROTECTED, assess(evidence(Ownership.UNCLAIMED, Habitat.UNKNOWN_PEN, true, 1000), 1000));
        assertEquals(Decision.PROTECTED, assess(evidence(Ownership.UNCLAIMED, Habitat.PLAYER_PEN, true, 1000), 1000));
        assertEquals(Decision.WAIT, assess(evidence(Ownership.UNCLAIMED, Habitat.UNKNOWN, true, 1000), 1000));
    }
    @Test void permanentOwnershipSurvivesEscapeAndBrokenSign() {
        assertEquals(Decision.PROTECTED, assess(evidence(Ownership.PLAYER, Habitat.FREE_RANGE, true, 1000), 1000));
        assertEquals(Decision.ALREADY_VILLAGE, assess(evidence(Ownership.VILLAGE, Habitat.UNKNOWN, false, 0), 1000));
    }
    @Test void villagePenIsBulkConsentButCannotOverridePlayerAnimalOwnership() {
        assertEquals(Decision.ADOPT, assess(evidence(Ownership.UNCLAIMED, Habitat.VILLAGE_PEN, true, 0), 1000));
        assertEquals(Decision.PROTECTED, assess(evidence(Ownership.PLAYER, Habitat.VILLAGE_PEN, true, 1000), 1000));
    }
    @Test void namesLeashesAndRecordedPlayerBreedingBlockAutomaticAdoption() {
        for (int flag = 0; flag < 3; flag++) assertEquals(Decision.PROTECTED,
                assess(new Evidence(Ownership.UNCLAIMED, Habitat.VILLAGE_PEN, flag == 0, flag == 1, flag == 2, true, 1000), 1000));
    }
}
