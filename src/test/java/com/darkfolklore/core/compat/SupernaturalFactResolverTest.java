package com.darkfolklore.core.compat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupernaturalFactResolverTest {
    @Test
    void exactProviderDetectsMcaVampireAndNonVampireMcaVillager() {
        assertEquals(FactResult.TRUE,
                SupernaturalFactResolver.resolveMca(CompatibilityStatus.ACTIVE, FactResult.TRUE));
        assertEquals(FactResult.FALSE,
                SupernaturalFactResolver.resolveMca(CompatibilityStatus.ACTIVE, FactResult.FALSE));
        assertEquals(FactResult.UNKNOWN,
                SupernaturalFactResolver.resolveMca(CompatibilityStatus.ACTIVE, FactResult.UNKNOWN));
        assertEquals(FactResult.UNKNOWN,
                SupernaturalFactResolver.resolveMca(CompatibilityStatus.ACTIVE, FactResult.NOT_APPLICABLE));
    }

    @Test
    void unsupportedOrFailedMcaAuthorityIsUnknownAndNeverGenericFalse() {
        for (CompatibilityStatus status : List.of(CompatibilityStatus.UNTESTED_VERSION,
                CompatibilityStatus.PARTIAL, CompatibilityStatus.UNSUPPORTED, CompatibilityStatus.ERROR)) {
            assertEquals(FactResult.UNKNOWN, SupernaturalFactResolver.resolveMca(status, FactResult.FALSE));
        }
    }

    @Test
    void absentMcaProviderIsNotApplicableInsteadOfFabricatingFalse() {
        assertEquals(FactResult.NOT_APPLICABLE,
                SupernaturalFactResolver.resolveMca(CompatibilityStatus.DISABLED, FactResult.FALSE));
    }

    @Test
    void playerVampireUsesGenericVampirismFact() {
        assertEquals(FactResult.TRUE,
                SupernaturalFactResolver.resolveGeneric(List.of(FactResult.TRUE)));
    }

    @Test
    void normalVampirismMobUsesGenericVampirismFact() {
        assertEquals(FactResult.TRUE,
                SupernaturalFactResolver.resolveGeneric(List.of(FactResult.FALSE, FactResult.TRUE)));
    }

    @Test
    void genericProviderUnknownOutweighsFalseButNotTrue() {
        assertEquals(FactResult.UNKNOWN,
                SupernaturalFactResolver.resolveGeneric(List.of(FactResult.FALSE, FactResult.UNKNOWN)));
        assertEquals(FactResult.TRUE,
                SupernaturalFactResolver.resolveGeneric(List.of(FactResult.UNKNOWN, FactResult.TRUE)));
        assertEquals(FactResult.FALSE,
                SupernaturalFactResolver.resolveGeneric(List.of(FactResult.FALSE)));
        assertEquals(FactResult.NOT_APPLICABLE,
                SupernaturalFactResolver.resolveGeneric(List.of(FactResult.NOT_APPLICABLE)));
    }

    @Test
    void componentFailureProducesPartialStatusWithoutDemotingHealthyFacts() {
        var components = new CompatibilityManager.ProviderComponents(CompatibilityStatus.ACTIVE,
                CompatibilityStatus.ERROR, CompatibilityStatus.ACTIVE);
        assertEquals(CompatibilityStatus.PARTIAL, components.combinedStatus());
        assertEquals(CompatibilityStatus.ACTIVE, components.facts());
    }

    @Test
    void eachProviderComponentCanFailWithoutDisablingTheOthers() {
        assertEquals(CompatibilityStatus.ACTIVE, new CompatibilityManager.ProviderComponents(
                CompatibilityStatus.ACTIVE, CompatibilityStatus.ACTIVE, CompatibilityStatus.ACTIVE).combinedStatus());

        for (var components : List.of(
                new CompatibilityManager.ProviderComponents(CompatibilityStatus.ERROR,
                        CompatibilityStatus.ACTIVE, CompatibilityStatus.ACTIVE),
                new CompatibilityManager.ProviderComponents(CompatibilityStatus.ACTIVE,
                        CompatibilityStatus.ERROR, CompatibilityStatus.ACTIVE),
                new CompatibilityManager.ProviderComponents(CompatibilityStatus.ACTIVE,
                        CompatibilityStatus.ACTIVE, CompatibilityStatus.ERROR))) {
            assertEquals(CompatibilityStatus.PARTIAL, components.combinedStatus());
        }

        var predationFailed = new CompatibilityManager.ProviderComponents(CompatibilityStatus.ACTIVE,
                CompatibilityStatus.ERROR, CompatibilityStatus.ACTIVE);
        assertTrue(predationFailed.factualAuthorityAvailable());
        assertFalse(predationFailed.predationAvailable());
        assertTrue(predationFailed.lifecycleAvailable());

        assertEquals(CompatibilityStatus.ERROR, new CompatibilityManager.ProviderComponents(
                CompatibilityStatus.ERROR, CompatibilityStatus.ERROR, CompatibilityStatus.ERROR).combinedStatus());
    }
}
