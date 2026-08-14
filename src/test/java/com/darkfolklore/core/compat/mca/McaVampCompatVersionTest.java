package com.darkfolklore.core.compat.mca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaVampCompatVersionTest {
    @Test
    void supportedProviderVersionsAreAccepted() {
        assertTrue(McaVampCompatAdapter.supportsVersion("2.0.29"));
        assertTrue(McaVampCompatAdapter.supportsVersion("2.0.12"));
        assertTrue(McaVampCompatAdapter.supportsVersion("3.0.29"));
    }

    @Test
    void unknownProviderVersionsRemainFailClosed() {
        assertFalse(McaVampCompatAdapter.supportsVersion("2.0.30"));
        assertFalse(McaVampCompatAdapter.supportsVersion("3.0.30"));
        assertFalse(McaVampCompatAdapter.supportsVersion(""));
        assertFalse(McaVampCompatAdapter.supportsVersion(null));
    }
}
