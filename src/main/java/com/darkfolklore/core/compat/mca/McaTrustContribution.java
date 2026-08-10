package com.darkfolklore.core.compat.mca;

import java.util.Objects;

/** One administrator-readable reason for an MCA trust adjustment. */
public record McaTrustContribution(String reason, double amount) {
    public McaTrustContribution {
        Objects.requireNonNull(reason, "reason");
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("amount must be finite");
    }
}
