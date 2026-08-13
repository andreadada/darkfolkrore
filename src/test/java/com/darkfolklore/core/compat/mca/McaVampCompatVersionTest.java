package com.darkfolklore.core.compat.mca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaVampCompatVersionTest {
    @Test
    void auditedProviderVersionsAreAccepted() {
        assertTrue(McaVampCompatAdapter.supportsVersion("3.0.29"));
        assertTrue(McaVampCompatAdapter.supportsVersion("2.0.12"));
    }

    @Test
    void unknownProviderVersionsRemainFailClosed() {
        assertFalse(McaVampCompatAdapter.supportsVersion("3.0.30"));
        assertFalse(McaVampCompatAdapter.supportsVersion(""));
        assertFalse(McaVampCompatAdapter.supportsVersion(null));
    }
}
