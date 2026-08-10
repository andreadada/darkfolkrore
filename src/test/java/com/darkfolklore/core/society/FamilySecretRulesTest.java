package com.darkfolklore.core.society;

import com.darkfolklore.core.compat.mca.McaRelationshipCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FamilySecretRulesTest {
    @Test
    void reactionsRequireVerifiedFamilyEvidence() {
        assertTrue(FamilySecretRules.choose(McaRelationshipCategory.STRANGER, "ANXIOUS", false).isEmpty());
        assertEquals(FamilySecretReaction.PROTECT_SECRET,
                FamilySecretRules.choose(McaRelationshipCategory.SPOUSE, "RELAXED", false).orElseThrow());
        assertEquals(FamilySecretReaction.CONFRONT_RELATIVE,
                FamilySecretRules.choose(McaRelationshipCategory.SIBLING, "UPBEAT", false).orElseThrow());
    }

    @Test
    void factualHunterAlignmentOverridesProtectiveDefault() {
        FamilySecretReaction reaction = FamilySecretRules.choose(
                McaRelationshipCategory.SOURCE_IS_CHILD, "RELAXED", true).orElseThrow();
        assertEquals(FamilySecretReaction.REPORT_TO_HUNTERS, reaction);
        assertFalse(FamilySecretRules.suppressesRetelling(reaction));
        assertFalse(FamilySecretRules.suppressesOrganizationReport(reaction));
    }
}
