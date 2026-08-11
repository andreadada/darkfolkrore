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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FolkloreConfig() {}

    private static ModConfigSpec.BooleanValue toggle(String name, boolean defaultValue) {
        return BUILDER.comment("Master toggle for " + name).define(name, defaultValue);
    }
}
