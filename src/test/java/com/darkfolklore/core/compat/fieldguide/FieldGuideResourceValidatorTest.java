package com.darkfolklore.core.compat.fieldguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FieldGuideResourceValidatorTest {
    @Test
    void shippedCuratedContentIsCompleteAndConsistent() throws Exception {
        Path resources = Path.of(System.getProperty("darkfolklore.resourcesDir"));
        FieldGuideResourceValidator.ValidationReport report = FieldGuideResourceValidator.validate(
                FieldGuideResourceValidator.load(resources));

        assertTrue(report.isClean(), () -> String.join(System.lineSeparator(), report.problems()));
        assertEquals(7, report.categories());
        assertEquals(10, report.entries());
        assertEquals(0, report.missingCategoryTranslations());
        assertEquals(0, report.missingEntryTranslations());
        assertEquals(0, report.orphanEntries());
        assertEquals(0, report.invalidCategories());
        assertEquals(0, report.duplicateEntryIds());
        assertEquals(0, report.missingCanonicalConcepts());
        assertEquals(0, report.unresolvedMappings());
        assertEquals(0, report.emptyCategories());
        assertEquals(0, report.invalidEntries());
    }

    @Test
    void validatorDetectsEveryRequiredFailureClass() {
        Map<String, JsonObject> categories = new LinkedHashMap<>();
        categories.put("darkfolklore:vampires", json("""
                {"icon":"fieldguide:textures/gui/icons/wither.png","contents":[
                  {"type":"entry","id":"entity:vampirism/vampire","canonical_concept":"darkfolklore:vampire",
                   "unlock":{"triggers":["scan","kill"]}}
                ]}
                """));
        categories.put("darkfolklore:duplicates", json("""
                {"icon":"fieldguide:textures/gui/icons/wolf.png","contents":[
                  {"type":"entry","id":"entity:vampirism/vampire","canonical_concept":"darkfolklore:vampire",
                   "unlock":{"triggers":["scan","kill"]}},
                  {"type":"entry","id":"entity:cnc/wendigo","canonical_concept":"darkfolklore:missing",
                   "unlock":{"triggers":["scan","kill"]}},
                  {"type":"entry","id":"entity:cnc/chupacabra","canonical_concept":"darkfolklore:vampire",
                   "unlock":{"triggers":["scan"]}},
                  {"type":"auto_populate","strategy":"monsters"}
                ]}
                """));
        categories.put("darkfolklore:empty", json("""
                {"icon":"fieldguide:textures/gui/icons/ghast.png","contents":[]}
                """));
        categories.put("darkfolklore:bad_target", json("""
                {"target_category":"darkfolklore:not_declared","icon":"missing:icon.png","contents":[]}
                """));

        JsonObject vampire = json("""
                {"concept":"darkfolklore:vampire","kind":"ENTITY","canonical":"vampirism:vampire","implementations":[]}
                """);
        Map<String, String> incompleteEnglish = Map.of(
                "category.darkfolklore.fieldguide.vampires", "category.darkfolklore.fieldguide.vampires",
                "fieldguide.name.entity.unused.orphan", "Orphan"
        );
        FieldGuideResourceValidator.Content broken = new FieldGuideResourceValidator.Content(
                categories, List.of(vampire), Map.of("en_us", incompleteEnglish));

        FieldGuideResourceValidator.ValidationReport report = FieldGuideResourceValidator.validate(broken);
        assertFalse(report.isClean());
        assertTrue(report.missingCategoryTranslations() > 0);
        assertTrue(report.problems().contains(
                "en_us is missing category translation category.darkfolklore.fieldguide.vampires"));
        assertTrue(report.missingEntryTranslations() > 0);
        assertTrue(report.orphanEntries() > 0);
        assertTrue(report.invalidCategories() > 0);
        assertTrue(report.duplicateEntryIds() > 0);
        assertTrue(report.missingCanonicalConcepts() > 0);
        assertTrue(report.unresolvedMappings() > 0);
        assertTrue(report.emptyCategories() > 0);
        assertTrue(report.invalidEntries() > 0);
    }

    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }
}
