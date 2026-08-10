package com.darkfolklore.core.society.rumor;

import com.darkfolklore.core.knowledge.social.KnowledgeSource;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeRecord;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;

public final class RumorRules {
    private RumorRules() {}

    public static SocialKnowledgeRecord retell(SocialKnowledgeRecord source, float trust, long gameTime) {
        float boundedTrust = Math.max(0.0F, Math.min(1.0F, trust));
        float degradation = source.state() == SocialKnowledgeState.RUMOR ? 0.60F : 0.75F;
        float confidence = source.confidence() * degradation * (0.5F + boundedTrust * 0.5F);
        return new SocialKnowledgeRecord(SocialKnowledgeState.RUMOR, confidence,
                KnowledgeSource.RUMOR, gameTime, source.evidence());
    }

    public static float decay(float confidence, long elapsedTicks, long halfLifeTicks) {
        if (confidence <= 0.0F) return 0.0F;
        if (elapsedTicks <= 0 || halfLifeTicks <= 0) return confidence;
        return (float) (confidence * Math.pow(0.5D, (double) elapsedTicks / halfLifeTicks));
    }
}
