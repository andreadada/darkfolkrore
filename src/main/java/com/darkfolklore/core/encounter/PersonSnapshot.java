package com.darkfolklore.core.encounter;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Narrative-only snapshot. It never becomes provider truth or a replacement MCA entity. */
public record PersonSnapshot(UUID personId, String displayName, Optional<UUID> killer, String homeRegion, boolean politicalFigure) {
    public PersonSnapshot {
        Objects.requireNonNull(personId);
        displayName = displayName == null || displayName.isBlank() ? "Unknown traveller" : displayName;
        killer = killer == null ? Optional.empty() : killer;
        homeRegion = homeRegion == null ? "" : homeRegion;
    }
}
