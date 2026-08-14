package com.darkfolklore.core.predation;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Pure deterministic profile resolver. No profile is randomly rerolled on tick/reload. */
public final class VampireBehaviorResolver {
    private VampireBehaviorResolver() {}

    public static Resolution resolve(PredatorKind kind, UUID entityId, Optional<String> mcaPersonality,
                                     boolean personalityModifiersEnabled) {
        if (kind == PredatorKind.MCA_VAMPIRE && personalityModifiersEnabled && mcaPersonality.isPresent()) {
            String personality = mcaPersonality.get().trim().toUpperCase(Locale.ROOT);
            VampireBehaviorProfile mapped = switch (personality) {
                // These are the exact personality names already audited by the MCA social bridge.
                case "EXTROVERTED" -> VampireBehaviorProfile.PREDATOR;
                case "INTROVERTED", "ANXIOUS" -> VampireBehaviorProfile.CAUTIOUS;
                case "RELAXED" -> VampireBehaviorProfile.CONTROLLED;
                default -> null;
            };
            if (mapped != null) {
                return new Resolution(mapped, "audited MCA personality " + personality);
            }
        }

        VampireBehaviorProfile stable = stableProfile(entityId);
        return new Resolution(stable, kind == PredatorKind.MCA_VAMPIRE
                ? "stable UUID fallback; MCA personality is absent/unmapped"
                : "stable UUID wild-vampire archetype");
    }

    public static VampireBehaviorProfile stableProfile(UUID entityId) {
        long mixed = mix64(entityId.getMostSignificantBits()
                ^ Long.rotateLeft(entityId.getLeastSignificantBits(), 23)
                ^ 0xD4A7F04E5EEDBEEFL);
        int bucket = (int) Math.floorMod(mixed, 100L);
        if (bucket < 25) return VampireBehaviorProfile.CONTROLLED;
        if (bucket < 45) return VampireBehaviorProfile.CAUTIOUS;
        if (bucket < 70) return VampireBehaviorProfile.PREDATOR;
        if (bucket < 82) return VampireBehaviorProfile.RIPPER;
        if (bucket < 94) return VampireBehaviorProfile.RECRUITER;
        return VampireBehaviorProfile.VENGEFUL;
    }

    static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public record Resolution(VampireBehaviorProfile profile, String detail) {
        public Resolution {
            profile = profile == null ? VampireBehaviorProfile.CONTROLLED : profile;
            detail = detail == null ? "" : detail;
        }
    }
}
