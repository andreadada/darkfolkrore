package com.darkfolklore.core.compat.mca;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaTrustModelTest {
    @Test
    void familyRelationshipsHaveNamedConfigurableContributions() {
        McaSocialContext spouse = context(McaRelationshipCategory.SPOUSE);
        McaTrustResult result = McaTrustModel.evaluate(spouse);

        assertEquals(0.25, result.modifier(), 0.0001);
        assertEquals("MCA spouse", result.contributions().getFirst().reason());

        McaTrustSettings custom = new McaTrustSettings(0, 0.41, 0, 0, 0, 0);
        assertEquals(0.41, McaTrustModel.evaluate(spouse, custom).modifier(), 0.0001);
    }

    @Test
    void unsupportedOrUnknownRelationshipDoesNotInventTrust() {
        McaTrustResult result = McaTrustModel.evaluate(context(McaRelationshipCategory.UNKNOWN));
        assertEquals(0, result.modifier());
        assertTrue(result.contributions().isEmpty());
    }

    @Test
    void onlyVerifiedPersonalityNamesReceiveSmallMappedEffects() {
        McaPersonalityInfluence extroverted = McaPersonalityInfluence.fromVerifiedName("EXTROVERTED");
        assertTrue(extroverted.recognized());
        assertEquals(0.15, extroverted.rumorTransmissionModifier(), 0.0001);

        McaPersonalityInfluence anxious = McaPersonalityInfluence.fromVerifiedName("ANXIOUS");
        assertEquals(0.15, anxious.fearModifier(), 0.0001);
        assertEquals(-0.10, anxious.investigationModifier(), 0.0001);

        McaPersonalityInfluence invented = McaPersonalityInfluence.fromVerifiedName("GOSSIPY");
        assertFalse(invented.recognized());
        assertEquals(0, invented.rumorTransmissionModifier());
    }

    @Test
    void adapterRejectsUntestedVersionBeforeLoadingOptionalClasses() {
        McaSocialAdapter adapter = new McaSocialAdapter();
        assertFalse(adapter.initialize("7.7.31+1.21.1"));
        assertFalse(adapter.isReady());
        assertTrue(adapter.statusDetail().contains(McaSocialAdapter.TESTED_VERSION));
    }

    private static McaSocialContext context(McaRelationshipCategory category) {
        return new McaSocialContext(category, OptionalInt.empty(), Optional.empty(), Optional.empty(), "test");
    }
}
