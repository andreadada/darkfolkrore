package com.darkfolklore.core.knowledge.lore;

public record LoreProgress(int points) {
    public LoreProgress {
        points = Math.max(0, Math.min(100, points));
    }

    public KnowledgeStage stage() {
        return KnowledgeStage.fromPoints(points);
    }

    public LoreProgress add(int amount) {
        return new LoreProgress(points + Math.max(0, amount));
    }
}
