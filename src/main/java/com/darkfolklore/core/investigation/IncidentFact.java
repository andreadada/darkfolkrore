package com.darkfolklore.core.investigation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Factual, non-player-facing metadata captured when an investigation incident is created. */
public record IncidentFact(Optional<UUID> culpritId, String observedImplementation, long createdAt) {
    public IncidentFact {
        culpritId = culpritId == null ? Optional.empty() : culpritId;
        observedImplementation = Objects.requireNonNullElse(observedImplementation, "");
        if (!observedImplementation.isBlank() && !observedImplementation.contains(":")) {
            throw new IllegalArgumentException("observedImplementation must be namespaced or blank");
        }
        createdAt = Math.max(0L, createdAt);
    }
}
