package com.darkfolklore.core.society.story;

import com.darkfolklore.core.knowledge.social.SecretType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StoryTemplateDefinitionTest {
    @Test
    void wildcardConceptUsesEventConceptAndContractFlagIsExplicit() {
        StoryTemplateDefinition template = new StoryTemplateDefinition("darkfolklore:test",
                StoryTrigger.HUNTER_INVESTIGATION, "*", 2, 100, 1000,
                Optional.of(SecretType.VAMPIRE), false, true, true);
        assertEquals("darkfolklore:vampire", template.resolvedConcept("darkfolklore:vampire"));
        assertTrue(template.contractEligible());
    }

    @Test
    void invalidWeightsAndLifetimesAreRejectedDuringReloadPreparation() {
        assertThrows(IllegalArgumentException.class, () -> new StoryTemplateDefinition("darkfolklore:test",
                StoryTrigger.WITCHING_HOUR, "*", 0, 0, 1000,
                Optional.empty(), false, false, true));
        assertThrows(IllegalArgumentException.class, () -> new StoryTemplateDefinition("darkfolklore:test",
                StoryTrigger.WITCHING_HOUR, "*", 1, 0, 10,
                Optional.empty(), false, false, true));
    }
}
