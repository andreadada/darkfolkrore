package com.darkfolklore.core.predation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalizedFeedPolicyTest {
    @Test
    void positiveFinalizedProviderDrainCreatesNonlethalEvidence() {
        assertTrue(FinalizedFeedPolicy.isRealFeed(1, false));
        assertTrue(FinalizedFeedPolicy.createsNonlethalEvidence(1, false, true));
    }

    @Test
    void cancelledOrZeroedProviderDrainCreatesNoCoreConsequences() {
        assertFalse(FinalizedFeedPolicy.isRealFeed(0, false));
        assertFalse(FinalizedFeedPolicy.isRealFeed(-1, false));
        assertFalse(FinalizedFeedPolicy.createsNonlethalEvidence(0, false, true));
    }

    @Test
    void selfTargetIsRejectedAndLethalDrainDoesNotDuplicateEvidence() {
        assertFalse(FinalizedFeedPolicy.isRealFeed(3, true));
        assertFalse(FinalizedFeedPolicy.createsNonlethalEvidence(3, false, false));
    }
}
