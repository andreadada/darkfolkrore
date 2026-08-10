package com.darkfolklore.core.knowledge.lore;

public enum KnowledgeStage {
    UNKNOWN(0),
    DISCOVERED(1),
    OBSERVED(25),
    STUDIED(60),
    MASTERED(100);

    private final int threshold;

    KnowledgeStage(int threshold) {
        this.threshold = threshold;
    }

    public int threshold() {
        return threshold;
    }

    public static KnowledgeStage fromPoints(int points) {
        KnowledgeStage result = UNKNOWN;
        for (KnowledgeStage stage : values()) {
            if (points >= stage.threshold) result = stage;
        }
        return result;
    }
}
