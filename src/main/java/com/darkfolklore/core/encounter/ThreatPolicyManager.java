package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.math.BigDecimal;
import java.util.*;

/** Atomic datapack registry for DarkFolklore-owned encounter and ritual policies. */
public final class ThreatPolicyManager extends SimplePreparableReloadListener<ThreatPolicyManager.Prepared> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    public static final ThreatPolicyManager INSTANCE = new ThreatPolicyManager();

    private volatile Map<String, EncounterPolicy> encountersById = Map.of();
    private volatile Map<String, EncounterPolicy> encountersByEntity = Map.of();
    private volatile Map<String, RitualDefinition> rituals = Map.of();
    private volatile List<String> validationErrors = List.of();

    private ThreatPolicyManager() {}

    public Optional<EncounterPolicy> encounter(String id) { return Optional.ofNullable(encountersById.get(id)); }
    public Optional<EncounterPolicy> forEntity(String entityId) { return Optional.ofNullable(encountersByEntity.get(entityId)); }
    public Optional<RitualDefinition> ritual(String id) { return Optional.ofNullable(rituals.get(id)); }
    public Collection<EncounterPolicy> encounters() { return encountersById.values(); }
    public Collection<RitualDefinition> rituals() { return rituals.values(); }
    public List<String> validationErrors() { return validationErrors; }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<EncounterPolicy> encounters = new ArrayList<>();
        List<RitualDefinition> rituals = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        parseDirectory(manager, "darkfolklore/encounters", (id, json) -> encounters.add(parseEncounter(id, json)), errors);
        parseDirectory(manager, "darkfolklore/rituals", (id, json) -> rituals.add(parseRitual(id, json)), errors);
        return new Prepared(List.copyOf(encounters), List.copyOf(rituals), List.copyOf(errors));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        validationErrors = prepared.errors();
        if (!validationErrors.isEmpty()) {
            DarkFolkloreCore.LOGGER.error("[encounter-data] Reload rejected; retaining previous state ({} error(s))", validationErrors.size());
            return;
        }
        try {
            Map<String, EncounterPolicy> byId = new LinkedHashMap<>();
            Map<String, EncounterPolicy> byEntity = new LinkedHashMap<>();
            for (EncounterPolicy encounter : prepared.encounters()) {
                if (byId.putIfAbsent(encounter.id(), encounter) != null) throw new IllegalArgumentException("Duplicate encounter id " + encounter.id());
                if (byEntity.putIfAbsent(encounter.entityId(), encounter) != null) throw new IllegalArgumentException("Duplicate encounter entity " + encounter.entityId());
            }
            Map<String, RitualDefinition> ritualMap = new LinkedHashMap<>();
            for (RitualDefinition ritual : prepared.rituals()) {
                if (ritualMap.putIfAbsent(ritual.id(), ritual) != null) throw new IllegalArgumentException("Duplicate ritual id " + ritual.id());
                if (!byId.containsKey(ritual.encounterId())) throw new IllegalArgumentException("Ritual " + ritual.id() + " references unknown encounter " + ritual.encounterId());
            }
            encountersById = Map.copyOf(byId);
            encountersByEntity = Map.copyOf(byEntity);
            rituals = Map.copyOf(ritualMap);
            DarkFolkloreCore.LOGGER.info("[encounter-data] Loaded {} encounter policies and {} ritual definitions", byId.size(), ritualMap.size());
        } catch (RuntimeException exception) {
            validationErrors = List.of(exception.getMessage());
            DarkFolkloreCore.LOGGER.error("[encounter-data] Cross-definition validation failed; retaining previous state", exception);
        }
    }

    private static EncounterPolicy parseEncounter(ResourceLocation file, JsonObject json) {
        if (json.has("vitality_multiplier") || json.has("guaranteed_traits")) {
            DarkFolkloreCore.LOGGER.warn("[encounter-data] {} contains legacy direct-stat fields; they are ignored. "
                    + "Use l2_minimum_level for combat scaling.", file);
        }
        return new EncounterPolicy(string(json, "id", file.toString()), requiredString(json, "entity"),
                number(json, "natural_spawn_multiplier", 1.0D), integer(json, "minimum_encounter_pressure", 0),
                integer(json, "l2_minimum_level", 0));
    }

    private static RitualDefinition parseRitual(ResourceLocation file, JsonObject json) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        if (json.has("item_costs")) {
            JsonElement costsElement = json.get("item_costs");
            if (!costsElement.isJsonObject()) throw new IllegalArgumentException("item_costs must be an object");
            costsElement.getAsJsonObject().entrySet().forEach(entry ->
                    costs.put(entry.getKey(), exactInteger(entry.getValue(), "item_costs." + entry.getKey())));
        }
        return new RitualDefinition(string(json, "id", file.toString()), requiredString(json, "encounter"),
                integer(json, "required_knowledge_points", 0), longNumber(json, "cooldown_ticks", 0L),
                bool(json, "enabled", true), string(json, "focus_block", "minecraft:soul_campfire"),
                string(json, "activation_item", "minecraft:bone"), bool(json, "requires_night", true),
                integer(json, "spawn_count", 1), costs);
    }

    private static void parseDirectory(ResourceManager manager, String directory, JsonConsumer consumer, List<String> errors) {
        Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, directory, GSON, files);
        files.forEach((id, element) -> {
            try { consumer.accept(id, element.getAsJsonObject()); }
            catch (RuntimeException exception) { errors.add(id + ": " + exception.getMessage()); }
        });
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) throw new IllegalArgumentException("Missing required field " + key);
        String value = json.get(key).getAsString();
        if (value.isBlank()) throw new IllegalArgumentException("Required field " + key + " must not be blank");
        return value;
    }

    private static String string(JsonObject json, String key, String fallback) { return json.has(key) ? json.get(key).getAsString() : fallback; }
    private static double number(JsonObject json, String key, double fallback) { return json.has(key) ? json.get(key).getAsDouble() : fallback; }
    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? exactInteger(json.get(key), key) : fallback;
    }
    private static long longNumber(JsonObject json, String key, long fallback) {
        return json.has(key) ? exactLong(json.get(key), key) : fallback;
    }
    private static boolean bool(JsonObject json, String key, boolean fallback) { return json.has(key) ? json.get(key).getAsBoolean() : fallback; }

    private static int exactInteger(JsonElement element, String key) {
        try {
            BigDecimal value = element.getAsBigDecimal();
            return value.intValueExact();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(key + " must be an exact 32-bit integer", exception);
        }
    }

    private static long exactLong(JsonElement element, String key) {
        try {
            BigDecimal value = element.getAsBigDecimal();
            return value.longValueExact();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(key + " must be an exact 64-bit integer", exception);
        }
    }

    @FunctionalInterface
    private interface JsonConsumer { void accept(ResourceLocation id, JsonObject json); }

    public record Prepared(List<EncounterPolicy> encounters, List<RitualDefinition> rituals, List<String> errors) {}
}
