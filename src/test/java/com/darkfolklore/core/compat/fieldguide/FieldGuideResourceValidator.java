package com.darkfolklore.core.compat.fieldguide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure resource validation for the exact Field Guide 1.14.0 category schema. */
final class FieldGuideResourceValidator {
    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final Pattern ENTITY_ENTRY = Pattern.compile("entity:[a-z0-9_.-]+/[a-z0-9/._-]+");
    private static final Set<String> REQUIRED_LOCALES = Set.of("en_us", "it_it");
    private static final Set<String> AUDITED_FIELD_GUIDE_ICONS = Set.of(
            "fieldguide:textures/gui/icons/ghast.png",
            "fieldguide:textures/gui/icons/ore.png",
            "fieldguide:textures/gui/icons/spider.png",
            "fieldguide:textures/gui/icons/wither.png",
            "fieldguide:textures/gui/icons/wolf.png"
    );

    private FieldGuideResourceValidator() {}

    static Content load(Path resourcesRoot) throws IOException {
        Map<String, JsonObject> categories = new LinkedHashMap<>();
        Path categoryRoot = resourcesRoot.resolve("data/darkfolklore/fieldguide/categories");
        try (var paths = Files.list(categoryRoot)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String fileName = path.getFileName().toString();
                String categoryPath = fileName.substring(0, fileName.length() - ".json".length());
                categories.put("darkfolklore:" + categoryPath, readObject(path));
            }
        }

