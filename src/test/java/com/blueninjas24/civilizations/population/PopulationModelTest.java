package com.blueninjas24.civilizations.population;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PopulationModelTest {
    private final UUID person = UUID.randomUUID();
    private final UUID settlement = UUID.randomUUID();

    @Test
    void acceptsUnassignedHousingAndAgeWithoutChoosingCalendarUnits() {
        Resident resident = new Resident(person, "Mara", settlement, null, Long.MAX_VALUE, Resident.Status.ALIVE);
        assertNull(resident.householdId());
        assertEquals(Long.MAX_VALUE, resident.ageSteps());
        assertEquals(person, resident.id());
    }

    @Test
    void rejectsInvalidResidentIdentityNameAgeAndStatus() {
        assertThrows(NullPointerException.class,
                () -> new Resident(null, "Mara", settlement, null, 0, Resident.Status.ALIVE));
        assertThrows(NullPointerException.class,
                () -> new Resident(person, "Mara", null, null, 0, Resident.Status.ALIVE));
        assertThrows(NullPointerException.class,
                () -> new Resident(person, "Mara", settlement, null, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Resident(person, " \t", settlement, null, 0, Resident.Status.ALIVE));
        assertThrows(IllegalArgumentException.class,
                () -> new Resident(person, "Mara", settlement, null, -1, Resident.Status.ALIVE));
    }

    @Test
    void rejectsInvalidHouseholdIdentityAndName() {
        assertThrows(NullPointerException.class, () -> new Household(null, settlement, "Home"));
        assertThrows(NullPointerException.class, () -> new Household(person, null, "Home"));
        assertThrows(IllegalArgumentException.class, () -> new Household(person, settlement, " "));
    }
}
