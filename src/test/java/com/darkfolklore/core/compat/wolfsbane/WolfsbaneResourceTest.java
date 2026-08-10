package com.darkfolklore.core.compat.wolfsbane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class WolfsbaneResourceTest {
    private static final Path ROOT = Path.of(System.getProperty("darkfolklore.resourcesDir"));

    @Test
    void canonicalDefinitionResolvesCanonicalAndLegacyIds() throws IOException {
        JsonObject definition = object("data/darkfolklore/darkfolklore/canonical/wolfsbane.json");
        assertEquals("darkfolklore:wolfsbane", definition.get("concept").getAsString());
        assertEquals("enchanted:wolfsbane_flower", definition.get("canonical").getAsString());
        assertEquals("FULL_CANONICALIZATION", definition.get("policy").getAsString());
        assertTrue(strings(definition.getAsJsonArray("implementations")).anyMatch("werewolves:wolfsbane"::equals));

        JsonObject tag = object("data/darkfolklore/tags/item/wolfsbane.json");
        assertEquals("enchanted:wolfsbane_flower",
                tag.getAsJsonArray("values").get(0).getAsJsonObject().get("id").getAsString());
        assertTrue(tagValues(tag).anyMatch("enchanted:wolfsbane_flower"::equals));
        assertTrue(tagValues(tag).anyMatch("werewolves:wolfsbane"::equals));
    }

    @Test
    void everyDirectWerewolvesHerbRecipeUsesTheSemanticTag() throws IOException {
        JsonObject dye = object("data/werewolves/recipe/purple_dye.json");
        assertEquals("darkfolklore:wolfsbane",
                dye.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("tag").getAsString());
        assertWerewolvesCondition(dye);

        JsonObject finder = object("data/werewolves/recipe/wolfsbane_finder.json");
        assertEquals("darkfolklore:wolfsbane",
                finder.getAsJsonObject("key").getAsJsonObject("Y").get("tag").getAsString());
        assertWerewolvesCondition(finder);

        for (String recipe : new String[]{"wolfsbane_diffuser_core", "wolfsbane_diffuser_core_improved"}) {
            JsonObject cauldron = object("data/werewolves/recipe/" + recipe + ".json");
            assertEquals("darkfolklore:wolfsbane",
                    cauldron.getAsJsonObject("fluid").get("tag").getAsString());
            assertWerewolvesCondition(cauldron);
        }

        JsonObject advancement = object("data/werewolves/advancement/recipes/misc/purple_dye.json");
        JsonObject itemPredicate = advancement.getAsJsonObject("criteria")
                .getAsJsonObject("has_wolfsbane")
                .getAsJsonObject("conditions")
                .getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("#darkfolklore:wolfsbane", itemPredicate.get("items").getAsString());
        assertWerewolvesCondition(advancement);
    }

    @Test
    void futureLootAndWorldgenRouteAwayFromLegacyWolfsbane() throws IOException {
        JsonObject loot = object("data/darkfolklore/loot_modifiers/canonicalize_items.json");
        assertEquals("enchanted:wolfsbane_flower",
                loot.getAsJsonObject("replacements").get("werewolves:wolfsbane").getAsString());

        JsonObject featureTag = object(
                "data/darkfolklore/tags/worldgen/placed_feature/noncanonical_wolfsbane.json");
        assertTrue(tagValues(featureTag).anyMatch("werewolves:wolfsbane"::equals));

        JsonObject modifier = object(
                "data/darkfolklore/neoforge/biome_modifier/remove_noncanonical_wolfsbane.json");
        assertEquals("#darkfolklore:noncanonical_wolfsbane", modifier.get("features").getAsString());
        assertEquals("vegetal_decoration", modifier.get("step").getAsString());
        JsonArray conditions = modifier.getAsJsonArray("neoforge:conditions");
        assertTrue(StreamSupport.stream(conditions.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .anyMatch(condition -> "werewolves".equals(string(condition, "modid"))));
        assertTrue(StreamSupport.stream(conditions.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .anyMatch(condition -> "enchanted".equals(string(condition, "modid"))));
        assertTrue(StreamSupport.stream(conditions.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .anyMatch(condition -> "werewolves:wolfsbane".equals(string(condition, "item"))));
        assertTrue(StreamSupport.stream(conditions.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .anyMatch(condition -> "enchanted:wolfsbane_flower".equals(string(condition, "item"))));
        assertTrue(StreamSupport.stream(conditions.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .anyMatch(condition -> "enchanted:wolfsbane_seeds".equals(string(condition, "item"))));
    }

    @Test
    void plantAndSeedTagsUseTheAuditedEnchantedRegistryIds() throws IOException {
        JsonObject plants = object("data/darkfolklore/tags/block/wolfsbane_plants.json");
        assertTrue(tagValues(plants).anyMatch("enchanted:wolfsbane"::equals));
        assertTrue(tagValues(plants).anyMatch("werewolves:wolfsbane"::equals));

        JsonObject seeds = object("data/darkfolklore/tags/item/wolfsbane_seeds.json");
        assertTrue(tagValues(seeds).anyMatch("enchanted:wolfsbane_seeds"::equals));

        JsonObject smallFlowers = object("data/minecraft/tags/item/small_flowers.json");
        assertTrue(tagValues(smallFlowers).anyMatch("enchanted:wolfsbane_flower"::equals));
    }

    private static JsonObject object(String relative) throws IOException {
        try (Reader reader = Files.newBufferedReader(ROOT.resolve(relative), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static java.util.stream.Stream<String> strings(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false).map(element -> element.getAsString());
    }

    private static java.util.stream.Stream<String> tagValues(JsonObject tag) {
        return StreamSupport.stream(tag.getAsJsonArray("values").spliterator(), false)
                .map(element -> element.isJsonPrimitive()
                        ? element.getAsString() : element.getAsJsonObject().get("id").getAsString());
    }

    private static void assertWerewolvesCondition(JsonObject resource) {
        JsonArray conditions = resource.getAsJsonArray("neoforge:conditions");
        assertTrue(StreamSupport.stream(conditions.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .anyMatch(condition -> "neoforge:mod_loaded".equals(string(condition, "type"))
                        && "werewolves".equals(string(condition, "modid"))));
        assertTrue(StreamSupport.stream(conditions.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .anyMatch(condition -> "neoforge:item_exists".equals(string(condition, "type"))
                        && "werewolves:wolfsbane".equals(string(condition, "item"))));
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : null;
    }
}
