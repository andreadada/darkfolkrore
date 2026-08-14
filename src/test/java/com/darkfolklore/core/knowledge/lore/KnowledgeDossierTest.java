package com.darkfolklore.core.knowledge.lore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeDossierTest {
    @Test
    void observedKnowledgeRevealsSignsButNotWeaknesses() {
        KnowledgeDossier dossier = KnowledgeDossier.from("darkfolklore:vampire", new LoreProgress(25));
        assertTrue(dossier.reveals(KnowledgeFacet.EXISTENCE));
        assertTrue(dossier.reveals(KnowledgeFacet.SIGNS));
        assertTrue(dossier.reveals(KnowledgeFacet.BEHAVIOR));
        assertFalse(dossier.reveals(KnowledgeFacet.WEAKNESSES));
        assertFalse(dossier.reveals(KnowledgeFacet.CURE));
    }

    @Test
    void studiedKnowledgeRevealsActionableCountermeasuresWithoutMasterLore() {
        KnowledgeDossier dossier = KnowledgeDossier.from("darkfolklore:vampire", new LoreProgress(60));
        assertTrue(dossier.reveals(KnowledgeFacet.WEAKNESSES));
        assertTrue(dossier.reveals(KnowledgeFacet.COUNTERMEASURES));
        assertTrue(dossier.reveals(KnowledgeFacet.CURE));
        assertFalse(dossier.reveals(KnowledgeFacet.ORIGIN));
        assertFalse(dossier.reveals(KnowledgeFacet.BLOODLINE));
    }

    @Test
    void masteredKnowledgeRevealsEveryFacet() {
        KnowledgeDossier dossier = KnowledgeDossier.from("darkfolklore:vampire", new LoreProgress(100));
        assertEquals(KnowledgeFacet.values().length, dossier.revealed().size());
        assertTrue(dossier.hidden().isEmpty());
    }
}
