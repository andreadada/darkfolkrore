package com.darkfolklore.core.society.village;

import com.darkfolklore.core.society.organization.OrganizationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillageResponseRulesTest {
    @Test
    void quietVillageRemainsCalm() {
        VillageSocietyState state = new VillageSocietyState();
        assertEquals(VillageResponseTier.CALM, VillageResponseRules.assess(state).tier());
    }

    @Test
    void confirmedThreatAndHunterPressureEscalateDefense() {
        VillageSocietyState state = new VillageSocietyState();
        state.recordIncident(5, true, 8);
        state.adjustInfluence(OrganizationType.HUNTER_SOCIETY, 55);
        var response = VillageResponseRules.assess(state);
        assertTrue(response.tier().ordinal() >= VillageResponseTier.MOBILIZED.ordinal());
        assertTrue(response.hunterReadiness() > 0);
    }

    @Test
    void overwhelmingVampireInfluenceMarksVillageCompromised() {
        VillageSocietyState state = new VillageSocietyState();
        state.adjustInfluence(OrganizationType.VAMPIRE_COVEN, 80);
        state.adjustInfluence(OrganizationType.HUNTER_SOCIETY, 30);
        var response = VillageResponseRules.assess(state);
        assertEquals(VillageResponseTier.COMPROMISED, response.tier());
        assertTrue(response.supernaturalPressure() >= 80);
    }
}
