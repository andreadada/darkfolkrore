package com.darkfolklore.core.contracts;

import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent assignment metadata around a {@link MonsterContract}.
 *
 * 0.3.1 keeps the narrative incident, factual culprit and observed provider
 * implementation separate from the canonical target concept.  The optional
 * fields deliberately default empty so schema-2 contracts remain readable.
 */
public record ContractAssignment(
        UUID player,
        MonsterContract contract,
        WorldPosition investigationCenter,
        String villageKey,
        int requiredDistinctClues,
        Optional<UUID> storyId,
        Optional<UUID> culpritId,
        String observedImplementation,
        boolean culpritFallbackAllowed,
        boolean issuerFallbackAllowed
) {
    public ContractAssignment {
        Objects.requireNonNull(player);
        Objects.requireNonNull(contract);
        Objects.requireNonNull(investigationCenter);
        villageKey = Objects.requireNonNullElse(villageKey, "");
        requiredDistinctClues = Math.max(1, Math.min(8, requiredDistinctClues));
        storyId = storyId == null ? Optional.empty() : storyId;
        culpritId = culpritId == null ? Optional.empty() : culpritId;
        observedImplementation = Objects.requireNonNullElse(observedImplementation, "");
        if (!observedImplementation.isBlank() && !observedImplementation.contains(":")) {
            throw new IllegalArgumentException("observedImplementation must be namespaced or blank");
        }
    }

    /** Backwards-compatible constructor used by legacy tests and schema-2 callers. */
    public ContractAssignment(UUID player, MonsterContract contract, WorldPosition investigationCenter,
                              String villageKey, int requiredDistinctClues) {
        this(player, contract, investigationCenter, villageKey, requiredDistinctClues,
                Optional.empty(), Optional.empty(), "", false, false);
    }

    public ContractAssignment withCulpritFallbackAllowed() {
        if (culpritFallbackAllowed) return this;
        return new ContractAssignment(player, contract, investigationCenter, villageKey, requiredDistinctClues,
                storyId, culpritId, observedImplementation, true, issuerFallbackAllowed);
    }

    public ContractAssignment withIssuerFallbackAllowed() {
        if (issuerFallbackAllowed) return this;
        return new ContractAssignment(player, contract, investigationCenter, villageKey, requiredDistinctClues,
                storyId, culpritId, observedImplementation, culpritFallbackAllowed, true);
    }
}
