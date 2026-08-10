package com.darkfolklore.core.compat.mca;

/** Configurable trust contributions for relationships that MCA 7.7.32 can actually verify. */
public record McaTrustSettings(
        double self,
        double spouse,
        double parentOrChild,
        double sibling,
        double playerFriend,
        double playerBountyTarget
) {
    public static final McaTrustSettings DEFAULTS = new McaTrustSettings(0.30, 0.25, 0.22, 0.18, 0.12, -0.20);

    public McaTrustSettings {
        requireFinite(self, "self");
        requireFinite(spouse, "spouse");
        requireFinite(parentOrChild, "parentOrChild");
        requireFinite(sibling, "sibling");
        requireFinite(playerFriend, "playerFriend");
        requireFinite(playerBountyTarget, "playerBountyTarget");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
