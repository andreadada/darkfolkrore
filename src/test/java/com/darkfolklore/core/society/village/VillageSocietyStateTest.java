package com.darkfolklore.core.society.village;

import com.darkfolklore.core.society.organization.OrganizationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillageSocietyStateTest {
    @Test
    void confirmedIncidentRaisesButClampsSocialPressure() {
        VillageSocietyState state = new VillageSocietyState();
        for (int i = 0; i < 100; i++) state.recordIncident(5, true, 10);
        assertEquals(100, state.publicAwareness());
        assertEquals(100, state.suspicion());
        assertEquals(100, state.fear());
        state.adjustPoliticalImportance(200);
        state.adjustInfluence(OrganizationType.HUNTER_SOCIETY, 200);
        assertEquals(100, state.politicalImportance());
        assertEquals(100, state.hunterInfluence());
    }
}
