package com.darkfolklore.core.society;

import com.darkfolklore.core.knowledge.social.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SocietyRulesTest {
    @Test
    void publicRevealRequiresSeveralCredibleIndividualWitnesses() {
        UUID subject = UUID.randomUUID();
        List<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> records = List.of(
                claim(UUID.randomUUID(), subject, 0.9F),
                claim(UUID.randomUUID(), subject, 0.8F),
                claim(UUID.randomUUID(), subject, 0.7F));
        assertFalse(PublicRevealRules.assess(records.subList(0, 1), subject, SecretType.VAMPIRE,
                3, 0.75F).eligible());
        PublicRevealRules.Assessment assessment = PublicRevealRules.assess(records, subject,
                SecretType.VAMPIRE, 3, 0.75F);
        assertTrue(assessment.eligible());
        assertEquals(0.8F, assessment.averageConfidence(), 0.0001F);
    }

    @Test
    void falseAccusationNeverQualifiesAgainstAFactualSecretOrWithoutStoryControl() {
        SocialKnowledgeRecord weakBelief = new SocialKnowledgeRecord(SocialKnowledgeState.RUMOR, 0.25F,
                KnowledgeSource.RUMOR, 0, EvidenceType.MAGICAL_RESIDUE);
        assertFalse(FalseAccusationRules.eligible(true, true, false, true, weakBelief));
        assertFalse(FalseAccusationRules.eligible(true, false, false, false, weakBelief));
        assertTrue(FalseAccusationRules.eligible(true, false, false, true, weakBelief));
    }

    private static Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> claim(UUID observer, UUID subject,
                                                                               float confidence) {
        return Map.entry(new SocialKnowledgeKey(observer, subject, SecretType.VAMPIRE),
                new SocialKnowledgeRecord(SocialKnowledgeState.CONFIRMED, confidence,
                        KnowledgeSource.DIRECT_WITNESS, 0, EvidenceType.DIRECT_WITNESS));
    }
}
