package com.darkfolklore.core.data;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.canonical.*;
import com.darkfolklore.core.investigation.InvestigationProfile;
import com.darkfolklore.core.magic.MagicIntegrationDefinition;
import com.darkfolklore.core.magic.MagicTradition;
import com.darkfolklore.core.spawn.*;
import com.darkfolklore.core.society.story.StoryTemplateDefinition;
import com.darkfolklore.core.society.story.StoryTrigger;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.compat.mca.McaTrustSettings;
import com.darkfolklore.core.compat.mcacapitals.*;
import com.darkfolklore.core.society.organization.*;
import com.darkfolklore.core.traits.CreatureTrait;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.weakness.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public final class FolkloreDataManager extends SimplePreparableReloadListener<FolkloreDataManager.Prepared> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    public static final FolkloreDataManager INSTANCE = new FolkloreDataManager();

    private final ValidatedReloadState<State> state = new ValidatedReloadState<>(State.empty());
    private volatile List<String> validationErrors = List.of();

    private FolkloreDataManager() {}

    public CanonicalRegistry canonical() { return state.get().canonical(); }
    public WeaknessRegistry weaknesses() { return state.get().weaknesses(); }
    public SpawnProfileRegistry spawns() { return state.get().spawns(); }
    public List<MagicIntegrationDefinition> magic() { return state.get().magic(); }
    public Collection<InvestigationProfile> investigationProfiles() {
        return state.get().investigationProfiles().values();
    }
    public Optional<InvestigationProfile> investigationProfile(String concept) {
        return Optional.ofNullable(state.get().investigationProfiles().get(concept));
    }
    public List<StoryTemplateDefinition> storyTemplates() { return state.get().storyTemplates(); }
    public Optional<OrganizationArchetypeDefinition> organizationArchetype(OrganizationType type) {
        return Optional.ofNullable(state.get().organizationArchetypes().get(type));
    }
    public McaTrustSettings socialTrustSettings() { return state.get().socialTrustSettings(); }
    public PoliticalWeights politicalWeights(PoliticalRole role) {
        return Optional.ofNullable(state.get().politicalWeights().get(role))
                .orElseGet(() -> PoliticalWeightModel.weights(role));
    }
    public List<String> validationErrors() { return validationErrors; }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<CanonicalDefinition> canonicalDefinitions = new ArrayList<>();
        List<WeaknessRule> weaknessRules = new ArrayList<>();
        List<SpawnProfile> spawnProfiles = new ArrayList<>();
        List<MagicIntegrationDefinition> magicDefinitions = new ArrayList<>();
        List<InvestigationProfile> investigationProfiles = new ArrayList<>();
        List<StoryTemplateDefinition> storyTemplates = new ArrayList<>();
        List<OrganizationArchetypeDefinition> organizationArchetypes = new ArrayList<>();
        List<McaTrustSettings> socialParameters = new ArrayList<>();
        List<PoliticalWeightDefinition> politicalWeights = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        parseDirectory(manager, "darkfolklore/canonical", (id, json) ->
                canonicalDefinitions.add(parseCanonical(id, json)), errors);
        parseDirectory(manager, "darkfolklore/weaknesses", (id, json) ->
                weaknessRules.add(parseWeakness(id, json)), errors);
        parseDirectory(manager, "darkfolklore/spawn_profiles", (id, json) ->
                spawnProfiles.add(parseSpawn(id, json)), errors);
        parseDirectory(manager, "darkfolklore/magic_integrations", (id, json) ->
                magicDefinitions.add(parseMagic(id, json)), errors);
        parseDirectory(manager, "darkfolklore/investigation_profiles", (id, json) ->
                investigationProfiles.add(parseInvestigationProfile(id, json)), errors);
        parseDirectory(manager, "darkfolklore/story_templates", (id, json) ->
                storyTemplates.add(parseStoryTemplate(id, json)), errors);
        parseDirectory(manager, "darkfolklore/organization_archetypes", (id, json) ->
                organizationArchetypes.add(parseOrganizationArchetype(id, json)), errors);
        parseDirectory(manager, "darkfolklore/social_parameters", (id, json) ->
                socialParameters.add(parseSocialParameters(id, json)), errors);
        parseDirectory(manager, "darkfolklore/political_weights", (id, json) ->
                politicalWeights.add(parsePoliticalWeight(id, json)), errors);
        State candidate = null;
        if (errors.isEmpty()) {
            try {
                CanonicalRegistry canonical = new CanonicalRegistry();
                canonical.replace(canonicalDefinitions);
                WeaknessRegistry weaknesses = new WeaknessRegistry();
                validateUnique(weaknessRules.stream().map(WeaknessRule::id).toList(), "weakness id");
                weaknesses.replace(weaknessRules);
                SpawnProfileRegistry spawns = new SpawnProfileRegistry();
                spawns.replace(spawnProfiles);
                validateUnique(magicDefinitions.stream().map(MagicIntegrationDefinition::id).toList(),
                        "magic integration id");
                validateUnique(investigationProfiles.stream().map(InvestigationProfile::concept).toList(),
                        "investigation profile concept");
                validateUnique(storyTemplates.stream().map(StoryTemplateDefinition::id).toList(),
                        "story template id");
                validateUnique(organizationArchetypes.stream().map(value -> value.type().name()).toList(),
                        "organization archetype type");
                if (socialParameters.size() > 1) {
                    throw new IllegalArgumentException("Only one social parameter definition is allowed");
                }
                validateUnique(politicalWeights.stream().map(value -> value.role().name()).toList(),
                        "political role weight");
                Map<OrganizationType, OrganizationArchetypeDefinition> archetypes = new EnumMap<>(OrganizationType.class);
                organizationArchetypes.forEach(value -> archetypes.put(value.type(), value));
                Map<PoliticalRole, PoliticalWeights> weights = new EnumMap<>(PoliticalRole.class);
                politicalWeights.forEach(value -> weights.put(value.role(), value.weights()));
                Map<String, InvestigationProfile> investigations = new LinkedHashMap<>();
                investigationProfiles.forEach(value -> {
                    if (canonical.concept(value.concept()).isEmpty()) {
                        throw new IllegalArgumentException("Investigation profile references missing canonical concept "
                                + value.concept());
                    }
                    investigations.put(value.concept(), value);
                });
                candidate = new State(canonical, weaknesses, spawns, List.copyOf(magicDefinitions),
                        Map.copyOf(investigations), List.copyOf(storyTemplates), Map.copyOf(archetypes),
                        socialParameters.isEmpty() ? McaTrustSettings.DEFAULTS : socialParameters.getFirst(),
                        Map.copyOf(weights));
            } catch (RuntimeException exception) {
                errors.add("cross-definition validation: " + exception.getMessage());
            }
        }
        return new Prepared(candidate, List.copyOf(errors));
    }

    private static void parseDirectory(ResourceManager manager, String directory,
                                       JsonConsumer consumer, List<String> errors) {
        Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, directory, GSON, files);
        files.forEach((id, json) -> {
            try {
                consumer.accept(id, json.getAsJsonObject());
            } catch (RuntimeException exception) {
                String message = id + ": " + exception.getMessage();
                errors.add(message);
                DarkFolkloreCore.LOGGER.error("[data] Invalid definition {}", message);
            }
        });
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        validationErrors = List.copyOf(prepared.errors());
        if (!state.apply(prepared.candidate(), validationErrors)) {
            DarkFolkloreCore.LOGGER.error("[data] Reload rejected atomically; retaining previous validated state "
                    + "because {} definition error(s) were found", validationErrors.size());
            return;
        }
        State loaded = state.get();
        DarkFolkloreCore.LOGGER.info("[data] Atomically loaded {} canonical concepts, {} weaknesses, {} spawn profiles, "
                        + "{} magic integrations, {} investigation profiles, {} story templates, {} organization archetypes, "
                        + "{} political overrides (0 invalid)",
                loaded.canonical().definitions().size(), loaded.weaknesses().rules().size(),
                loaded.spawns().profiles().size(), loaded.magic().size(), loaded.investigationProfiles().size(),
                loaded.storyTemplates().size(), loaded.organizationArchetypes().size(), loaded.politicalWeights().size());
    }

    private static CanonicalDefinition parseCanonical(ResourceLocation file, JsonObject json) {
        String concept = string(json, "concept", file.toString());
        CanonicalKind kind = enumValue(CanonicalKind.class, string(json, "kind", "CONCEPT"));
        String canonicalId = string(json, "canonical", "");
        List<String> implementations = strings(json, "implementations");
        CanonicalPolicy policy = enumValue(CanonicalPolicy.class, string(json, "policy", "CANONICAL"));
        return new CanonicalDefinition(concept, kind, canonicalId, implementations, policy,
                string(json, "reason", ""));
    }

    private static WeaknessRule parseWeakness(ResourceLocation file, JsonObject json) {
        Set<CreatureTrait> targets = enumSet(CreatureTrait.class, json, "target_traits");
        Set<ItemTrait> items = enumSet(ItemTrait.class, json, "item_traits");
        return new WeaknessRule(string(json, "id", file.toString()), targets, items,
                number(json, "multiplier", 1.0F), Set.copyOf(strings(json, "native_provider_namespaces")),
                integer(json, "priority", 0));
    }

    private static SpawnProfile parseSpawn(ResourceLocation file, JsonObject json) {
        return new SpawnProfile(string(json, "entity", file.toString()),
                enumValue(SpawnRarity.class, string(json, "rarity", "RARE")),
                bool(json, "natural_spawn_enabled", true), bool(json, "canonicalization_suppression", false),
                bool(json, "nocturnal", false),
                number(json, "event_multiplier", 1.0F));
    }

    private static MagicIntegrationDefinition parseMagic(ResourceLocation file, JsonObject json) {
        return new MagicIntegrationDefinition(string(json, "id", file.toString()),
                enumSet(MagicTradition.class, json, "traditions"),
                enumSet(ItemTrait.class, json, "required_traits"),
                string(json, "knowledge_reward", "darkfolklore:magic"),
                integer(json, "knowledge_points", 5));
    }

    private static InvestigationProfile parseInvestigationProfile(ResourceLocation file, JsonObject json) {
        String concept = string(json, "concept", file.toString());
        Set<CreatureTrait> creatureTraits = enumSet(CreatureTrait.class, json, "creature_traits");
        Set<EvidenceType> signatures = enumSet(EvidenceType.class, json, "signatures");
        EnumMap<MagicTradition, EvidenceType> analyses = new EnumMap<>(MagicTradition.class);
        if (json.has("analysis_results")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("analysis_results").entrySet()) {
                analyses.put(enumValue(MagicTradition.class, entry.getKey()),
                        enumValue(EvidenceType.class, entry.getValue().getAsString()));
            }
        }
        List<EvidenceType> incidentEvidence = strings(json, "incident_evidence").stream()
                .map(value -> enumValue(EvidenceType.class, value)).toList();
        return new InvestigationProfile(concept, creatureTraits, signatures, analyses, incidentEvidence,
                integer(json, "required_evidence", 3), integer(json, "tracking_radius", 96));
    }

    private static StoryTemplateDefinition parseStoryTemplate(ResourceLocation file, JsonObject json) {
        Optional<SecretType> requiredSecret = json.has("required_secret")
                ? Optional.of(enumValue(SecretType.class, json.get("required_secret").getAsString()))
                : Optional.empty();
        return new StoryTemplateDefinition(string(json, "id", file.toString()),
                enumValue(StoryTrigger.class, string(json, "trigger", "WITCHING_HOUR")),
                string(json, "concept", "*"), integer(json, "weight", 1),
                longNumber(json, "cooldown_ticks", 24000L), longNumber(json, "lifetime_ticks", 144000L),
                requiredSecret, bool(json, "capital_only", false), bool(json, "contract_eligible", false),
                bool(json, "enabled", true));
    }

    private static OrganizationArchetypeDefinition parseOrganizationArchetype(ResourceLocation file,
                                                                                JsonObject json) {
        return new OrganizationArchetypeDefinition(
                enumValue(OrganizationType.class, string(json, "type", file.getPath())),
                integer(json, "base_influence", 5), integer(json, "max_members", 32),
                bool(json, "auto_found", false), bool(json, "public_reveal_authority", false),
                enumSet(OrganizationObjective.class, json, "objectives"));
    }

    private static McaTrustSettings parseSocialParameters(ResourceLocation file, JsonObject json) {
        return new McaTrustSettings(number(json, "self", 0.30F), number(json, "spouse", 0.25F),
                number(json, "parent_or_child", 0.22F), number(json, "sibling", 0.18F),
                number(json, "player_friend", 0.12F), number(json, "player_bounty_target", -0.20F));
    }

    private static PoliticalWeightDefinition parsePoliticalWeight(ResourceLocation file, JsonObject json) {
        PoliticalRole role = enumValue(PoliticalRole.class, string(json, "role", file.getPath()));
        PoliticalWeights weights = new PoliticalWeights(number(json, "credibility", 0.0F),
                number(json, "organization_response", 0.0F), number(json, "investigation_priority", 0.0F),
                number(json, "public_awareness", 0.0F));
        return new PoliticalWeightDefinition(role, weights);
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static float number(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    private static long longNumber(JsonObject json, String key, long fallback) {
        return json.has(key) ? json.get(key).getAsLong() : fallback;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static List<String> strings(JsonObject json, String key) {
        if (!json.has(key)) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement value : json.getAsJsonArray(key)) result.add(value.getAsString());
        return result;
    }

    private static <E extends Enum<E>> Set<E> enumSet(Class<E> type, JsonObject json, String key) {
        EnumSet<E> result = EnumSet.noneOf(type);
        for (String value : strings(json, key)) result.add(enumValue(type, value));
        return result;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    private static void validateUnique(Collection<String> values, String label) {
        Set<String> unique = new HashSet<>();
        for (String value : values) {
            if (!unique.add(value)) throw new IllegalArgumentException("Duplicate " + label + " " + value);
        }
    }

    @FunctionalInterface
    private interface JsonConsumer {
        void accept(ResourceLocation id, JsonObject object);
    }

    protected record Prepared(State candidate, List<String> errors) {}

    protected record State(CanonicalRegistry canonical, WeaknessRegistry weaknesses,
                           SpawnProfileRegistry spawns, List<MagicIntegrationDefinition> magic,
                           Map<String, InvestigationProfile> investigationProfiles,
                           List<StoryTemplateDefinition> storyTemplates,
                           Map<OrganizationType, OrganizationArchetypeDefinition> organizationArchetypes,
                           McaTrustSettings socialTrustSettings,
                           Map<PoliticalRole, PoliticalWeights> politicalWeights) {
        private static State empty() {
            return new State(new CanonicalRegistry(), new WeaknessRegistry(),
                    new SpawnProfileRegistry(), List.of(), Map.of(), List.of(), Map.of(),
                    McaTrustSettings.DEFAULTS, Map.of());
        }
    }
}
