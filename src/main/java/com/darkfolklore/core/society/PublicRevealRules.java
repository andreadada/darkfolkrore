package com.darkfolklore.core.society;

import com.darkfolklore.core.knowledge.social.*;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Pure eligibility rules for turning individual credible knowledge into an explicit public claim. */
public final class PublicRevealRules {
    private PublicRevealRules() {}

    public static Assessment assess(Collection<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> records,
                                    UUID subject, SecretType secret, int requiredWitnesses,
                                    float minimumAverageConfidence) {
        int credible = 0;
        float total = 0.0F;
        for (Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> entry : records) {
            if (!entry.getKey().subject().equals(subject) || entry.getKey().secret() != secret) continue;
            SocialKnowledgeRecord record = entry.getValue();
            if (record.state() == SocialKnowledgeState.PUBLIC) {
                return new Assessment(true, Math.max(requiredWitnesses, 1), 1.0F, "already public");
            }
            if (record.state().strength() < SocialKnowledgeState.CONFIRMED.strength()) continue;
            credible++;
            total += record.confidence();
        }
        float average = credible == 0 ? 0.0F : total / credible;
        boolean eligible = credible >= Math.max(2, requiredWitnesses)
                && average >= Math.max(0.5F, minimumAverageConfidence);
        String reason = eligible ? "credible witness threshold reached"
                : "needs " + Math.max(2, requiredWitnesses) + " confirmed observers at average confidence "
                + Math.max(0.5F, minimumAverageConfidence);
        return new Assessment(eligible, credible, average, reason);
    }

    public record Assessment(boolean eligible, int credibleObservers, float averageConfidence, String reason) {}
}
