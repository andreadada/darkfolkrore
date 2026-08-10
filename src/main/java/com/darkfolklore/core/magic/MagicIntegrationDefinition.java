package com.darkfolklore.core.magic;

import com.darkfolklore.core.traits.ItemTrait;

import java.util.Objects;
import java.util.Set;

public record MagicIntegrationDefinition(
        String id,
        Set<MagicTradition> traditions,
        Set<ItemTrait> requiredTraits,
        String knowledgeReward,
        int knowledgePoints
) {
    public MagicIntegrationDefinition {
        if (id == null || !id.contains(":")) throw new IllegalArgumentException("id must be namespaced");
        traditions = Set.copyOf(Objects.requireNonNull(traditions));
        requiredTraits = Set.copyOf(Objects.requireNonNull(requiredTraits));
        if (traditions.size() < 2) throw new IllegalArgumentException("integration must join at least two traditions");
        if (requiredTraits.isEmpty()) throw new IllegalArgumentException("requiredTraits cannot be empty");
        knowledgeReward = Objects.requireNonNullElse(knowledgeReward, "darkfolklore:magic");
        knowledgePoints = Math.max(0, Math.min(100, knowledgePoints));
    }
}
