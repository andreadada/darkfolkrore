package com.darkfolklore.core.encounter;

import net.minecraft.resources.ResourceLocation;

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
        id = requireResourceId(id, "Encounter id");
        entityId = requireResourceId(entityId, "Encounter entity");
        if (!Double.isFinite(naturalSpawnMultiplier) || naturalSpawnMultiplier < 0.0D || naturalSpawnMultiplier > 1.0D) {
            throw new IllegalArgumentException("natural_spawn_multiplier must be finite and 0..1");
        }
        if (minimumEncounterPressure < 0 || minimumEncounterPressure > 100) {
            throw new IllegalArgumentException("minimum_encounter_pressure must be 0..100");
        }
        if (l2MinimumLevel < 0 || l2MinimumLevel > 500) {
            throw new IllegalArgumentException("l2_minimum_level must be 0..500");
        }
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
