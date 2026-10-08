package com.blueninjas24.civilizations.population;

import java.util.Objects;
import java.util.UUID;

/** Parent points to child; partners have one canonical, unordered representation. */
public record FamilyTie(UUID first, UUID second, Type type) {
    public enum Type { PARENT_CHILD, PARTNER }

    public FamilyTie {
        Objects.requireNonNull(first, "first resident");
        Objects.requireNonNull(second, "second resident");
        Objects.requireNonNull(type, "relationship type");
        if (first.equals(second)) {
            throw new IllegalArgumentException("A resident cannot be their own relative");
        }
        if (type == Type.PARTNER && first.toString().compareTo(second.toString()) > 0) {
            UUID swap = first;
            first = second;
            second = swap;
        }
    }
}
