package com.blueninjas24.civilizations.population;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Structured explanation; names may change, stable identities do not. */
public record PopulationEvent(long step, Type type, UUID residentId, UUID settlementId,
                              List<UUID> relatedResidents, String reason, UUID sourceSettlementId) {
    public enum Type { BIRTH, DEATH, MIGRATION, ARRIVAL }

    public PopulationEvent(long step, Type type, UUID residentId, UUID settlementId,
                           List<UUID> relatedResidents, String reason) {
        this(step, type, residentId, settlementId, relatedResidents, reason, null);
    }

    public PopulationEvent {
        if (step <= 0) throw new IllegalArgumentException("Event step must be positive");
        Objects.requireNonNull(type);
        Objects.requireNonNull(residentId);
        Objects.requireNonNull(settlementId);
        relatedResidents = List.copyOf(relatedResidents);
        Objects.requireNonNull(reason);
        if (reason.isBlank()) throw new IllegalArgumentException("Event requires a reason");
        if ((type != Type.BIRTH && !relatedResidents.isEmpty())
                || (type == Type.BIRTH && (relatedResidents.size() != 2
                || relatedResidents.get(0).equals(relatedResidents.get(1))
                || relatedResidents.contains(residentId)))) {
            throw new IllegalArgumentException("Invalid demographic event participants");
        }
        if ((type == Type.MIGRATION && (sourceSettlementId == null || sourceSettlementId.equals(settlementId)))
                || (type != Type.MIGRATION && sourceSettlementId != null)) {
            throw new IllegalArgumentException("Invalid migration origin");
        }
    }
}
