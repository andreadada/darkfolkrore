package com.darkfolklore.core.society.village;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillageReactionTrackerTest {
    @Test
    void deduplicatesIncidentsAndSuppressesRapidNotifications() {
        VillageReactionTracker tracker = VillageReactionTracker.INSTANCE;
        tracker.clear();
        VillageSocietyState state = new VillageSocietyState();
        state.setValues(0, 0, 0, 0, 0, 25, 0);

        var first = tracker.record("village", "incident-a", 1000L, state);
        assertTrue(first.isPresent());
        assertEquals(VillageAlertLevel.WATCHFUL, first.orElseThrow().after());
        assertTrue(tracker.record("village", "incident-a", 1020L, state).isEmpty());

        state.adjustFear(30);
        assertTrue(tracker.record("village", "incident-b", 1050L, state).isEmpty());
        assertEquals(VillageAlertLevel.ALARMED, tracker.level("village"));
        assertTrue(tracker.record("village", "incident-c", 1300L, state).isEmpty());
        tracker.clear();
    }
}
