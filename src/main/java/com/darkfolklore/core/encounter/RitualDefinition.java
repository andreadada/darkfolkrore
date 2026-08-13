package com.darkfolklore.core.encounter;

public record RitualDefinition(String id, String encounterId, int requiredKnowledgePoints, long cooldownTicks, boolean enabled) {
    public RitualDefinition {
        if (id == null || id.isBlank() || !id.contains(":")) throw new IllegalArgumentException("Ritual id must be namespaced");
        if (encounterId == null || encounterId.isBlank() || !encounterId.contains(":")) throw new IllegalArgumentException("Ritual encounter must be namespaced");
        if (requiredKnowledgePoints < 0 || requiredKnowledgePoints > 100) throw new IllegalArgumentException("required_knowledge_points must be 0..100");
        if (cooldownTicks < 0L) throw new IllegalArgumentException("cooldown_ticks must be >= 0");
    }
}
