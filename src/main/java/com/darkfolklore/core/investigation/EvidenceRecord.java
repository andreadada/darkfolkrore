package com.darkfolklore.core.investigation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record EvidenceRecord(
        UUID id,
        EvidenceType type,
        String concept,
        Optional<UUID> subject,
        WorldPosition position,
        long createdAt,
        long expiresAt,
        Optional<UUID> collectedBy
) {
    public EvidenceRecord {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        if (concept == null || !concept.contains(":")) throw new IllegalArgumentException("concept must be namespaced");
        subject = subject == null ? Optional.empty() : subject;
        Objects.requireNonNull(position);
        collectedBy = collectedBy == null ? Optional.empty() : collectedBy;
    }

    public EvidenceRecord collect(UUID player) {
        return new EvidenceRecord(id, type, concept, subject, position, createdAt, expiresAt, Optional.of(player));
    }

    public boolean expired(long now) {
        return now >= expiresAt;
    }
}
