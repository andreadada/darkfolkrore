package com.darkfolklore.core.canonical;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalDefinitionTest {
    @Test
    void resolvesPrimaryAndLegacyImplementations() {
        CanonicalDefinition definition = new CanonicalDefinition(
                "darkfolklore:garlic", CanonicalKind.ITEM, "vampirism:item_garlic",
                List.of("enchanted:garlic"), CanonicalPolicy.INTEROPERABILITY_ONLY,
                "Vampirism owns specialized behavior");
        assertTrue(definition.containsImplementation("vampirism:item_garlic"));
        assertTrue(definition.containsImplementation("enchanted:garlic"));
        assertFalse(definition.containsImplementation("minecraft:apple"));
    }

    @Test
    void rejectsMissingCanonicalIdForActivePolicy() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalDefinition(
                "darkfolklore:test", CanonicalKind.ITEM, "", List.of(),
                CanonicalPolicy.INTEROPERABILITY_ONLY, ""));
    }
}
