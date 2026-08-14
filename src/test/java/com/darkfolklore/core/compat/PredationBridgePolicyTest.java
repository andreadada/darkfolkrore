package com.darkfolklore.core.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredationBridgePolicyTest {
    @Test
    void wildPredationDependsOnlyOnVampirism() {
        assertTrue(PredationBridgePolicy.loadWildBridge(true));
        assertFalse(PredationBridgePolicy.loadWildBridge(false));

        // MCA/provider state is deliberately irrelevant to the wild decision.
        assertTrue(PredationBridgePolicy.loadWildBridge(true));
    }

    @Test
    void mcaPredationRequiresTheWholeProviderStack() {
        assertTrue(PredationBridgePolicy.enableMcaProbe(true, true, true));
        assertFalse(PredationBridgePolicy.enableMcaProbe(false, true, true));
        assertFalse(PredationBridgePolicy.enableMcaProbe(true, false, true));
        assertFalse(PredationBridgePolicy.enableMcaProbe(true, true, false));
    }
}
