package com.darkfolklore.core.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompatCapabilityCircuitTest {
    @Test
    void failureIsIsolatedPerCapability() {
        CompatCapabilityCircuit facts = new CompatCapabilityCircuit("facts");
        CompatCapabilityCircuit provenance = new CompatCapabilityCircuit("provenance");

        provenance.fail("provider member changed");

        assertTrue(facts.available());
        assertFalse(provenance.available());
        assertTrue(provenance.detail().contains("provider member changed"));
    }
}
