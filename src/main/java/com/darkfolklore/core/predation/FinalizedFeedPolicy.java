package com.darkfolklore.core.predation;

/** Pure gate between finalized provider feeding outcomes and Core-owned narrative consequences. */
public final class FinalizedFeedPolicy {
    private FinalizedFeedPolicy() {}

    public static boolean isRealFeed(int finalizedAmount, boolean selfTarget) {
        return finalizedAmount > 0 && !selfTarget;
    }

    public static boolean createsNonlethalEvidence(int finalizedAmount, boolean selfTarget, boolean targetAlive) {
        return isRealFeed(finalizedAmount, selfTarget) && targetAlive;
    }
}
