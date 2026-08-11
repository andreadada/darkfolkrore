package com.darkfolklore.core.knowledge.observation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.knowledge.social.KnowledgeSource;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent observation of a creature concept. This is deliberately separate
 * from social identity secrets: seeing a Wendigo is not the same fact as
 * learning that a villager is secretly a vampire.
 */
public record CreatureSightingRecord(
        SocialKnowledgeState state,
        float confidence,
        KnowledgeSource source,
        long gameTime,
        Optional<UUID> entityId,
        Optional<WorldPosition> location,
        EvidenceType evidence
) {
    public CreatureSightingRecord {
        state = Objects.requireNonNull(state, "state");
        source = Objects.requireNonNull(source, "source");
        confidence = Math.max(0.0F, Math.min(1.0F, confidence));
        entityId = entityId == null ? Optional.empty() : entityId;
        location = location == null ? Optional.empty() : location;
    }

    public CreatureSightingRecord merge(CreatureSightingRecord incoming) {
        Objects.requireNonNull(incoming, "incoming");
        boolean incomingPreferred = incoming.state.strength() > state.strength()
                || (incoming.state == state && incoming.confidence > confidence)
                || (incoming.state == state && incoming.confidence == confidence && incoming.gameTime > gameTime);
        CreatureSightingRecord preferred = incomingPreferred ? incoming : this;
        return new CreatureSightingRecord(state.max(incoming.state), Math.max(confidence, incoming.confidence),
                preferred.source, Math.max(gameTime, incoming.gameTime), preferred.entityId,
                preferred.location, preferred.evidence);
    }

    public boolean shouldForget(float minimumConfidence, long now, long maximumAge) {
        return state != SocialKnowledgeState.PUBLIC
                && confidence < minimumConfidence
                && now - gameTime > maximumAge;
    }
}
