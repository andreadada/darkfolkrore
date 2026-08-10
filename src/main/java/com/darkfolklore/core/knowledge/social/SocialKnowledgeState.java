package com.darkfolklore.core.knowledge.social;

public enum SocialKnowledgeState {
    UNKNOWN(0),
    RUMOR(1),
    SUSPECTED(2),
    CONFIRMED(3),
    PUBLIC(4);

    private final int strength;

    SocialKnowledgeState(int strength) {
        this.strength = strength;
    }

    public int strength() {
        return strength;
    }

    public SocialKnowledgeState max(SocialKnowledgeState other) {
        return strength >= other.strength ? this : other;
    }
}
