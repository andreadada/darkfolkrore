package com.darkfolklore.core.compat.mca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaVampCompatVersionTest {
    @Test
    void reviewedProviderVersionsAreAcceptedAfterNormalization() {
        assertTrue(McaVampCompatAdapter.supportsVersion("2.0.29"));
        assertTrue(McaVampCompatAdapter.supportsVersion("2.0.12"));
        assertTrue(McaVampCompatAdapter.supportsVersion("3.0.29"));
        assertTrue(McaVampCompatAdapter.supportsVersion("v2.0.29"));
        assertTrue(McaVampCompatAdapter.supportsVersion("2.0.29+pack.7"));
        assertEquals("2.0.29", McaVampCompatAdapter.normalizeVersion("  mca-vamp-2.0.29+pack.7  "));
    }

    @Test
    void compatibleTwoPointZeroLineMayEnterRuntimeProbeButOtherLinesFailClosed() {
        assertTrue(McaVampCompatAdapter.runtimeProbeEligible("2.0.30"));
        assertTrue(McaVampCompatAdapter.runtimeProbeEligible("2.0.99+custom"));
        assertFalse(McaVampCompatAdapter.runtimeProbeEligible("2.0.11"));
        assertFalse(McaVampCompatAdapter.runtimeProbeEligible("2.1.0"));
        assertFalse(McaVampCompatAdapter.runtimeProbeEligible("3.0.30"));
        assertFalse(McaVampCompatAdapter.runtimeProbeEligible(""));
        assertFalse(McaVampCompatAdapter.runtimeProbeEligible(null));
    }
}
