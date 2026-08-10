package com.darkfolklore.core.society.rumor;

import com.darkfolklore.core.knowledge.social.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RumorRulesTest {
    @Test
    void retellingLosesCertaintyAndBecomesRumor() {
        SocialKnowledgeRecord witnessed = new SocialKnowledgeRecord(SocialKnowledgeState.CONFIRMED,
                1.0F, KnowledgeSource.DIRECT_WITNESS, 100, EvidenceType.DIRECT_WITNESS);
        SocialKnowledgeRecord retold = RumorRules.retell(witnessed, 1.0F, 200);
        assertEquals(SocialKnowledgeState.RUMOR, retold.state());
        assertEquals(0.75F, retold.confidence());
    }

    @Test
    void halfLifeDecayIsBounded() {
        assertEquals(0.5F, RumorRules.decay(1.0F, 100, 100), 0.0001F);
        assertEquals(0.25F, RumorRules.decay(1.0F, 200, 100), 0.0001F);
    }
}
