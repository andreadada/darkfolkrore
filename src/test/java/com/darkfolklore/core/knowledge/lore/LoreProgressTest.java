package com.darkfolklore.core.knowledge.lore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoreProgressTest {
    @Test
    void thresholdsAreStableAndPointsClamp() {
        assertEquals(KnowledgeStage.UNKNOWN, new LoreProgress(0).stage());
        assertEquals(KnowledgeStage.DISCOVERED, new LoreProgress(1).stage());
        assertEquals(KnowledgeStage.OBSERVED, new LoreProgress(25).stage());
        assertEquals(KnowledgeStage.STUDIED, new LoreProgress(60).stage());
        assertEquals(KnowledgeStage.MASTERED, new LoreProgress(1000).stage());
    }
}
