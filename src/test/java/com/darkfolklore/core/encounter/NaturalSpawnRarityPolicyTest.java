package com.darkfolklore.core.encounter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalSpawnRarityPolicyTest {
    @Test
    void explicitEncounterPolicyOverridesDuplicateProfileRarity() {
        var result = NaturalSpawnRarityPolicy.resolve(0.002D, 0.12D, true,
                0.04D, 1.0D, 1.0D);

        assertEquals(NaturalSpawnRarityPolicy.Source.ENCOUNTER_POLICY, result.source());
        assertEquals(0.002D, result.chance(), 0.0000001D);
    }

    @Test
    void profileOnlyHostilePreservesOldCombinedAcceptanceProbabilityInOneRoll() {
        var result = NaturalSpawnRarityPolicy.resolve(null, 0.30D, true,
                0.04D, 1.0D, 1.0D);

        assertEquals(NaturalSpawnRarityPolicy.Source.HOSTILE_PROFILE, result.source());
        assertEquals(0.012D, result.chance(), 0.0000001D);
    }

    @Test
    void zeroMultiplierIsAnAbsoluteNaturalSpawnRejection() {
        var result = NaturalSpawnRarityPolicy.resolve(0.0D, null, true,
                0.04D, 1.0D, 1.0D);

        assertTrue(NaturalSpawnRarityPolicy.reject(result, 0.0D));
        assertTrue(NaturalSpawnRarityPolicy.reject(result, 0.5D));
    }

    @Test
    void unmanagedNonHostileEntityIsNeverRejectedByThisPolicy() {
        var result = NaturalSpawnRarityPolicy.resolve(null, null, false,
                0.04D, 1.0D, 1.0D);

        assertFalse(result.managed());
        assertFalse(NaturalSpawnRarityPolicy.reject(result, 0.999D));
    }
}
