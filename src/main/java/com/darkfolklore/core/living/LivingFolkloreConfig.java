package com.darkfolklore.core.living;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Dedicated 0.10 configuration for the investigation/society orchestration layer. */
public final class LivingFolkloreConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LIVING_FOLKLORE = toggle("livingFolklore", true);
    public static final ModConfigSpec.BooleanValue CASEBOOK = toggle("casebook", true);
    public static final ModConfigSpec.BooleanValue CONCLUSIVE_IDENTIFICATION = toggle("conclusiveIdentification", true);
    public static final ModConfigSpec.BooleanValue WITNESS_CREDIBILITY = toggle("witnessCredibility", true);
    public static final ModConfigSpec.BooleanValue WITNESS_PRESSURE = toggle("witnessPressure", true);
    public static final ModConfigSpec.BooleanValue ARCHAEOLOGY_LORE = toggle("archaeologyLore", true);
    public static final ModConfigSpec.BooleanValue ORGANIZATION_INTELLIGENCE = toggle("organizationIntelligence", true);
    public static final ModConfigSpec.BooleanValue BLOOD_FORENSICS = toggle("bloodForensics", true);

    public static final ModConfigSpec.DoubleValue IDENTIFICATION_CONFIDENCE = BUILDER.comment(
            "Minimum support of the leading evidence-derived hypothesis before it can identify a contract target")
            .defineInRange("identificationConfidence", 0.67D, 0.50D, 1.0D);
    public static final ModConfigSpec.DoubleValue IDENTIFICATION_MARGIN = BUILDER.comment(
            "Minimum support lead over the second hypothesis before identification is considered conclusive")
            .defineInRange("identificationMargin", 0.15D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue CASE_NOTES = BUILDER.comment(
            "Maximum persisted notes retained by one investigation case")
            .defineInRange("maxCaseNotes", 64, 16, 256);
    public static final ModConfigSpec.IntValue CASES_PER_PLAYER = BUILDER.comment(
            "Maximum persisted non-pruned investigation cases per player")
            .defineInRange("maxCasesPerPlayer", 16, 2, 64);
    public static final ModConfigSpec.IntValue CASE_RETENTION = BUILDER.comment(
            "Ticks to retain terminal casebook entries")
            .defineInRange("caseRetentionTicks", 336000, 24000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue WITNESS_SILENCE = BUILDER.comment(
            "Ticks a confirmed threatened witness remains unwilling to spread the relevant rumor")
            .defineInRange("witnessIntimidationSilenceTicks", 24000, 1200, 168000);
    public static final ModConfigSpec.IntValue ARCHAEOLOGY_COOLDOWN = BUILDER.comment(
            "Per-player cooldown in ticks between lore gains from repeatedly acquiring the same archaeological proof")
            .defineInRange("archaeologyLoreCooldownTicks", 12000, 200, 168000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private LivingFolkloreConfig() {}

    private static ModConfigSpec.BooleanValue toggle(String name, boolean defaultValue) {
        return BUILDER.comment("0.10 Living Folklore toggle for " + name).define(name, defaultValue);
    }
}
