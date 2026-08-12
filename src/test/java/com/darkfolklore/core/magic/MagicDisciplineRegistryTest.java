package com.darkfolklore.core.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MagicDisciplineRegistryTest {
    @Test
    void everyDisciplineHasACompleteCuratedProfile() {
        assertEquals(MagicDiscipline.values().length, MagicDisciplineRegistry.disciplines().size());
        for (MagicDiscipline discipline : MagicDiscipline.values()) {
            MagicDisciplineProfile profile = MagicDisciplineRegistry.profile(discipline);
            assertNotNull(profile);
            assertEquals(discipline, profile.discipline());
            assertTrue(profile.knowledgeConcept().startsWith("darkfolklore:"));
            assertFalse(profile.uses().isEmpty());
            assertFalse(profile.providerNamespaces().isEmpty());
        }
    }

    @Test
    void ritualMagicIsCrossProviderButSpecificDisciplinesStayProviderScoped() {
        assertTrue(MagicDisciplineRegistry.profile(MagicDiscipline.RITUAL_MAGIC).providerNamespaces().size() > 3);
        assertEquals(1, MagicDisciplineRegistry.profile(MagicDiscipline.WITCHCRAFT).providerNamespaces().size());
        assertEquals(1, MagicDisciplineRegistry.profile(MagicDiscipline.SPIRITUALISM).providerNamespaces().size());
        assertTrue(MagicDisciplineRegistry.profile(MagicDiscipline.BLOOD_MAGIC).uses().contains(MagicUse.BLOOD_READING));
    }
}
