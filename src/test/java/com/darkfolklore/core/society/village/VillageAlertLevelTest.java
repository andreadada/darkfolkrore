package com.darkfolklore.core.society.village;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillageAlertLevelTest {
    @Test
    void usesHysteresisForRecovery() {
        assertEquals(VillageAlertLevel.WATCHFUL, VillageAlertLevel.transition(VillageAlertLevel.CALM, 20));
        assertEquals(VillageAlertLevel.WATCHFUL, VillageAlertLevel.transition(VillageAlertLevel.WATCHFUL, 15));
        assertEquals(VillageAlertLevel.CALM, VillageAlertLevel.transition(VillageAlertLevel.WATCHFUL, 11));
        assertEquals(VillageAlertLevel.ALARMED, VillageAlertLevel.transition(VillageAlertLevel.WATCHFUL, 45));
        assertEquals(VillageAlertLevel.WATCHFUL, VillageAlertLevel.transition(VillageAlertLevel.ALARMED, 34));
        assertEquals(VillageAlertLevel.CRISIS, VillageAlertLevel.transition(VillageAlertLevel.ALARMED, 75));
        assertEquals(VillageAlertLevel.ALARMED, VillageAlertLevel.transition(VillageAlertLevel.CRISIS, 61));
    }
}
