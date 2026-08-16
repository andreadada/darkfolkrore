package com.darkfolklore.core.encounter;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record RitualDefinition(String id, String encounterId, int requiredKnowledgePoints, long cooldownTicks,
                               boolean enabled, String focusBlock, String activationItem, boolean requiresNight,
                               int spawnCount, Map<String, Integer> itemCosts) {
    public RitualDefinition {
        id = requireResourceId(id, "Ritual id");
        encounterId = requireResourceId(encounterId, "Ritual encounter");
        if (requiredKnowledgePoints < 0 || requiredKnowledgePoints > 100) throw new IllegalArgumentException("required_knowledge_points must be 0..100");
        if (cooldownTicks < 0L) throw new IllegalArgumentException("cooldown_ticks must be >= 0");
        focusBlock = requireResourceId(focusBlock, "focus_block");
        activationItem = requireResourceId(activationItem, "activation_item");
        if (spawnCount < 1 || spawnCount > 8) throw new IllegalArgumentException("spawn_count must be 1..8");
        itemCosts = itemCosts == null ? Map.of() : Map.copyOf(itemCosts);
        itemCosts.forEach((item, count) -> {
            requireResourceId(item, "ritual item cost id");
            if (count == null || count < 1 || count > 64) throw new IllegalArgumentException("ritual item cost must be 1..64");
        });
    }

    private static String requireResourceId(String value, String label) {
        if (value == null || !value.contains(":")) {
            throw new IllegalArgumentException(label + " must be an explicit namespaced resource location");
        }
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null || parsed.getNamespace().isBlank() || parsed.getPath().isBlank()) {
            throw new IllegalArgumentException(label + " must be a valid namespaced resource location");
        }
        return value;
    }
}
