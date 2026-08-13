package com.darkfolklore.core.encounter;

import java.util.Set;

public record EncounterPolicy(
        String id,
        String entityId,
        double naturalSpawnMultiplier,
        double vitalityMultiplier,
        int minimumEncounterPressure,
        int l2MinimumLevel,
        Set<ThreatTrait> guaranteedTraits
) {
    public EncounterPolicy {
        if (id == null || id.isBlank() || !id.contains(":")) throw new IllegalArgumentException("Encounter id must be namespaced");
        if (entityId == null || entityId.isBlank() || !entityId.contains(":")) throw new IllegalArgumentException("Encounter entity must be namespaced");
        if (naturalSpawnMultiplier < 0.0D || naturalSpawnMultiplier > 1.0D) throw new IllegalArgumentException("natural_spawn_multiplier must be 0..1");
        if (vitalityMultiplier < 1.0D) throw new IllegalArgumentException("vitality_multiplier must be >= 1");
        if (minimumEncounterPressure < 0 || minimumEncounterPressure > 100) throw new IllegalArgumentException("minimum_encounter_pressure must be 0..100");
        if (l2MinimumLevel < 0) throw new IllegalArgumentException("l2_minimum_level must be >= 0");
        guaranteedTraits = guaranteedTraits == null ? Set.of() : Set.copyOf(guaranteedTraits);
    }
}
