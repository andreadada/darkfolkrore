package com.darkfolklore.core.investigation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InvestigationResourceValidatorTest {
    @Test
    void shippedInvestigationProfilesAreCuratedAndInternallyConsistent() throws IOException {
        Path root = Path.of(System.getProperty("darkfolklore.resourcesDir"));
        Path profileDir = root.resolve("data/darkfolklore/darkfolklore/investigation_profiles");
        assertTrue(Files.isDirectory(profileDir));

        Set<String> canonicalConcepts = new HashSet<>();
        Path canonicalDir = root.resolve("data/darkfolklore/darkfolklore/canonical");
        try (var paths = Files.list(canonicalDir)) {
            for (Path path : paths.filter(value -> value.getFileName().toString().endsWith(".json")).toList()) {
                JsonObject json = read(path);
                if (json.has("concept")) canonicalConcepts.add(json.get("concept").getAsString());
            }
        }

        int count = 0;
        boolean faeAnalysisShipped = false;
        boolean revenantShipped = false;
        boolean wildHuntShipped = false;
        try (var paths = Files.list(profileDir)) {
            for (Path path : paths.filter(value -> value.getFileName().toString().endsWith(".json")).toList()) {
                count++;
                JsonObject json = read(path);
                String concept = json.get("concept").getAsString();
                assertTrue(canonicalConcepts.contains(concept), () -> "missing canonical concept for " + path);
                assertFalse(json.getAsJsonArray("creature_traits").isEmpty(), () -> "missing creature traits in " + path);
                assertFalse(json.getAsJsonArray("signatures").isEmpty(), () -> "missing signatures in " + path);
                int radius = json.get("tracking_radius").getAsInt();
                assertTrue(radius >= 16 && radius <= 192, () -> "unsafe tracking radius in " + path);
                int requiredEvidence = json.get("required_evidence").getAsInt();
                assertTrue(requiredEvidence >= 2 && requiredEvidence <= 8,
                        () -> "unsafe evidence threshold in " + path);
                assertFalse(json.getAsJsonArray("incident_evidence").isEmpty(),
                        () -> "missing incident evidence in " + path);

                Set<String> signatures = new HashSet<>();
                json.getAsJsonArray("signatures").forEach(value -> signatures.add(value.getAsString()));
                json.getAsJsonArray("incident_evidence").forEach(value ->
                        assertTrue(signatures.contains(value.getAsString()),
                                () -> "incident produces undeclared signature in " + path));
                if (json.has("analysis_results")) {
                    json.getAsJsonObject("analysis_results").entrySet().forEach(entry ->
                            assertTrue(signatures.contains(entry.getValue().getAsString()),
                                    () -> entry.getKey() + " produces undeclared signature in " + path));
                    if (json.getAsJsonObject("analysis_results").has("FAE")) {
                        faeAnalysisShipped |= "GLAMOUR_TRACE".equals(
                                json.getAsJsonObject("analysis_results").get("FAE").getAsString());
                    }
                }
                revenantShipped |= "darkfolklore:revenant".equals(concept);
                wildHuntShipped |= "darkfolklore:wild_hunt".equals(concept);
            }
        }
        assertEquals(11, count, "0.8 ships eleven curated investigation profiles");
        assertTrue(revenantShipped, "0.8 must ship a Revenant forensic profile");
        assertTrue(wildHuntShipped, "0.8 must ship a Wild Hunt forensic profile");
        assertTrue(faeAnalysisShipped, "FAE must be a real shipped analysis path, not only an unused tool tag");

        for (String tradition : Set.of("witchcraft", "spirit", "soul", "forbidden_theurgy", "fae")) {
            Path toolTag = root.resolve("data/darkfolklore/tags/item/investigation_tools/" + tradition + ".json");
            assertTrue(Files.isRegularFile(toolTag), () -> "missing investigation tool tag " + tradition);
            assertFalse(read(toolTag).getAsJsonArray("values").isEmpty(), () -> "empty tool tag " + tradition);
        }
    }

    private static JsonObject read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
