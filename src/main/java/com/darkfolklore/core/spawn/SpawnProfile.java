package com.darkfolklore.core.spawn;

import java.util.Objects;

public record SpawnProfile(
        String entityId,
        SpawnRarity rarity,
        boolean naturalSpawnEnabled,
        boolean canonicalizationSuppression,
        boolean nocturnal,
        float eventMultiplier
) {
    public SpawnProfile {
        if (entityId == null || !entityId.contains(":")) {
            throw new IllegalArgumentException("entityId must be namespaced");
        }
        rarity = Objects.requireNonNull(rarity, "rarity");
        if (!Float.isFinite(eventMultiplier) || eventMultiplier < 0 || eventMultiplier > 10) {
            throw new IllegalArgumentException("eventMultiplier must be in [0,10]");
        }
    }
}
