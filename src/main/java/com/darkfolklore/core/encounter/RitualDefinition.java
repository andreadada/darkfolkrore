package com.darkfolklore.core.encounter;

import java.util.Map;

public record RitualDefinition(String id, String encounterId, int requiredKnowledgePoints, long cooldownTicks,
                               boolean enabled, String focusBlock, String activationItem, boolean requiresNight,
                               int spawnCount, Map<String, Integer> itemCosts) {
    public RitualDefinition {
        if (id == null || id.isBlank() || !id.contains(":")) throw new IllegalArgumentException("Ritual id must be namespaced");
        if (encounterId == null || encounterId.isBlank() || !encounterId.contains(":")) throw new IllegalArgumentException("Ritual encounter must be namespaced");
        if (requiredKnowledgePoints < 0 || requiredKnowledgePoints > 100) throw new IllegalArgumentException("required_knowledge_points must be 0..100");
        if (cooldownTicks < 0L) throw new IllegalArgumentException("cooldown_ticks must be >= 0");
        if (focusBlock == null || !focusBlock.contains(":")) throw new IllegalArgumentException("focus_block must be namespaced");
        if (activationItem == null || !activationItem.contains(":")) throw new IllegalArgumentException("activation_item must be namespaced");
        if (spawnCount < 1 || spawnCount > 8) throw new IllegalArgumentException("spawn_count must be 1..8");
        itemCosts = itemCosts == null ? Map.of() : Map.copyOf(itemCosts);
        itemCosts.forEach((item, count) -> {
            if (item == null || !item.contains(":")) throw new IllegalArgumentException("ritual item costs must use namespaced ids");
            if (count == null || count < 1 || count > 64) throw new IllegalArgumentException("ritual item cost must be 1..64");
        });
    }
}
