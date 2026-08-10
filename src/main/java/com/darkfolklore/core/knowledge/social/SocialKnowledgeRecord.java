package com.darkfolklore.core.knowledge.social;

import java.util.Objects;

public record SocialKnowledgeRecord(
        SocialKnowledgeState state,
        float confidence,
        KnowledgeSource source,
        long gameTime,
        EvidenceType evidence
) {
    public SocialKnowledgeRecord {
        state = Objects.requireNonNull(state, "state");
        source = Objects.requireNonNull(source, "source");
        confidence = Math.max(0.0F, Math.min(1.0F, confidence));
    }

    public SocialKnowledgeRecord merge(SocialKnowledgeRecord incoming) {
        Objects.requireNonNull(incoming, "incoming");
        SocialKnowledgeState strongest = state.max(incoming.state);
        if (incoming.state.strength() > state.strength()
                || (incoming.state == state && incoming.confidence > confidence)) {
            return new SocialKnowledgeRecord(strongest, Math.max(confidence, incoming.confidence),
                    incoming.source, Math.max(gameTime, incoming.gameTime), incoming.evidence);
        }
        return new SocialKnowledgeRecord(strongest, Math.max(confidence, incoming.confidence),
                source, Math.max(gameTime, incoming.gameTime), evidence);
    }

    public boolean shouldForget(float minimumConfidence, long now, long maximumAge) {
        return state != SocialKnowledgeState.PUBLIC
                && confidence < minimumConfidence
                && now - gameTime > maximumAge;
    }
}
