package com.darkfolklore.core.predation;

/** Runtime phase of one bounded feeding attempt. No phase is persisted across server restart. */
public enum PredationPhase {
    IDLE,
    SEARCHING,
    TARGET_SELECTED,
    PURSUING,
    STALKING,
    ATTACKING,
    FEEDING,
    COOLDOWN,
    ABORTED
}
