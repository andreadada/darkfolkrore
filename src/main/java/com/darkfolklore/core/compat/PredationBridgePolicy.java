package com.darkfolklore.core.compat;

/**
 * Pure dependency policy for the predation bridge. Keeping this free of optional-mod types makes the most
 * important isolation invariant unit-testable without loading Vampirism or MCA implementation classes.
 */
public final class PredationBridgePolicy {
    private PredationBridgePolicy() {}

    /** Ordinary Vampirism predation requires Vampirism only. */
    public static boolean loadWildBridge(boolean vampirismActive) {
        return vampirismActive;
    }

    /** MCA predation may be probed only when Vampirism, MCA and the provider admission gate are all healthy. */
    public static boolean enableMcaProbe(boolean vampirismActive, boolean mcaActive, boolean providerProbeEligible) {
        return vampirismActive && mcaActive && providerProbeEligible;
    }
}
