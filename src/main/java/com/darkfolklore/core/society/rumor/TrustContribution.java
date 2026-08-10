package com.darkfolklore.core.society.rumor;

import java.util.Objects;

public record TrustContribution(String reason, float amount) {
    public TrustContribution {
        reason = Objects.requireNonNull(reason, "reason");
        if (!Float.isFinite(amount)) throw new IllegalArgumentException("amount must be finite");
    }
}
