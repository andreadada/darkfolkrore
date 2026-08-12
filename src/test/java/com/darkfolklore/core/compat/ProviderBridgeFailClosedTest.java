package com.darkfolklore.core.compat;

import com.darkfolklore.core.predation.PredatorKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProviderBridgeFailClosedTest {
    @Test
    void absentPredationProviderCannotClassifyTargetOrPerformFeed() {
        VampirePredationBridge bridge = VampirePredationBridge.DISABLED;

        assertFalse(bridge.runtimeAvailable());
        assertEquals(PredatorKind.NONE, bridge.predatorKind(null));
        assertFalse(bridge.wantsBlood(null));
        assertFalse(bridge.canWildFeed(null, null));
        assertFalse(bridge.performWildFeed(null, null));
        assertFalse(bridge.canMcaVampireTarget(null, null));
        assertFalse(bridge.canMcaAnimalFeed(null, null));
        assertFalse(bridge.performMcaAnimalFeed(null, null));
        assertFalse(bridge.providerSnapshot(null).available());
        bridge.clearRuntimeState();
    }

    @Test
    void absentLifecycleProviderCannotObserveOrInstallNativeAi() {
        McaVampireLifecycleBridge bridge = McaVampireLifecycleBridge.DISABLED;

        assertFalse(bridge.runtimeAvailable());
        assertFalse(bridge.snapshot(null).available());
        assertFalse(bridge.ensureNativeAi(null));
    }
}
