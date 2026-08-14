package com.darkfolklore.core.compat.vampirism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VampirePredationCompatIsolationTest {
    @Test
    void disablingMcaProviderNeverDisablesWildVampirismCircuit() {
        VampirePredationCompat bridge = new VampirePredationCompat(false);

        assertTrue(bridge.runtimeAvailable());
        assertTrue(bridge.wildRuntimeAvailable());
        assertFalse(bridge.mcaRuntimeAvailable());
        assertTrue(bridge.circuitStatus().get("wild_feed"));
        assertFalse(bridge.circuitStatus().get("mca_facts"));
        assertFalse(bridge.circuitStatus().get("mca_target"));
        assertFalse(bridge.circuitStatus().get("mca_animal_feed"));
        assertFalse(bridge.circuitStatus().get("mca_native_bite"));

        bridge.clearRuntimeState();
        assertTrue(bridge.wildRuntimeAvailable());
        assertFalse(bridge.mcaRuntimeAvailable());
        assertFalse(bridge.circuitStatus().get("mca_facts"));
    }
}
