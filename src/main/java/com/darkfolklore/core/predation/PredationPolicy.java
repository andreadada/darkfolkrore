package com.darkfolklore.core.predation;

/** Pure scoring policy kept independent from Minecraft/provider classes for deterministic tests. */
public final class PredationPolicy {
    private PredationPolicy() {}

    public static Decision score(Context context, Candidate candidate) {
        if (context.predatorKind() == PredatorKind.NONE) return Decision.rejected("not a supported vampire predator");
        if (!context.night()) return Decision.rejected("predation is restricted to night");
        if (!candidate.alive()) return Decision.rejected("target is not alive");
        if (candidate.child()) return Decision.rejected("children are never autonomous feeding targets");
        if (candidate.closeFamily()) return Decision.rejected("close family is protected from autonomous feeding");
        if (candidate.supernatural()) return Decision.rejected("ordinary feeding does not target supernatural civilians");
        if (candidate.hunter()) return Decision.rejected("known hunters are combat threats, not feeding prey");
        if (candidate.namedNonMca()) return Decision.rejected("named non-MCA entities retain Vampirism's protection");
        if (!candidate.animal() && !candidate.mcaCivilian()) return Decision.rejected("unsupported prey kind");
        if (context.predatorKind() == PredatorKind.MCA_VAMPIRE && candidate.mcaCivilian() && !candidate.providerEligible()) {
            return Decision.rejected("MCA Vamp Compat rejected infection-bite target");
        }
        if (context.predatorKind() == PredatorKind.WILD_VAMPIRISM && !candidate.providerEligible()) {
            return Decision.rejected("Vampirism rejected blood source");
        }

        double socialRisk = clamp(context.localRisk(), 0, 100) * 0.55D
                + clamp(context.personalRisk(), 0, 100) * 0.45D;
        double witnessPenalty = Math.min(80.0D, Math.max(0, candidate.visibleWitnesses()) * 12.0D);
        double distancePenalty = Math.min(24.0D, Math.max(0.0D, candidate.distance()) * 1.4D);
        double score;

        if (context.predatorKind() == PredatorKind.MCA_VAMPIRE) {
            // Social vampires adapt: rising suspicion strongly pushes them toward livestock.
            if (candidate.animal()) {
                score = 62.0D + socialRisk * 0.42D - witnessPenalty * 0.20D - distancePenalty;
            } else {
                score = 86.0D - socialRisk * 0.92D - witnessPenalty - distancePenalty;
                if (socialRisk >= 70.0D) score -= 25.0D;
            }
        } else {
            // Wild Vampirism mobs remain predatory but still avoid very exposed civilian attacks.
            if (candidate.animal()) {
                score = 58.0D + socialRisk * 0.10D - witnessPenalty * 0.15D - distancePenalty;
            } else {
                score = 78.0D - socialRisk * 0.22D - witnessPenalty * 0.55D - distancePenalty;
            }
        }

        if (candidate.isolated()) score += candidate.mcaCivilian() ? 14.0D : 6.0D;
        return score < 10.0D ? Decision.rejected("social risk makes this prey unsuitable")
                : new Decision(true, score, candidate.animal() ? "animal" : "mca_civilian");
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Context(PredatorKind predatorKind, boolean night, double localRisk, double personalRisk) {}

    public record Candidate(boolean animal, boolean mcaCivilian, boolean alive, boolean child,
                            boolean closeFamily, boolean supernatural, boolean hunter, boolean namedNonMca,
                            boolean providerEligible, int visibleWitnesses, double distance, boolean isolated) {}

    public record Decision(boolean eligible, double score, String reason) {
        public static Decision rejected(String reason) { return new Decision(false, Double.NEGATIVE_INFINITY, reason); }
    }
}
