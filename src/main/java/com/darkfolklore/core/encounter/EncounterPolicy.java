package com.darkfolklore.core.encounter;

/**
 * Dark Folklore controls how often an encounter may appear and which L2 Hostility level floor it should have.
 * It intentionally does not own health, damage, speed, armor, or forced combat-trait multipliers.
 */
public record EncounterPolicy(
        String id,
        String entityId,
        double naturalSpawnMultiplier,
        int minimumEncounterPressure,
        int l2MinimumLevel
) {
    public EncounterPolicy {
        if (id == null || id.isBlank() || !id.contains(":")) throw new IllegalArgumentException("Encounter id must be namespaced");
        if (entityId == null || entityId.isBlank() || !entityId.contains(":")) throw new IllegalArgumentException("Encounter entity must be namespaced");
        if (naturalSpawnMultiplier < 0.0D || naturalSpawnMultiplier > 1.0D) throw new IllegalArgumentException("natural_spawn_multiplier must be 0..1");
        if (minimumEncounterPressure < 0 || minimumEncounterPressure > 100) throw new IllegalArgumentException("minimum_encounter_pressure must be 0..100");
        if (l2MinimumLevel < 0) throw new IllegalArgumentException("l2_minimum_level must be >= 0");
    }
}
