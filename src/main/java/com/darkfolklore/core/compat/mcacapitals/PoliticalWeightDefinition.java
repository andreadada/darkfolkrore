package com.darkfolklore.core.compat.mcacapitals;

import java.util.Objects;

public record PoliticalWeightDefinition(PoliticalRole role, PoliticalWeights weights) {
    public PoliticalWeightDefinition {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(weights, "weights");
        if (weights.credibility() < 0 || weights.credibility() > 1
                || weights.organizationResponse() < 0 || weights.organizationResponse() > 1
                || weights.investigationPriority() < 0 || weights.investigationPriority() > 1
                || weights.publicAwareness() < 0 || weights.publicAwareness() > 1) {
            throw new IllegalArgumentException("political weights must be 0..1");
        }
    }
}
