package com.darkfolklore.core.investigation;

import com.darkfolklore.core.knowledge.lore.KnowledgeStage;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.traits.CreatureTrait;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.weakness.WeaknessRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PreparationAssessmentTest {
    private static final InvestigationProfile VAMPIRE = new InvestigationProfile(
            "darkfolklore:vampire", Set.of(CreatureTrait.VAMPIRE, CreatureTrait.SUPERNATURAL),
            Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK), Map.of(),
            List.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK), 2, 96);
    private static final WeaknessRule SILVER = new WeaknessRule("darkfolklore:test_silver",
            Set.of(CreatureTrait.VAMPIRE), Set.of(ItemTrait.SILVER_WEAPON), 1.5F, Set.of(), 10);

    @Test
    void observedLoreDoesNotLeakHiddenWeaknesses() {
        PreparationAssessment value = PreparationAssessment.evaluate(KnowledgeStage.OBSERVED,
                Set.of(ItemTrait.SILVER_WEAPON), VAMPIRE, List.of(SILVER));
        assertEquals(KnowledgeStage.OBSERVED, value.knowledgeStage());
        assertFalse(value.hasKnownCountermeasure());
        assertFalse(value.prepared());
        assertTrue(value.satisfiedRules().isEmpty());
        assertTrue(value.missingOptions().isEmpty());
    }

    @Test
    void studiedLoreRevealsKnownButMissingCountermeasure() {
        PreparationAssessment value = PreparationAssessment.evaluate(KnowledgeStage.STUDIED,
                Set.of(), VAMPIRE, List.of(SILVER));
        assertTrue(value.hasKnownCountermeasure());
        assertFalse(value.prepared());
        assertTrue(value.satisfiedRules().isEmpty());
        assertEquals(List.of(Set.of(ItemTrait.SILVER_WEAPON)), value.missingOptions());
    }

    @Test
    void studiedLoreAndRequiredTraitProducePreparedState() {
        PreparationAssessment value = PreparationAssessment.evaluate(KnowledgeStage.STUDIED,
                Set.of(ItemTrait.SILVER_WEAPON), VAMPIRE, List.of(SILVER));
        assertTrue(value.hasKnownCountermeasure());
        assertTrue(value.prepared());
        assertEquals(List.of("darkfolklore:test_silver"), value.satisfiedRules());
        assertTrue(value.missingOptions().isEmpty());
    }
}
