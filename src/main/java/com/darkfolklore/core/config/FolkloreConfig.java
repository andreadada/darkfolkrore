package com.darkfolklore.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FolkloreConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CANONICALIZATION = toggle("canonicalization", true);
    public static final ModConfigSpec.BooleanValue WEAKNESSES = toggle("weaknesses", true);
    public static final ModConfigSpec.BooleanValue KNOWLEDGE = toggle("knowledge", true);
    public static final ModConfigSpec.BooleanValue SOCIAL_KNOWLEDGE = toggle("socialKnowledge", true);
    public static final ModConfigSpec.BooleanValue WITNESSES = toggle("witnesses", true);
    public static final ModConfigSpec.BooleanValue RUMORS = toggle("rumors", true);
    public static final ModConfigSpec.BooleanValue ORGANIZATIONS = toggle("organizations", true);
    public static final ModConfigSpec.BooleanValue VILLAGE_SOCIETY = toggle("villageSociety", true);
    public static final ModConfigSpec.BooleanValue SPAWN_DIRECTOR = toggle("spawnDirector", true);
    public static final ModConfigSpec.BooleanValue ENCOUNTER_DIRECTOR = toggle("encounterDirector", true);
    public static final ModConfigSpec.BooleanValue RITUALS = toggle("rituals", true);
    public static final ModConfigSpec.BooleanValue L2_HOSTILITY_INTEGRATION = toggle("l2HostilityIntegration", true);
    public static final ModConfigSpec.BooleanValue CONTRACTS = toggle("contracts", true);
    public static final ModConfigSpec.BooleanValue DYNAMIC_STORIES = toggle("dynamicStories", true);
    public static final ModConfigSpec.BooleanValue WORLD_EVENTS = toggle("worldEvents", true);
    public static final ModConfigSpec.BooleanValue RELATIONSHIP_TRUST = toggle("relationshipTrust", true);
    public static final ModConfigSpec.BooleanValue PERSONALITY_MODIFIERS = toggle("personalityModifiers", true);
    public static final ModConfigSpec.BooleanValue FAMILY_SECRETS = toggle("familySecrets", true);
    public static final ModConfigSpec.BooleanValue ORGANIZATION_BEHAVIOR = toggle("organizationBehavior", true);
    public static final ModConfigSpec.BooleanValue MCA_CAPITALS = toggle("mcaCapitalsIntegration", true);
    public static final ModConfigSpec.BooleanValue FALSE_ACCUSATIONS = toggle("falseAccusations", true);
    public static final ModConfigSpec.BooleanValue OCCULT_INVESTIGATION = toggle("occultInvestigation", true);
    public static final ModConfigSpec.BooleanValue PREPARED_HUNT_BONUS = toggle("preparedHuntBonus", true);
    public static final ModConfigSpec.BooleanValue VAMPIRE_PREDATION = toggle("vampirePredation", true);
    public static final ModConfigSpec.BooleanValue VAMPIRE_BEHAVIOR_PROFILES = toggle("vampireBehaviorProfiles", true);
    public static final ModConfigSpec.BooleanValue MCA_VAMPIRE_LIFECYCLE = toggle("mcaVampireLifecycle", true);

    public static final ModConfigSpec.BooleanValue LEGENDARY_ENCOUNTERS = toggle("legendaryEncounters", true);
    public static final ModConfigSpec.BooleanValue WARDS = toggle("wards", true);
    public static final ModConfigSpec.BooleanValue FAE_BARGAINS = toggle("faeBargains", true);
    public static final ModConfigSpec.BooleanValue TROPHY_RESEARCH = toggle("trophyResearch", true);
    /** Legacy 0.8 switch retained for existing configs; active combat scaling is owned by L2 via level floors. */
    public static final ModConfigSpec.BooleanValue L2_HOSTILITY_SCALING = toggle("l2HostilityScaling", false);
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = toggle("debugLogging", false);

    public static final ModConfigSpec.IntValue WITNESS_RADIUS = BUILDER.comment("Maximum event-driven witness radius in blocks")
            .defineInRange("witnessRadius", 24, 4, 64);
    public static final ModConfigSpec.IntValue MAX_WITNESSES = BUILDER.comment("Hard cap on observers evaluated per incident")
            .defineInRange("maxWitnessesPerIncident", 32, 1, 128);
    public static final ModConfigSpec.IntValue RUMOR_INTERVAL = BUILDER.comment("Ticks between bounded rumor queue batches")
            .defineInRange("rumorIntervalTicks", 100, 20, 24000);
    public static final ModConfigSpec.IntValue RUMORS_PER_BATCH = BUILDER.comment("Maximum rumor transmissions in one batch")
            .defineInRange("rumorsPerBatch", 8, 1, 64);
    public static final ModConfigSpec.DoubleValue RUMOR_CHANCE = BUILDER.comment("Base probability for a nearby social retelling")
            .defineInRange("rumorPropagationChance", 0.35D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue RUMOR_HALF_LIFE = BUILDER.comment("Rumor confidence half-life in ticks")
            .defineInRange("rumorHalfLifeTicks", 72000, 1200, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue STORY_COOLDOWN = BUILDER.comment("Minimum ticks between incidents in one village region")
            .defineInRange("storyCooldownTicks", 24000, 1200, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue ENCOUNTER_COOLDOWN = BUILDER.comment("Ticks over which encounter pressure naturally relaxes")
            .defineInRange("encounterCooldownTicks", 12000, 200, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue SPAWN_MULTIPLIER = BUILDER.comment("Global multiplier applied to curated natural spawn chances")
            .defineInRange("naturalSpawnMultiplier", 1.0D, 0.0D, 4.0D);
    public static final ModConfigSpec.DoubleValue HOSTILE_NATURAL_SPAWN_MULTIPLIER = BUILDER.comment(
            "Fallback natural-spawn multiplier for hostile Monster entities without an explicit encounter policy")
            .defineInRange("hostileNaturalSpawnMultiplier", 0.04D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue L2_GENERIC_MIN_LEVEL = BUILDER.comment(
            "Minimum L2 Hostility level requested for generic hostile encounters when audited L2 Hostility is installed")
            .defineInRange("l2GenericMinimumLevel", 20, 0, 500);
    public static final ModConfigSpec.IntValue EVIDENCE_LIFETIME = BUILDER.comment("Logical evidence lifetime in ticks")
            .defineInRange("evidenceLifetimeTicks", 24000, 1200, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CONTRACT_LIFETIME = BUILDER.comment("Monster contract lifetime in ticks")
            .defineInRange("contractLifetimeTicks", 72000, 2400, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue HISTORY_RETENTION = BUILDER.comment("Ticks to retain completed/expired story and contract history after its deadline")
            .defineInRange("terminalHistoryRetentionTicks", 168000, 24000, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue POLITICAL_RUMOR_WEIGHT = BUILDER.comment("Maximum multiplier contributed by verified MCA Capitals political roles")
            .defineInRange("politicalRumorWeight", 1.25D, 1.0D, 2.0D);
    public static final ModConfigSpec.DoubleValue DYNAMIC_STORY_RATE = BUILDER.comment("Probability that an eligible bounded social event creates an optional story")
            .defineInRange("dynamicStoryRate", 0.20D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue ORGANIZATION_RECRUITMENT_RATE = BUILDER.comment("Probability that an eligible NPC joins a compatible local organization")
            .defineInRange("organizationRecruitmentRate", 0.35D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue PUBLIC_REVEAL_WITNESSES = BUILDER.comment("Distinct confirmed observers needed before an explicit public reveal")
            .defineInRange("publicRevealWitnesses", 3, 2, 16);
    public static final ModConfigSpec.DoubleValue PUBLIC_REVEAL_CONFIDENCE = BUILDER.comment("Minimum average confidence for automatic public-reveal eligibility")
            .defineInRange("publicRevealAverageConfidence", 0.75D, 0.5D, 1.0D);
    public static final ModConfigSpec.IntValue ORGANIZATION_MAINTENANCE_BUDGET = BUILDER.comment("Maximum organizations processed during one maintenance pass")
            .defineInRange("organizationMaintenanceBudget", 16, 1, 128);
    public static final ModConfigSpec.IntValue MAX_SOCIAL_RECORDS = BUILDER.comment("Hard safety cap for persisted observer-specific beliefs")
            .defineInRange("maxSocialKnowledgeRecords", 50000, 1000, 500000);
    public static final ModConfigSpec.IntValue MAX_ORGANIZATIONS = BUILDER.comment("Hard safety cap for persisted supernatural social organizations")
            .defineInRange("maxOrganizations", 512, 16, 4096);
    public static final ModConfigSpec.IntValue OCCULT_ANALYSIS_RADIUS = BUILDER.comment("Maximum distance in blocks from a clue for occult analysis")
            .defineInRange("occultAnalysisRadius", 6, 2, 16);
    public static final ModConfigSpec.IntValue TRACKING_RADIUS = BUILDER.comment("Maximum loaded-area radius in blocks for an explicit monster-tracking pulse")
            .defineInRange("monsterTrackingRadius", 96, 16, 192);
    public static final ModConfigSpec.IntValue TRACKING_COOLDOWN = BUILDER.comment("Ticks between explicit monster-tracking pulses")
            .defineInRange("monsterTrackingCooldownTicks", 80, 20, 1200);

    public static final ModConfigSpec.IntValue LEGENDARY_MAX_ACTIVE = BUILDER.comment(
            "Maximum simultaneous non-terminal Legendary Encounter records across the server")
            .defineInRange("legendaryMaxActive", 4, 1, 16);
    public static final ModConfigSpec.IntValue LIVESTOCK_PANIC_THRESHOLD = BUILDER.comment(
            "Confirmed nighttime livestock deaths in one region before a Chupacabra story becomes eligible")
            .defineInRange("livestockPanicThreshold", 3, 2, 12);
    public static final ModConfigSpec.DoubleValue WENDIGO_DAILY_CHANCE = BUILDER.comment(
            "Stable per region/day chance for a missing-traveller Wendigo origin while players are present at night")
            .defineInRange("wendigoDailyChance", 0.015D, 0.0D, 0.25D);
    public static final ModConfigSpec.DoubleValue WILD_HUNT_FULL_MOON_CHANCE = BUILDER.comment(
            "Stable per region/full-moon-night chance for a Wild Hunt omen")
            .defineInRange("wildHuntFullMoonChance", 0.12D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue REVENANT_DEATH_CHANCE = BUILDER.comment(
            "Stable chance that a confirmed violent villager/MCA death seeds a returned-dead story")
            .defineInRange("revenantDeathChance", 0.08D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue WARD_LIFETIME = BUILDER.comment(
            "Lifetime in ticks of an explicit Dark Folklore threshold ward")
            .defineInRange("wardLifetimeTicks", 168000, 1200, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue FAE_BARGAIN_COOLDOWN = BUILDER.comment(
            "Regional cooldown in ticks between accepted fae offerings")
            .defineInRange("faeBargainCooldownTicks", 24000, 1200, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue VAMPIRE_PREDATION_SCAN_INTERVAL = BUILDER.comment(
            "Ticks between staggered prey evaluations for an eligible vampire; each entity is phase-offset")
            .defineInRange("vampirePredationScanIntervalTicks", 40, 20, 400);
    public static final ModConfigSpec.IntValue VAMPIRE_PREDATION_RADIUS = BUILDER.comment(
            "Loaded-area prey search radius; predation never force-loads chunks")
            .defineInRange("vampirePredationRadius", 12, 4, 32);
    public static final ModConfigSpec.IntValue VAMPIRE_PREDATION_COOLDOWN = BUILDER.comment(
            "Minimum ticks before one vampire can complete another directed feeding attempt")
            .defineInRange("vampirePredationCooldownTicks", 600, 100, 24000);
    public static final ModConfigSpec.IntValue VAMPIRE_PREDATION_LOCAL_WINDOW = BUILDER.comment(
            "Rolling local anti-chaos window for completed feeding events")
            .defineInRange("vampirePredationLocalWindowTicks", 2400, 200, 24000);
    public static final ModConfigSpec.IntValue VAMPIRE_PREDATION_MAX_LOCAL_FEEDS = BUILDER.comment(
            "Maximum completed feedings/lethal predation incidents in one village-region during the anti-chaos window")
            .defineInRange("vampirePredationMaxLocalFeeds", 4, 1, 32);
    public static final ModConfigSpec.DoubleValue VAMPIRE_PREDATOR_KILL_CHANCE = BUILDER.comment(
            "Stable per predator/victim/day chance that a hungry PREDATOR keeps attacking after feeding")
            .defineInRange("vampirePredatorKillChance", 0.18D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue VAMPIRE_RIPPER_OVERFEED_CHANCE = BUILDER.comment(
            "Stable per predator/victim/day chance that a hungry RIPPER deliberately continues feeding")
            .defineInRange("vampireRipperOverfeedChance", 0.78D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue VAMPIRE_RIPPER_SPORT_KILL_CHANCE = BUILDER.comment(
            "Stable per predator/victim/day chance that a RIPPER hunts an MCA human without current feeding pressure")
            .defineInRange("vampireRipperSportKillChance", 0.10D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue VAMPIRE_VENGEFUL_KILL_CHANCE = BUILDER.comment(
            "Stable chance that a VENGEFUL wild vampire targets an MCA witness who already knows its vampire identity")
            .defineInRange("vampireVengefulKillChance", 0.90D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue VAMPIRE_RIPPER_MAX_EXTRA_FEEDS = BUILDER.comment(
            "Maximum additional provider-confirmed feeds attempted by one RIPPER session before switching to combat")
            .defineInRange("vampireRipperMaxExtraFeeds", 2, 0, 8);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FolkloreConfig() {}

    private static ModConfigSpec.BooleanValue toggle(String name, boolean defaultValue) {
        return BUILDER.comment("Master toggle for " + name).define(name, defaultValue);
    }
}
