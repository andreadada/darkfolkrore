package com.darkfolklore.core.compat.mca;

import java.util.Set;

/**
 * Dark Folklore's deliberately small interpretation of exact MCA 7.7.32 personality constants.
 * Zero values mean that no behavior was inferred from the label.
 */
public record McaPersonalityInfluence(
        boolean recognized,
        double rumorTransmissionModifier,
        double fearModifier,
        double investigationModifier
) {
    private static final Set<String> VERIFIED_NAMES = Set.of(
            "UNASSIGNED", "FRIENDLY", "FLIRTY", "PLAYFUL", "GLOOMY", "SENSITIVE", "GREEDY", "ODD",
            "CRABBY", "EXTROVERTED", "INTROVERTED", "RELAXED", "ANXIOUS", "PEACEFUL", "UPBEAT"
    );

    public static McaPersonalityInfluence fromVerifiedName(String name) {
        if (name == null || !VERIFIED_NAMES.contains(name)) return new McaPersonalityInfluence(false, 0, 0, 0);
        return switch (name) {
            case "EXTROVERTED" -> new McaPersonalityInfluence(true, 0.15, 0, 0);
            case "INTROVERTED" -> new McaPersonalityInfluence(true, -0.15, 0, 0);
            case "ANXIOUS" -> new McaPersonalityInfluence(true, 0, 0.15, -0.10);
            case "RELAXED" -> new McaPersonalityInfluence(true, 0, -0.10, 0.05);
            default -> new McaPersonalityInfluence(true, 0, 0, 0);
        };
    }
}
