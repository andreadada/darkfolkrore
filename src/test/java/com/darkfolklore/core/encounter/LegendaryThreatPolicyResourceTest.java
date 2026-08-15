package com.darkfolklore.core.encounter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegendaryThreatPolicyResourceTest {
    private final Path root = Path.of(System.getProperty("darkfolklore.resourcesDir"));

    @Test
    void revenantHasExplicitDreadFloorInsteadOfGenericHostileLevel() throws IOException {
        JsonObject policy = load("graveyard_revenant.json");
        assertEquals("graveyard:revenant", policy.get("entity").getAsString());
        assertTrue(policy.get("l2_minimum_level").getAsInt() >= 45);
        assertTrue(policy.get("natural_spawn_multiplier").getAsDouble() <= 0.002D);
    }

    @Test
    void wildHuntEntitiesCannotLeakIntoOrdinaryNaturalSpawns() throws IOException {
        JsonObject captain = load("occultism_wild_hunt_captain.json");
        JsonObject follower = load("occultism_wild_hunt_follower.json");

        assertEquals(0.0D, captain.get("natural_spawn_multiplier").getAsDouble(), 0.0D);
        assertEquals(0.0D, follower.get("natural_spawn_multiplier").getAsDouble(), 0.0D);
        assertTrue(captain.get("l2_minimum_level").getAsInt() > follower.get("l2_minimum_level").getAsInt());
        assertTrue(follower.get("l2_minimum_level").getAsInt() > 20,
                "Wild Hunt followers should be stronger than the generic hostile floor");
    }

    private JsonObject load(String name) throws IOException {
        Path path = root.resolve("data/darkfolklore/darkfolklore/encounters").resolve(name);
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
