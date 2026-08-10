package com.darkfolklore.core.compat.mcacapitals;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliticalRoleTest {
    @Test
    void exactAuditedTitlesMapToSemanticRoles() {
        Map<String, PoliticalRole> titles = Map.ofEntries(
                Map.entry("High Queen", PoliticalRole.HIGH_SOVEREIGN),
                Map.entry("High King", PoliticalRole.HIGH_SOVEREIGN),
                Map.entry("Queen", PoliticalRole.SOVEREIGN),
                Map.entry("King", PoliticalRole.SOVEREIGN),
                Map.entry("Queen Consort", PoliticalRole.CONSORT),
                Map.entry("King Consort", PoliticalRole.CONSORT),
                Map.entry("Dowager Queen", PoliticalRole.DOWAGER),
                Map.entry("Dowager King", PoliticalRole.DOWAGER),
                Map.entry("Heir Apparent", PoliticalRole.HEIR),
                Map.entry("Crown Princess", PoliticalRole.HEIR),
                Map.entry("Crown Prince", PoliticalRole.HEIR),
                Map.entry("Princess", PoliticalRole.ROYAL_CHILD),
                Map.entry("Prince", PoliticalRole.ROYAL_CHILD),
                Map.entry("Princess Consort", PoliticalRole.PRINCE_CONSORT),
                Map.entry("Prince Consort", PoliticalRole.PRINCE_CONSORT),
                Map.entry("Dowager Princess", PoliticalRole.DOWAGER_PRINCE),
                Map.entry("Dowager Prince", PoliticalRole.DOWAGER_PRINCE),
                Map.entry("Hand of the Queen", PoliticalRole.HAND),
                Map.entry("Hand of the King", PoliticalRole.HAND),
                Map.entry("Grand Maester", PoliticalRole.GRAND_MAESTER),
                Map.entry("Court Herald", PoliticalRole.HERALD),
                Map.entry("Duchess", PoliticalRole.DUKE),
                Map.entry("Duke", PoliticalRole.DUKE),
                Map.entry("Dowager Duchess", PoliticalRole.DOWAGER_DUKE),
                Map.entry("Dowager Duke", PoliticalRole.DOWAGER_DUKE),
                Map.entry("Maester", PoliticalRole.MAESTER),
                Map.entry("Lord Commander", PoliticalRole.COMMANDER),
                Map.entry("Lady", PoliticalRole.LORD),
                Map.entry("Lord", PoliticalRole.LORD),
                Map.entry("Commoner", PoliticalRole.COMMONER),
                Map.entry("None", PoliticalRole.NONE)
        );
        titles.forEach((title, expected) ->
                assertEquals(expected, PoliticalRole.fromExactTitle(title, false), title));
    }

    @Test
    void sirAndDameUseIndependentGuardEvidence() {
        assertEquals(PoliticalRole.KNIGHT, PoliticalRole.fromExactTitle("Sir", false));
        assertEquals(PoliticalRole.ROYAL_GUARD, PoliticalRole.fromExactTitle("Sir", true));
        assertEquals(PoliticalRole.KNIGHT, PoliticalRole.fromExactTitle("Dame", false));
        assertEquals(PoliticalRole.ROYAL_GUARD, PoliticalRole.fromExactTitle("Dame", true));
    }

    @Test
    void unknownTitlesStayUnknown() {
        assertEquals(PoliticalRole.UNKNOWN, PoliticalRole.fromExactTitle("Archwizard", false));
        assertEquals(PoliticalRole.UNKNOWN, PoliticalRole.fromExactTitle(null, false));
    }

    @Test
    void politicalWeightsNeverCreateKnowledgeAndReflectAuthority() {
        PoliticalWeights sovereign = PoliticalWeightModel.weights(PoliticalRole.SOVEREIGN);
        PoliticalWeights commoner = PoliticalWeightModel.weights(PoliticalRole.COMMONER);
        PoliticalWeights herald = PoliticalWeightModel.weights(PoliticalRole.HERALD);

        assertTrue(sovereign.investigationPriority() > commoner.investigationPriority());
        assertTrue(herald.publicAwareness() > herald.investigationPriority());
        assertEquals(PoliticalWeights.NONE, commoner);
    }

    @Test
    void capitalsAdapterRejectsUntestedVersionBeforeClassLoading() {
        McaCapitalsCompat adapter = new McaCapitalsCompat();
        assertFalse(adapter.initialize("1.0.9"));
        assertFalse(adapter.isReady());
        assertTrue(adapter.statusDetail().contains(McaCapitalsCompat.TESTED_VERSION));
        assertEquals(0, adapter.cachedRoleCount());
    }
}
