package com.darkfolklore.core.encounter;

/**
 * Pure precedence/composition rules for Dark Folklore natural-spawn rarity.
 *
 * <p>There is exactly one random roll. Explicit encounter policies are authoritative when present. A hostile mob
 * that only has a legacy spawn profile keeps the old effective rarity by composing that profile with the generic
 * hostile multiplier before the one roll, rather than being independently rolled twice.</p>
 */
public final class NaturalSpawnRarityPolicy {
    private NaturalSpawnRarityPolicy() {}

    public enum Source {
        UNMANAGED,
        ENCOUNTER_POLICY,
        HOSTILE_PROFILE,
        SPAWN_PROFILE,
        GENERIC_HOSTILE
    }

    public static Decision resolve(Double encounterMultiplier,
                                   Double spawnProfileChance,
                                   boolean hostile,
                                   double genericHostileMultiplier,
                                   double globalCuratedMultiplier,
                                   double profileContextMultiplier) {
        double generic = clamp01(genericHostileMultiplier);
        double curated = clamp(globalCuratedMultiplier, 0.0D, 4.0D);
        double context = Math.max(0.0D, profileContextMultiplier);

        if (encounterMultiplier != null) {
            double chance = clamp01(encounterMultiplier) * curated;
            if (spawnProfileChance != null) chance *= context;
            return new Decision(true, Source.ENCOUNTER_POLICY, clamp01(chance));
        }
        if (spawnProfileChance != null) {
            double chance = clamp01(spawnProfileChance) * curated * context;
            if (hostile) {
                chance *= generic;
                return new Decision(true, Source.HOSTILE_PROFILE, clamp01(chance));
            }
            return new Decision(true, Source.SPAWN_PROFILE, clamp01(chance));
        }
        if (hostile) {
            return new Decision(true, Source.GENERIC_HOSTILE, generic);
        }
        return new Decision(false, Source.UNMANAGED, 1.0D);
    }

    /** nextDouble() is in [0,1), so >= gives an exact zero-chance rejection and exact probability semantics. */
    public static boolean reject(Decision decision, double roll) {
        if (decision == null || !decision.managed()) return false;
        if (decision.chance() <= 0.0D) return true;
        if (decision.chance() >= 1.0D) return false;
        return clamp01(roll) >= decision.chance();
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0D, 1.0D);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Decision(boolean managed, Source source, double chance) {}
}
