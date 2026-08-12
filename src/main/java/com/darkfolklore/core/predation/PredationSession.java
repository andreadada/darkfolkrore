package com.darkfolklore.core.predation;

import java.util.UUID;

/** Mutable runtime-only state machine for one feeding attempt. */
final class PredationSession {
    private final UUID target;
    private final boolean animal;
    private final PredatorKind kind;
    private final boolean directedTarget;
    private final long startedAt;
    private final long expiresAt;
    private PredationPhase phase;
    private String detail;

    PredationSession(UUID target, boolean animal, PredatorKind kind, boolean directedTarget,
                     long startedAt, long expiresAt) {
        this.target = target;
        this.animal = animal;
        this.kind = kind;
        this.directedTarget = directedTarget;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.phase = PredationPhase.TARGET_SELECTED;
        this.detail = "target selected";
    }

    UUID target() { return target; }
    boolean animal() { return animal; }
    PredatorKind kind() { return kind; }
    boolean directedTarget() { return directedTarget; }
    long startedAt() { return startedAt; }
    long expiresAt() { return expiresAt; }
    PredationPhase phase() { return phase; }
    String detail() { return detail; }

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
                    || from == PredationPhase.FEEDING;
        }
        return switch (from) {
            case TARGET_SELECTED -> to == PredationPhase.PURSUING || to == PredationPhase.STALKING
                    || to == PredationPhase.ATTACKING;
            case PURSUING -> to == PredationPhase.PURSUING || to == PredationPhase.STALKING
                    || to == PredationPhase.ATTACKING;
            case STALKING -> to == PredationPhase.STALKING || to == PredationPhase.PURSUING
                    || to == PredationPhase.ATTACKING;
            case ATTACKING -> to == PredationPhase.ATTACKING || to == PredationPhase.PURSUING;
            default -> false;
        };
    }
}
