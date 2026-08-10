package com.darkfolklore.core.knowledge.social;

import java.util.Objects;
import java.util.UUID;

public record SocialKnowledgeKey(UUID observer, UUID subject, SecretType secret) {
    public SocialKnowledgeKey {
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(secret, "secret");
        if (observer.equals(subject)) throw new IllegalArgumentException("observer and subject must differ");
    }
}