        List<JsonObject> canonicalDefinitions = new ArrayList<>();
        Path canonicalRoot = resourcesRoot.resolve("data/darkfolklore/darkfolklore/canonical");
        try (var paths = Files.list(canonicalRoot)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                canonicalDefinitions.add(readObject(path));
            }
        }

        Map<String, Map<String, String>> languages = new LinkedHashMap<>();
        Path languageRoot = resourcesRoot.resolve("assets/darkfolklore/lang");
        for (String locale : REQUIRED_LOCALES) {
            Path path = languageRoot.resolve(locale + ".json");
            if (!Files.isRegularFile(path)) continue;
            JsonObject json = readObject(path);
            Map<String, String> translations = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            languages.put(locale, translations);
        }
        return new Content(categories, canonicalDefinitions, languages);
    }

    static ValidationReport validate(Content content) {
        List<String> problems = new ArrayList<>();
        Map<String, Set<String>> canonicalMembers = canonicalMembers(content.canonicalDefinitions(), problems);
        Set<String> declaredCategoryIds = content.categories().keySet();
        Set<String> seenEntries = new HashSet<>();
        Set<String> expectedCategoryTranslations = new LinkedHashSet<>();
        Set<String> expectedEntryTranslations = new LinkedHashSet<>();
        int categoryCount = 0;
        int entryCount = 0;
        int invalidCategories = 0;
        int emptyCategories = 0;
        int invalidEntries = 0;
        int duplicateEntryIds = 0;
        int missingCanonicalConcepts = 0;
        int unresolvedMappings = 0;

        for (Map.Entry<String, JsonObject> categoryFile : content.categories().entrySet()) {
            String categoryId = categoryFile.getKey();
            JsonObject category = categoryFile.getValue();
            if (category.has("target_category")) {
                categoryId = string(category, "target_category");
                if (!validResourceLocation(categoryId) || !declaredCategoryIds.contains(categoryId)) {
                    invalidCategories++;
                    problems.add(categoryFile.getKey() + " points to invalid category " + categoryId);
                }
            }

            boolean hidden = category.has("hidden") && category.get("hidden").getAsBoolean();
            if (hidden) continue;
            categoryCount++;
            if (!validResourceLocation(categoryId)) {
                invalidCategories++;
                problems.add("Malformed category ID " + categoryId);
            } else {
                expectedCategoryTranslations.add(categoryTranslationKey(categoryId));
            }

            String icon = string(category, "icon");
            if (!AUDITED_FIELD_GUIDE_ICONS.contains(icon)) {
                invalidCategories++;
                problems.add(categoryId + " references unaudited or missing Field Guide icon " + icon);
            }

            JsonArray entries = category.has("contents") && category.get("contents").isJsonArray()
                    ? category.getAsJsonArray("contents") : null;
            if (entries == null) {
                emptyCategories++;
                invalidEntries++;
                problems.add(categoryId + " has no contents array");
                continue;
            }

            int validEntriesInCategory = 0;
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) {
                    invalidEntries++;
                    problems.add(categoryId + " contains a non-object entry");
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                if (!"entry".equals(string(entry, "type"))) {
                    invalidEntries++;
                    problems.add(categoryId + " contains non-curated content type " + string(entry, "type"));
                    continue;
                }

                String entryId = string(entry, "id");
                String concept = string(entry, "canonical_concept");
                if (!ENTITY_ENTRY.matcher(entryId).matches()) {
                    invalidEntries++;
                    problems.add(categoryId + " has malformed entity entry ID " + entryId);
                    continue;
                }

                entryCount++;
                validEntriesInCategory++;
                if (!seenEntries.add(entryId)) {
                    duplicateEntryIds++;
                    problems.add("Duplicate Field Guide entry " + entryId);
                }
                if (!hasBinaryTriggers(entry)) {
                    invalidEntries++;
                    problems.add(entryId + " must declare both SCAN and KILL binary unlock triggers");
                }

                Set<String> members = canonicalMembers.get(concept);
                if (members == null) {
                    missingCanonicalConcepts++;
                    problems.add(entryId + " references missing canonical concept " + concept);
                } else {
                    String entityId = rawEntityId(entryId);
                    if (!members.contains(entityId)) {
                        unresolvedMappings++;
                        problems.add(entryId + " does not resolve through " + concept);
                    }
                }

                expectedEntryTranslations.add(entryNameKey(entryId));
                expectedEntryTranslations.add(entryDescriptionKey(entryId));
            }
            if (validEntriesInCategory == 0) {
                emptyCategories++;
                problems.add(categoryId + " is an empty visible category");
            }
        }

        int missingCategoryTranslations = 0;
        int missingEntryTranslations = 0;
        int orphanEntries = 0;
        Set<String> allExpectedTranslations = new HashSet<>(expectedCategoryTranslations);
        allExpectedTranslations.addAll(expectedEntryTranslations);
        for (String locale : REQUIRED_LOCALES) {
            Map<String, String> language = content.languages().getOrDefault(locale, Map.of());
            for (String key : expectedCategoryTranslations) {
                if (missingOrRaw(language.get(key), key)) {
                    missingCategoryTranslations++;
                    problems.add(locale + " is missing category translation " + key);
                }
            }
            for (String key : expectedEntryTranslations) {
                if (missingOrRaw(language.get(key), key)) {
                    missingEntryTranslations++;
                    problems.add(locale + " is missing entry translation " + key);
                }
            }
            for (String key : language.keySet()) {
                if (isDarkFolkloreFieldGuideKey(key) && !allExpectedTranslations.contains(key)) {
                    orphanEntries++;
                    problems.add(locale + " contains orphan Field Guide translation " + key);
                }
            }
        }

        return new ValidationReport(categoryCount, entryCount, missingCategoryTranslations,
                missingEntryTranslations, orphanEntries, invalidCategories, duplicateEntryIds,
                missingCanonicalConcepts, unresolvedMappings, emptyCategories, invalidEntries,
                List.copyOf(problems));
    }

    private static Map<String, Set<String>> canonicalMembers(List<JsonObject> definitions, List<String> problems) {
        Map<String, Set<String>> result = new HashMap<>();
        for (JsonObject definition : definitions) {
            if (!"ENTITY".equals(string(definition, "kind"))) continue;
            String concept = string(definition, "concept");
            if (!validResourceLocation(concept)) {
                problems.add("Malformed canonical entity concept " + concept);
                continue;
            }
            Set<String> members = new LinkedHashSet<>();
            String canonical = string(definition, "canonical");
            if (!canonical.isBlank()) members.add(canonical);
            if (definition.has("implementations") && definition.get("implementations").isJsonArray()) {
                for (JsonElement implementation : definition.getAsJsonArray("implementations")) {
                    members.add(implementation.getAsString());
                }
            }
            if (result.putIfAbsent(concept, Set.copyOf(members)) != null) {
                problems.add("Duplicate canonical entity concept " + concept);
            }
        }
        return result;
    }

    private static boolean hasBinaryTriggers(JsonObject entry) {
        if (!entry.has("unlock") || !entry.get("unlock").isJsonObject()) return false;
        JsonObject unlock = entry.getAsJsonObject("unlock");
        if (!unlock.has("triggers") || !unlock.get("triggers").isJsonArray()) return false;
        Set<String> triggers = new HashSet<>();
        for (JsonElement trigger : unlock.getAsJsonArray("triggers")) {
            triggers.add(trigger.getAsString().toUpperCase());
        }
        return triggers.contains("SCAN") && triggers.contains("KILL");
    }

    private static JsonObject readObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) throw new IOException(path + " must contain a JSON object");
            return parsed.getAsJsonObject();
        }
    }

    private static boolean validResourceLocation(String value) {
        return value != null && RESOURCE_LOCATION.matcher(value).matches();
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static String rawEntityId(String entryId) {
        String path = entryId.substring("entity:".length());
        int separator = path.indexOf('/');
        return path.substring(0, separator) + ':' + path.substring(separator + 1);
    }

    private static String categoryTranslationKey(String categoryId) {
        int separator = categoryId.indexOf(':');
        return "category." + categoryId.substring(0, separator) + ".fieldguide." + categoryId.substring(separator + 1);
    }

    private static String entryNameKey(String entryId) {
        return "fieldguide.name.entity." + entryId.substring("entity:".length()).replace('/', '.');
    }

    private static String entryDescriptionKey(String entryId) {
        return "fieldguide.entity." + entryId.substring("entity:".length()).replace('/', '.') + ".description";
    }

    private static boolean isDarkFolkloreFieldGuideKey(String key) {
        return key.startsWith("category.darkfolklore.fieldguide.")
                || key.startsWith("fieldguide.name.entity.")
                || key.startsWith("fieldguide.entity.") && key.endsWith(".description");
    }

    private static boolean missingOrRaw(String value, String key) {
        return value == null || value.isBlank() || value.equals(key);
    }

    record Content(Map<String, JsonObject> categories, List<JsonObject> canonicalDefinitions,
                   Map<String, Map<String, String>> languages) {}

    record ValidationReport(int categories, int entries, int missingCategoryTranslations,
                            int missingEntryTranslations, int orphanEntries, int invalidCategories,
                            int duplicateEntryIds, int missingCanonicalConcepts, int unresolvedMappings,
                            int emptyCategories, int invalidEntries, List<String> problems) {
        boolean isClean() {
            return missingCategoryTranslations == 0 && missingEntryTranslations == 0
                    && orphanEntries == 0 && invalidCategories == 0 && duplicateEntryIds == 0
                    && missingCanonicalConcepts == 0 && unresolvedMappings == 0
                    && emptyCategories == 0 && invalidEntries == 0 && problems.isEmpty();
        }
    }
}
