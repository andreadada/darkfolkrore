package com.darkfolklore.core.society.village;

import java.util.Iterator;
import java.util.LinkedHashMap;
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
    static final int MAX_TRACKED = 4096;

    private final LinkedHashMap<String, VillageAlertLevel> levels = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> lastNotification = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> recentIncidents = new LinkedHashMap<>();

    private VillageReactionTracker() {}

    public synchronized Optional<Transition> record(String villageKey, String incidentFingerprint,
                                                    long gameTime, VillageSocietyState state) {
        if (villageKey == null || villageKey.isBlank() || state == null) return Optional.empty();
        if (incidentFingerprint != null && !incidentFingerprint.isBlank()) {
            String dedupKey = villageKey + "|" + incidentFingerprint;
            long previous = recentIncidents.getOrDefault(dedupKey, Long.MIN_VALUE / 2);
            if (gameTime - previous < INCIDENT_DEDUP_TICKS) return Optional.empty();
            recentIncidents.remove(dedupKey);
            recentIncidents.put(dedupKey, gameTime);
        }

        VillageAlertLevel before = levels.getOrDefault(villageKey, VillageAlertLevel.CALM);
        int score = VillageAlertLevel.score(state);
        VillageAlertLevel after = VillageAlertLevel.transition(before, score);
        levels.remove(villageKey);
        levels.put(villageKey, after);
        prune(gameTime);

        if (after == before) return Optional.empty();
        long notified = lastNotification.getOrDefault(villageKey, Long.MIN_VALUE / 2);
        if (gameTime - notified < NOTIFICATION_COOLDOWN_TICKS) return Optional.empty();

        lastNotification.remove(villageKey);
        lastNotification.put(villageKey, gameTime);
        trimOldest(lastNotification, MAX_TRACKED);
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

    int trackedVillageCount() { return levels.size(); }
    int recentIncidentCount() { return recentIncidents.size(); }

    private void prune(long gameTime) {
        if (recentIncidents.size() > MAX_TRACKED) {
            recentIncidents.entrySet().removeIf(entry -> gameTime - entry.getValue() > INCIDENT_DEDUP_TICKS * 4);
            trimOldest(recentIncidents, MAX_TRACKED);
        }
        if (levels.size() > MAX_TRACKED) {
            lastNotification.entrySet().removeIf(entry -> gameTime - entry.getValue() > 24000L);
            while (levels.size() > MAX_TRACKED) {
                Iterator<Map.Entry<String, VillageAlertLevel>> iterator = levels.entrySet().iterator();
                if (!iterator.hasNext()) break;
                String removed = iterator.next().getKey();
                iterator.remove();
                lastNotification.remove(removed);
            }
        }
        trimOldest(lastNotification, MAX_TRACKED);
    }

    private static <K, V> void trimOldest(LinkedHashMap<K, V> map, int maximum) {
        while (map.size() > maximum) {
            Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    public record Transition(VillageAlertLevel before, VillageAlertLevel after, int score) {}
}
