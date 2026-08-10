package com.darkfolklore.core.compat.mcacapitals;

import java.util.Objects;
import java.util.UUID;

/** Verified identity from one MCA Capitals record. */
public record CapitalIdentity(UUID capitalId, int mcaVillageId, String state) {
    public CapitalIdentity {
        Objects.requireNonNull(capitalId, "capitalId");
        Objects.requireNonNull(state, "state");
    }
}
