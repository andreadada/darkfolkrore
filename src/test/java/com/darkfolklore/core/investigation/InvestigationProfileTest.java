package com.darkfolklore.core.investigation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.magic.MagicTradition;
import com.darkfolklore.core.traits.CreatureTrait;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InvestigationProfileTest {
    @Test
    void incidentEvidenceAndAnalysisMustBelongToDeclaredSignatures() {
        assertThrows(IllegalArgumentException.class, () -> new InvestigationProfile(
                "darkfolklore:test", Set.of(CreatureTrait.CRYPTID), Set.of(EvidenceType.BLOOD),
                Map.of(MagicTradition.SOUL, EvidenceType.SOUL_ECHO), List.of(EvidenceType.BLOOD), 3, 96));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationProfile(
                "darkfolklore:test", Set.of(CreatureTrait.CRYPTID), Set.of(EvidenceType.BLOOD),
                Map.of(), List.of(EvidenceType.TRACK), 3, 96));
    }
}
