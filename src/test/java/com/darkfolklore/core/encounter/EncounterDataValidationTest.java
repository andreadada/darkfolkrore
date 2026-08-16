package com.darkfolklore.core.encounter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncounterDataValidationTest {
    @Test
    void encounterRejectsMalformedAndNonFinitePolicyData() {
        assertThrows(IllegalArgumentException.class,
                () -> new EncounterPolicy("darkfolklore:", "minecraft:zombie", 0.5D, 0, 20));
        assertThrows(IllegalArgumentException.class,
                () -> new EncounterPolicy("darkfolklore:zombie", "minecraft:", 0.5D, 0, 20));
        assertThrows(IllegalArgumentException.class,
                () -> new EncounterPolicy("darkfolklore:zombie", "minecraft:zombie", Double.NaN, 0, 20));
        assertThrows(IllegalArgumentException.class,
                () -> new EncounterPolicy("darkfolklore:zombie", "minecraft:zombie", 0.5D, 0, 501));

        assertDoesNotThrow(() -> new EncounterPolicy(
                "darkfolklore:zombie", "minecraft:zombie", 0.5D, 20, 40));
    }

    @Test
    void ritualRejectsMalformedProviderAndCostIds() {
        assertThrows(IllegalArgumentException.class, () -> new RitualDefinition(
                "darkfolklore:test", "darkfolklore:encounter", 0, 0L, true,
                "minecraft:", "minecraft:bone", true, 1, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RitualDefinition(
                "darkfolklore:test", "darkfolklore:encounter", 0, 0L, true,
                "minecraft:soul_campfire", "minecraft:bone", true, 1, Map.of("minecraft:", 1)));

        assertDoesNotThrow(() -> new RitualDefinition(
                "darkfolklore:test", "darkfolklore:encounter", 0, 0L, true,
                "minecraft:soul_campfire", "minecraft:bone", true, 1, Map.of("minecraft:bone", 4)));
    }
}
