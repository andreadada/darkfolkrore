package com.darkfolklore.core.knowledge.observation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.knowledge.social.KnowledgeSource;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import com.darkfolklore.core.persistence.WorldPosition;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreatureSightingRecordTest {
    @Test
    void strongerDirectObservationCannotBeDowngradedByLaterRumor() {
        UUID entity = UUID.randomUUID();
        CreatureSightingRecord direct = new CreatureSightingRecord(
                SocialKnowledgeState.CONFIRMED, 0.9F, KnowledgeSource.DIRECT_WITNESS, 100L,
                Optional.of(entity), Optional.of(new WorldPosition("minecraft:overworld", 10, 64, 20)),
                EvidenceType.DIRECT_WITNESS);
        CreatureSightingRecord rumor = new CreatureSightingRecord(
                SocialKnowledgeState.RUMOR, 0.4F, KnowledgeSource.RUMOR, 200L,
                Optional.empty(), Optional.empty(), EvidenceType.TESTIMONY);

        CreatureSightingRecord merged = direct.merge(rumor);
        assertEquals(SocialKnowledgeState.CONFIRMED, merged.state());
        assertEquals(0.9F, merged.confidence());
        assertEquals(KnowledgeSource.DIRECT_WITNESS, merged.source());
        assertEquals(Optional.of(entity), merged.entityId());
        assertTrue(merged.location().isPresent());
        assertEquals(200L, merged.gameTime(), "freshness advances without discarding stronger factual detail");
    }

    @Test
    void confidenceIsClampedAndWeakOldSightingsCanExpire() {
        CreatureSightingRecord record = new CreatureSightingRecord(
                SocialKnowledgeState.RUMOR, 5.0F, KnowledgeSource.RUMOR, 10L,
                Optional.empty(), Optional.empty(), EvidenceType.TESTIMONY);
        assertEquals(1.0F, record.confidence());

        CreatureSightingRecord weak = new CreatureSightingRecord(
                SocialKnowledgeState.RUMOR, 0.05F, KnowledgeSource.RUMOR, 10L,
                Optional.empty(), Optional.empty(), EvidenceType.TESTIMONY);
        assertTrue(weak.shouldForget(0.08F, 5000L, 1000L));
        assertFalse(weak.shouldForget(0.08F, 500L, 1000L));
    }
}
