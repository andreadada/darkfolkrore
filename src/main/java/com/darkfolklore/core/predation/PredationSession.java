package com.darkfolklore.core.predation;

import java.util.UUID;

/** Mutable runtime-only state machine for one feeding/predation attempt. */
final class PredationSession {
    private final UUID target;
    private final boolean animal;
    private final PredatorKind kind;
    private final boolean directedTarget;
    private final long startedAt;
    private final VampireBehaviorProfile behaviorProfile;
    private final VampirePredationIntent intent;
    private long expiresAt;
    private int confirmedFeeds;
    private long lastFeedAt = Long.MIN_VALUE;
    private PredationPhase phase;
    private String detail;

    PredationSession(UUID target, boolean animal, PredatorKind kind, boolean directedTarget,
                     long startedAt, long expiresAt) {
        this(target, animal, kind, directedTarget, startedAt, expiresAt,
                VampireBehaviorProfile.CONTROLLED,
                kind == PredatorKind.MCA_VAMPIRE ? VampirePredationIntent.PROVIDER_OWNED : VampirePredationIntent.FEED);
    }

    PredationSession(UUID target, boolean animal, PredatorKind kind, boolean directedTarget,
                     long startedAt, long expiresAt, VampireBehaviorProfile behaviorProfile,
                     VampirePredationIntent intent) {
        this.target = target;
        this.animal = animal;
        this.kind = kind;
        this.directedTarget = directedTarget;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.behaviorProfile = behaviorProfile == null ? VampireBehaviorProfile.CONTROLLED : behaviorProfile;
        this.intent = intent == null ? VampirePredationIntent.FEED : intent;
        this.phase = PredationPhase.TARGET_SELECTED;
        this.detail = "target selected";
    }

    UUID target() { return target; }
    boolean animal() { return animal; }
    PredatorKind kind() { return kind; }
    boolean directedTarget() { return directedTarget; }
    long startedAt() { return startedAt; }
    long expiresAt() { return expiresAt; }
    VampireBehaviorProfile behaviorProfile() { return behaviorProfile; }
    VampirePredationIntent intent() { return intent; }
    int confirmedFeeds() { return confirmedFeeds; }
    long lastFeedAt() { return lastFeedAt; }
    PredationPhase phase() { return phase; }
    String detail() { return detail; }

    int recordConfirmedFeed(long gameTime) {
        lastFeedAt = gameTime;
        return ++confirmedFeeds;
    }

    void extendUntil(long deadline) {
        expiresAt = Math.max(expiresAt, deadline);
    }

    void note(String detail) {
        this.detail = detail == null ? "" : detail;
    }

    void transition(PredationPhase next, String detail) {
        if (!allowed(phase, next)) {
            throw new IllegalStateException("Invalid predation transition " + phase + " -> " + next);
        }
        phase = next;
        this.detail = detail == null ? "" : detail;
    }

    private static boolean allowed(PredationPhase from, PredationPhase to) {
        if (to == PredationPhase.ABORTED || to == PredationPhase.COOLDOWN) return true;
        if (to == PredationPhase.FEEDING) {
            return from == PredationPhase.TARGET_SELECTED || from == PredationPhase.PURSUING
                    || from == PredationPhase.STALKING || from == PredationPhase.ATTACKING
                    || from == PredationPhase.FEEDING || from == PredationPhase.OVERFEEDING;
        }
        if (to == PredationPhase.KILLING) {
            return from == PredationPhase.TARGET_SELECTED || from == PredationPhase.PURSUING
                    || from == PredationPhase.STALKING || from == PredationPhase.ATTACKING
                    || from == PredationPhase.FEEDING || from == PredationPhase.OVERFEEDING
                    || from == PredationPhase.KILLING;
        }
        if (to == PredationPhase.OVERFEEDING) {
            return from == PredationPhase.FEEDING || from == PredationPhase.OVERFEEDING;
        }
        return switch (from) {
            case TARGET_SELECTED -> to == PredationPhase.PURSUING || to == PredationPhase.STALKING
                    || to == PredationPhase.ATTACKING;
            case PURSUING -> to == PredationPhase.PURSUING || to == PredationPhase.STALKING
                    || to == PredationPhase.ATTACKING;
            case STALKING -> to == PredationPhase.STALKING || to == PredationPhase.PURSUING
                    || to == PredationPhase.ATTACKING;
            case ATTACKING -> to == PredationPhase.ATTACKING || to == PredationPhase.PURSUING;
            case KILLING -> to == PredationPhase.KILLING;
            case OVERFEEDING -> to == PredationPhase.OVERFEEDING || to == PredationPhase.PURSUING
                    || to == PredationPhase.STALKING || to == PredationPhase.ATTACKING;
            default -> false;
        };
    }
}
