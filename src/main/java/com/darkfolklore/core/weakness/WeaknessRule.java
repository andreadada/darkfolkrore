package com.darkfolklore.core.weakness;

import com.darkfolklore.core.traits.CreatureTrait;
import com.darkfolklore.core.traits.ItemTrait;

import java.util.Objects;
import java.util.Set;

public record WeaknessRule(
        String id,
        Set<CreatureTrait> targetTraits,
        Set<ItemTrait> requiredItemTraits,
        float multiplier,
        Set<String> nativeProviderNamespaces,
        int priority
) {
    public WeaknessRule {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        targetTraits = Set.copyOf(Objects.requireNonNull(targetTraits, "targetTraits"));
        requiredItemTraits = Set.copyOf(Objects.requireNonNull(requiredItemTraits, "requiredItemTraits"));
        nativeProviderNamespaces = Set.copyOf(nativeProviderNamespaces == null ? Set.of() : nativeProviderNamespaces);
        if (targetTraits.isEmpty()) throw new IllegalArgumentException("targetTraits cannot be empty");
        if (requiredItemTraits.isEmpty()) throw new IllegalArgumentException("requiredItemTraits cannot be empty");
        if (!Float.isFinite(multiplier) || multiplier <= 0.0F || multiplier > 16.0F) {
            throw new IllegalArgumentException("multiplier must be finite and in (0,16]");
        }
    }

    public boolean matches(Set<CreatureTrait> creatureTraits, Set<ItemTrait> itemTraits, String targetNamespace) {
        return creatureTraits.containsAll(targetTraits)
                && itemTraits.containsAll(requiredItemTraits)
                && !nativeProviderNamespaces.contains(targetNamespace);
    }
}
