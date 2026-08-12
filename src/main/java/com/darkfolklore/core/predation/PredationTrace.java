package com.darkfolklore.core.predation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bounded diagnostic snapshot for explaining why a vampire did or did not hunt. */
public record PredationTrace(UUID predator, PredatorKind kind, PredationPhase phase, boolean day,
                             boolean skyVisible, boolean environmentAllowed, boolean wantsBlood,
                             double localRisk, double personalRisk, Optional<UUID> selectedTarget,
                             String detail, List<Candidate> candidates, long gameTime) {
    public PredationTrace {
        selectedTarget = selectedTarget == null ? Optional.empty() : selectedTarget;
        detail = detail == null ? "" : detail;
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public record Candidate(UUID entity, String name, boolean animal, boolean mcaCivilian,
                            boolean providerEligible, int witnesses, double distance,
                            double score, boolean eligible, String reason) {
        public Candidate {
            name = name == null ? "" : name;
            reason = reason == null ? "" : reason;
        }
    }
}
