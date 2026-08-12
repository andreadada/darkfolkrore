package com.darkfolklore.core.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialObservationPolicyTest {
    @Test
    void unavailableSnapshotRetriesEveryTickUntilAvailable() {
        assertFalse(InitialObservationPolicy.shouldAttempt(100, 101, false));
        assertFalse(InitialObservationPolicy.shouldAttempt(100, 101, true));
        assertTrue(InitialObservationPolicy.shouldAttempt(101, 101, false));
        assertTrue(InitialObservationPolicy.shouldAttempt(150, 101, false));
        assertTrue(InitialObservationPolicy.shouldRetain(150, 101, false));
        assertFalse(InitialObservationPolicy.shouldRetain(150, 101, true));
    }

    @Test
    void retryExpiresBoundedlyAndReturnsToNormalSampling() {
        int expired = 101 + InitialObservationPolicy.MAX_RETRY_TICKS;
        assertFalse(InitialObservationPolicy.shouldRetain(expired, 101, false));
        assertFalse(InitialObservationPolicy.shouldAttempt(expired + 1, 101, false));
        assertTrue(InitialObservationPolicy.shouldAttempt(expired + 1, 101, true));
    }
}
