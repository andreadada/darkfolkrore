package com.darkfolklore.core.knowledge.social;

import java.util.Objects;
import java.util.UUID;

/** A subject/secret pair independent of any individual observer. */
public record SecretClaimKey(UUID subject, SecretType secret) {
    public SecretClaimKey {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(secret, "secret");
    }
}
