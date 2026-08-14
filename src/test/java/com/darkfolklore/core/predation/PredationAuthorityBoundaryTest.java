package com.darkfolklore.core.predation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredationAuthorityBoundaryTest {
    @Test
    void mcaNamespaceNeverUsesGenericWildAuthorityEvenWhenWildProviderIsHealthy() {
        assertFalse(PredationPolicy.mayUseWildAuthority(true, true));
        assertFalse(PredationPolicy.mayUseWildAuthority(true, false));
    }

    @Test
    void nonMcaEntitiesMayUseWildAuthorityOnlyWhenThatProviderIsHealthy() {
        assertTrue(PredationPolicy.mayUseWildAuthority(false, true));
        assertFalse(PredationPolicy.mayUseWildAuthority(false, false));
    }
}
