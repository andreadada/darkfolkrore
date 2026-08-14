package com.darkfolklore.core.society.village;

/**
 * Stable public reaction bands for village supernatural awareness.
 *
 * <p>Escalation thresholds are intentionally higher than recovery thresholds so a village cannot oscillate
 * between two messages when one bounded incident moves a score by a few points.</p>
 */
public enum VillageAlertLevel {
    CALM,
    WATCHFUL,
    ALARMED,
    CRISIS;

    public static int score(VillageSocietyState state) {
        if (state == null) return 0;
        return Math.max(state.publicAwareness(), Math.max(state.fear(), state.suspicion()));
    }

    public static VillageAlertLevel transition(VillageAlertLevel current, VillageSocietyState state) {
        return transition(current, score(state));
    }

    static VillageAlertLevel transition(VillageAlertLevel current, int score) {
        VillageAlertLevel level = current == null ? CALM : current;
        int bounded = Math.max(0, Math.min(100, score));

        if (bounded >= 75) return CRISIS;
        if (bounded >= 45 && level.ordinal() < ALARMED.ordinal()) return ALARMED;
        if (bounded >= 20 && level == CALM) return WATCHFUL;

        if (level == CRISIS && bounded < 62) level = ALARMED;
        if (level == ALARMED && bounded < 35) level = WATCHFUL;
        if (level == WATCHFUL && bounded < 12) level = CALM;
        return level;
    }
}
