package com.darkfolklore.core.predation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredationEnvironmentPolicyTest {
    @Test
    void nightAlwaysAllowsEnvironmentGate() {
        assertTrue(PredationPolicy.environmentAllowsPredation(false, true));
        assertTrue(PredationPolicy.environmentAllowsPredation(false, false));
    }

    @Test
    void openSkyDaylightBlocksAutonomousPredation() {
        assertFalse(PredationPolicy.environmentAllowsPredation(true, true));
    }

    @Test
    void shelteredDaytimeCanBeConsideredWithoutInventingSunImmunity() {
        assertTrue(PredationPolicy.environmentAllowsPredation(true, false));
    }
}
