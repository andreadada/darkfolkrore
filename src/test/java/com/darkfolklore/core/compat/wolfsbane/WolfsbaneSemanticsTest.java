package com.darkfolklore.core.compat.wolfsbane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WolfsbaneSemanticsTest {
    @Test
    void canonicalAndLegacyFlowersAreRecipeIngredientsButSeedsAreNot() {
        assertEquals(WolfsbaneSemantics.Identity.CANONICAL,
                WolfsbaneSemantics.classifyItem("enchanted:wolfsbane_flower"));
        assertEquals(WolfsbaneSemantics.Identity.LEGACY,
                WolfsbaneSemantics.classifyItem("werewolves:wolfsbane"));
        assertEquals(WolfsbaneSemantics.Identity.SEEDS,
                WolfsbaneSemantics.classifyItem("enchanted:wolfsbane_seeds"));

        assertTrue(WolfsbaneSemantics.isRecipeIngredient("enchanted:wolfsbane_flower"));
        assertTrue(WolfsbaneSemantics.isRecipeIngredient("werewolves:wolfsbane"));
        assertFalse(WolfsbaneSemantics.isRecipeIngredient("enchanted:wolfsbane_seeds"));
        assertFalse(WolfsbaneSemantics.isRecipeIngredient("minecraft:allium"));
        assertEquals(WolfsbaneSemantics.Identity.UNRELATED, WolfsbaneSemantics.classifyItem(null));
        assertFalse(WolfsbaneSemantics.isRecipeIngredient(null));
    }

    @Test
    void futureLootOnlyRoutesTheLegacyFlower() {
        assertEquals("enchanted:wolfsbane_flower",
                WolfsbaneSemantics.futureLootReplacement("werewolves:wolfsbane").orElseThrow());
        assertTrue(WolfsbaneSemantics.futureLootReplacement("enchanted:wolfsbane_flower").isEmpty());
        assertTrue(WolfsbaneSemantics.futureLootReplacement("enchanted:wolfsbane_seeds").isEmpty());
    }

    @Test
    void implementationBridgeIsStrictlyVersionPinned() {
        assertTrue(WolfsbaneSemantics.supportsNativeBridge("2.0.3.3", "4.2.7"));
        assertFalse(WolfsbaneSemantics.supportsNativeBridge("2.0.3.4", "4.2.7"));
        assertFalse(WolfsbaneSemantics.supportsNativeBridge("2.0.3.3", "4.2.8"));
        assertFalse(WolfsbaneSemantics.supportsNativeBridge("-", "4.2.7"));
    }
}
