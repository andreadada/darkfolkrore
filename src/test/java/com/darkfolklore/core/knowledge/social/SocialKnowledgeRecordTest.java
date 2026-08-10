package com.darkfolklore.core.knowledge.social;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocialKnowledgeRecordTest {
    @Test
    void strongEvidenceUpgradesButWeakRumorCannotDowngrade() {
        SocialKnowledgeRecord rumor = new SocialKnowledgeRecord(SocialKnowledgeState.RUMOR,
                0.4F, KnowledgeSource.RUMOR, 10, EvidenceType.TESTIMONY);
        SocialKnowledgeRecord direct = new SocialKnowledgeRecord(SocialKnowledgeState.CONFIRMED,
                0.95F, KnowledgeSource.DIRECT_WITNESS, 20, EvidenceType.DIRECT_WITNESS);
        SocialKnowledgeRecord merged = rumor.merge(direct).merge(rumor);
        assertEquals(SocialKnowledgeState.CONFIRMED, merged.state());
        assertEquals(KnowledgeSource.DIRECT_WITNESS, merged.source());
        assertEquals(0.95F, merged.confidence());
    }

    @Test
    void publicKnowledgeNeverExpires() {
        SocialKnowledgeRecord record = new SocialKnowledgeRecord(SocialKnowledgeState.PUBLIC,
                0.0F, KnowledgeSource.PUBLIC_REVEAL, 1, null);
        assertFalse(record.shouldForget(0.5F, 1_000_000L, 100L));
    }
}
