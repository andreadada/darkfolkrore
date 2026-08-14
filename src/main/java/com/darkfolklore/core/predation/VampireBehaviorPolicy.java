package com.darkfolklore.core.predation;

import java.util.UUID;

/** Pure scoring/intent policy. Provider supernatural facts and infection remain outside this class. */
public final class VampireBehaviorPolicy {
    private VampireBehaviorPolicy() {}

    public static final Rates DEFAULT_RATES = new Rates(0.18D, 0.78D, 0.10D, 0.90D, 2);

    public static Preference preference(VampireBehaviorProfile profile, CandidateContext candidate) {
        double adjustment = 0.0D;
        String detail;
        switch (profile) {
            case CONTROLLED -> {
                adjustment += candidate.animal() ? 24.0D : -8.0D;
                adjustment -= candidate.visibleWitnesses() * 2.0D;
                adjustment -= combinedRisk(candidate) * 0.05D;
                detail = candidate.animal() ? "controlled feeder prefers animals" : "controlled feeder avoids human feeding";
            }
            case CAUTIOUS -> {
                adjustment += candidate.animal() ? 32.0D : -18.0D;
                adjustment -= candidate.visibleWitnesses() * 5.0D;
                adjustment -= combinedRisk(candidate) * 0.16D;
                if (candidate.isolated()) adjustment += 14.0D;
                detail = "cautious feeder strongly avoids exposure";
            }
            case PREDATOR -> {
                adjustment += candidate.mcaCivilian() ? 24.0D : -16.0D;
                adjustment += candidate.isolated() && candidate.mcaCivilian() ? 8.0D : 0.0D;
                adjustment += candidate.visibleWitnesses() * 1.5D; // partially offsets the base witness penalty
                detail = "predator prefers human prey";
            }
            case RIPPER -> {
                adjustment += candidate.mcaCivilian() ? 40.0D : -30.0D;
                adjustment += candidate.visibleWitnesses() * 4.0D; // rippers care much less about secrecy
                adjustment += candidate.isolated() ? 4.0D : 0.0D;
                detail = "ripper strongly prefers human prey and tolerates exposure";
            }
            case RECRUITER -> {
                adjustment += candidate.mcaCivilian() ? 34.0D : -30.0D;
                adjustment -= candidate.visibleWitnesses() * 4.0D;
                if (candidate.isolated() && candidate.mcaCivilian()) adjustment += 18.0D;
                detail = "recruiter prefers isolated human bite candidates";
            }
            case VENGEFUL -> {
                adjustment += candidate.mcaCivilian() ? 8.0D : -24.0D;
                if (candidate.victimKnowsIdentity() && candidate.mcaCivilian()) adjustment += 68.0D;
                adjustment += candidate.visibleWitnesses() * 1.0D;
                detail = candidate.victimKnowsIdentity()
                        ? "vengeful vampire prioritizes a witness who knows its identity"
                        : "vengeful vampire has no personal grievance against this candidate";
            }
            default -> throw new IllegalStateException("Unhandled profile " + profile);
        }
        return new Preference(adjustment, detail);
    }

    /**
     * Determines the intent for one predator/victim/day tuple. The roll is deterministic for that tuple so it
     * cannot reroll every scan tick until a lethal outcome happens.
     */
    public static VampirePredationIntent intent(VampireBehaviorProfile profile, boolean animal,
                                                 boolean victimKnowsIdentity, boolean hungry,
                                                 UUID predator, UUID victim, long worldDay, Rates rates) {
        if (animal) return hungry ? VampirePredationIntent.FEED : VampirePredationIntent.NONE;

        if (!hungry) {
            return switch (profile) {
                case RIPPER -> roll(predator, victim, worldDay, 31) < rates.ripperSportKillChance()
                        ? VampirePredationIntent.KILL_FOR_SPORT : VampirePredationIntent.NONE;
                case VENGEFUL -> victimKnowsIdentity
                        && roll(predator, victim, worldDay, 37) < rates.vengefulKillChance()
                        ? VampirePredationIntent.KILL_FOR_SPORT : VampirePredationIntent.NONE;
                default -> VampirePredationIntent.NONE;
            };
        }

        return switch (profile) {
            case CONTROLLED, CAUTIOUS -> VampirePredationIntent.FEED;
            case RECRUITER -> VampirePredationIntent.RECRUIT;
            case PREDATOR -> roll(predator, victim, worldDay, 41) < rates.predatorKillChance()
                    ? VampirePredationIntent.KILL_AFTER_FEED : VampirePredationIntent.FEED;
            case RIPPER -> roll(predator, victim, worldDay, 43) < rates.ripperOverfeedChance()
                    ? VampirePredationIntent.OVERFEED : VampirePredationIntent.KILL_AFTER_FEED;
            case VENGEFUL -> victimKnowsIdentity
                    && roll(predator, victim, worldDay, 47) < rates.vengefulKillChance()
                    ? VampirePredationIntent.KILL_AFTER_FEED : VampirePredationIntent.FEED;
        };
    }

    public static boolean mayActWithoutHunger(VampireBehaviorProfile profile) {
        return profile == VampireBehaviorProfile.RIPPER || profile == VampireBehaviorProfile.VENGEFUL;
    }

    public static boolean shouldKeepAggressingAfterFeed(VampirePredationIntent intent) {
        return intent == VampirePredationIntent.OVERFEED || intent == VampirePredationIntent.KILL_AFTER_FEED;
    }

    private static double combinedRisk(CandidateContext candidate) {
        return clamp(candidate.localRisk(), 0.0D, 100.0D) * 0.55D
                + clamp(candidate.personalRisk(), 0.0D, 100.0D) * 0.45D;
    }

    static double roll(UUID predator, UUID victim, long worldDay, int channel) {
        long seed = predator.getMostSignificantBits()
                ^ Long.rotateLeft(predator.getLeastSignificantBits(), 11)
                ^ Long.rotateLeft(victim.getMostSignificantBits(), 29)
                ^ victim.getLeastSignificantBits()
                ^ (worldDay * 0x9E3779B97F4A7C15L)
                ^ ((long) channel * 0xD1B54A32D192ED03L);
        long mixed = VampireBehaviorResolver.mix64(seed);
        return (mixed >>> 11) * 0x1.0p-53;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record CandidateContext(boolean animal, boolean mcaCivilian, boolean isolated,
                                   int visibleWitnesses, boolean victimKnowsIdentity,
                                   double localRisk, double personalRisk) {}

    public record Preference(double scoreAdjustment, String detail) {
        public Preference {
            detail = detail == null ? "" : detail;
        }
    }

    public record Rates(double predatorKillChance, double ripperOverfeedChance,
                        double ripperSportKillChance, double vengefulKillChance,
                        int maxRipperExtraFeeds) {
        public Rates {
            predatorKillChance = probability(predatorKillChance);
            ripperOverfeedChance = probability(ripperOverfeedChance);
            ripperSportKillChance = probability(ripperSportKillChance);
            vengefulKillChance = probability(vengefulKillChance);
            maxRipperExtraFeeds = Math.max(0, Math.min(8, maxRipperExtraFeeds));
        }

        private static double probability(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }
}
