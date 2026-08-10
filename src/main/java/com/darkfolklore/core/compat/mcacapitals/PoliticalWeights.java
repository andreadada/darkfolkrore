package com.darkfolklore.core.compat.mcacapitals;

/** Political consequences applied only after an actor has obtained actual social knowledge. */
public record PoliticalWeights(
        double credibility,
        double organizationResponse,
        double investigationPriority,
        double publicAwareness
) {
    public PoliticalWeights {
        requireFinite(credibility, "credibility");
        requireFinite(organizationResponse, "organizationResponse");
        requireFinite(investigationPriority, "investigationPriority");
        requireFinite(publicAwareness, "publicAwareness");
    }

    public static final PoliticalWeights NONE = new PoliticalWeights(0, 0, 0, 0);

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
