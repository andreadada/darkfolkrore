package com.darkfolklore.core.society.village;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime-only deduplication/hysteresis layer for village reaction notifications.
 *
 * <p>The persisted village state remains authoritative. This class only remembers the last reaction band and
 * recent incident fingerprints, so notification failures can never corrupt world state.</p>
 */
public final class VillageReactionTracker {
    public static final VillageReactionTracker INSTANCE = new VillageReactionTracker();

    static final long INCIDENT_DEDUP_TICKS = 100L;
    static final long NOTIFICATION_COOLDOWN_TICKS = 200L;
    private static final int MAX_TRACKED = 4096;

    private final Map<String, VillageAlertLevel> levels = new HashMap<>();
    private final Map<String, Long> lastNotification = new HashMap<>();
    private final Map<String, Long> recentIncidents = new HashMap<>();

    private VillageReactionTracker() {}

    public synchronized Optional<Transition> record(String villageKey, String incidentFingerprint,
                                                    long gameTime, VillageSocietyState state) {
        if (villageKey == null || villageKey.isBlank() || state == null) return Optional.empty();
        if (incidentFingerprint != null && !incidentFingerprint.isBlank()) {
            String dedupKey = villageKey + "|" + incidentFingerprint;
            long previous = recentIncidents.getOrDefault(dedupKey, Long.MIN_VALUE / 2);
            if (gameTime - previous < INCIDENT_DEDUP_TICKS) return Optional.empty();
            recentIncidents.put(dedupKey, gameTime);
        }

        VillageAlertLevel before = levels.getOrDefault(villageKey, VillageAlertLevel.CALM);
        int score = VillageAlertLevel.score(state);
        VillageAlertLevel after = VillageAlertLevel.transition(before, score);
        levels.put(villageKey, after);
        prune(gameTime);

        if (after == before) return Optional.empty();
        long notified = lastNotification.getOrDefault(villageKey, Long.MIN_VALUE / 2);
        if (gameTime - notified < NOTIFICATION_COOLDOWN_TICKS) return Optional.empty();

        lastNotification.put(villageKey, gameTime);
        return Optional.of(new Transition(before, after, score));
    }

    public synchronized VillageAlertLevel level(String villageKey) {
        return levels.getOrDefault(villageKey, VillageAlertLevel.CALM);
    }

    public synchronized void clear() {
        levels.clear();
        lastNotification.clear();
        recentIncidents.clear();
    }

    private void prune(long gameTime) {
        if (recentIncidents.size() > MAX_TRACKED) {
            recentIncidents.entrySet().removeIf(entry -> gameTime - entry.getValue() > INCIDENT_DEDUP_TICKS * 4);
        }
        if (levels.size() > MAX_TRACKED) {
            lastNotification.entrySet().removeIf(entry -> gameTime - entry.getValue() > 24000L);
            levels.keySet().retainAll(lastNotification.keySet());
        }
    }

    public record Transition(VillageAlertLevel before, VillageAlertLevel after, int score) {}
}
