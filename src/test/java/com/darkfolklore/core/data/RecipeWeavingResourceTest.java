package com.darkfolklore.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeWeavingResourceTest {
    private final Path root = Path.of(System.getProperty("darkfolklore.resourcesDir"));

    @Test
    void garlicRecipeEquivalenceIsNarrowAndCommonTagIsExtended() throws IOException {
        Set<String> recipeGarlic = tagValues("data/darkfolklore/tags/item/recipe/garlic.json");
        assertTrue(recipeGarlic.contains("vampirism:garlic"));
        assertTrue(recipeGarlic.contains("enchanted:garlic"));
        assertFalse(recipeGarlic.contains("vampiresdelight:wild_garlic"),
                "related wild garlic must remain semantic-only, not a universal recipe substitute");

        Set<String> commonGarlic = tagValues("data/c/tags/item/crops/garlic.json");
        assertTrue(commonGarlic.contains("enchanted:garlic"),
                "Enchanted garlic should participate in the common crop tag for future provider recipes");
    }

    @Test
    void customProviderStationsRemainProviderOwned() throws IOException {
        JsonObject arrows = load("data/vampirism/recipe/crossbow_arrow_vampire_killer.json");
        assertEquals("vampirism:shaped_crafting_weapontable", arrows.get("type").getAsString());
        assertEquals("darkfolklore:recipe/garlic",
                arrows.getAsJsonObject("key").getAsJsonObject("X").get("tag").getAsString());

        JsonObject salt = load("data/vampirism/recipe/pure_salt.json");
        assertEquals("vampirism:alchemical_cauldron", salt.get("type").getAsString());
        assertEquals("darkfolklore:recipe/garlic",
                salt.getAsJsonObject("ingredient").get("tag").getAsString());

        JsonObject wax = load("data/eidolon_repraised/recipe/magicians_wax.json");
        assertEquals("eidolon_repraised:crucible", wax.get("type").getAsString());
        JsonArray steps = wax.getAsJsonArray("steps");
        JsonArray finalItems = steps.get(2).getAsJsonObject().getAsJsonArray("items");
        for (JsonElement element : finalItems) {
            assertEquals("c:tallow", element.getAsJsonObject().get("tag").getAsString());
        }
    }

    @Test
    void keyInteroperabilityOverridesUseRecipeSafeTags() throws IOException {
        assertRecipeContains("data/enchanted/recipe/ritual_chalk.json", "darkfolklore:recipe/ritual_ash");
        assertRecipeContains("data/fangs_n_claws/recipe/fur_chestplate.json", "darkfolklore:recipe/fur");
        assertRecipeContains("data/hearth_and_timber/recipe/white_plaster.json", "darkfolklore:recipe/quicklime");
        assertRecipeContains("data/farm_and_charm/recipe/fertilized_soil.json", "darkfolklore:recipe/fertilizer");
        assertRecipeContains("data/mca_vamp_compat/recipe/book_occult_arts.json", "darkfolklore:recipe/ritual_focus");
    }

    @Test
    void curatedWeavingDoesNotCollapseKeepDistinctItems() throws IOException {
        Set<String> recipeTagFiles = new HashSet<>();
        Path recipeTags = root.resolve("data/darkfolklore/tags/item/recipe");
        try (var paths = Files.walk(recipeTags)) {
            paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try {
                    recipeTagFiles.add(Files.readString(path, StandardCharsets.UTF_8));
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
        String combined = String.join("\n", recipeTagFiles);
        assertFalse(combined.contains("occultism:soul_shard"),
                "Occultism soul shard is mechanically distinct from Eidolon's soul shard");
        assertFalse(combined.contains("feywild:mandrake_root"),
                "Feywild and Enchanted mandrakes are KEEP_DISTINCT");
        assertFalse(combined.contains("enchanted:poppet") && combined.contains("malum:poppet"),
                "Poppets must not become universal recipe substitutes");
    }

    @Test
    void wovenRecipesHaveSafeFallbacksOrAreAdditive() throws IOException {
        Set<String> ritualFocus = tagValues("data/darkfolklore/tags/item/recipe/ritual_focus.json");
        assertTrue(ritualFocus.contains("minecraft:amethyst_shard"),
                "same-id woven recipes need a vanilla ritual-focus fallback");

        JsonObject altar = load("data/werewolves/recipe/stone_altar.json");
        assertEquals("werewolves:stone_altar", altar.getAsJsonObject("result").get("id").getAsString());
        assertRecipeContains("data/werewolves/recipe/stone_altar.json", "darkfolklore:wolfsbane");
        assertRecipeContains("data/werewolves/recipe/stone_altar.json", "c:ingots/silver");

        JsonObject totem = load("data/darkfolklore/recipe/woven/vampirism_totem_top_occult.json");
        assertEquals("vampirism:totem_top_crafted", totem.getAsJsonObject("result").get("id").getAsString());
        assertRecipeContains("data/darkfolklore/recipe/woven/vampirism_totem_top_occult.json",
                "darkfolklore:recipe/occult_focus_gem");
    }

    private Set<String> tagValues(String relative) throws IOException {
        JsonArray values = load(relative).getAsJsonArray("values");
        Set<String> result = new HashSet<>();
        for (JsonElement element : values) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            } else {
                result.add(element.getAsJsonObject().get("id").getAsString());
            }
        }
        return result;
    }

    private void assertRecipeContains(String relative, String expected) throws IOException {
        String content = Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"" + expected + "\""), relative + " should reference " + expected);
    }

    private JsonObject load(String relative) throws IOException {
        String content = Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
        return JsonParser.parseString(content).getAsJsonObject();
    }
}
