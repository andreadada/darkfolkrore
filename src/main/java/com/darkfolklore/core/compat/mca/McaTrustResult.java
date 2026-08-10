package com.darkfolklore.core.compat.mca;

import java.util.List;

/** Aggregate relationship modifier plus its bounded diagnostic explanation. */
public record McaTrustResult(double modifier, List<McaTrustContribution> contributions) {
    public McaTrustResult {
        if (!Double.isFinite(modifier)) throw new IllegalArgumentException("modifier must be finite");
        contributions = List.copyOf(contributions);
    }
}
