package com.darkfolklore.core.society.rumor;

import com.darkfolklore.core.knowledge.social.SecretType;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Bounded transient admin trace; it is deliberately not persisted or sent over a custom network channel. */
public record RumorDiagnostic(long gameTime, UUID sender, UUID recipient, UUID subject, SecretType secret,
                              float trust, float incomingConfidence, float resultingConfidence,
                              boolean delivered, String outcome, List<TrustContribution> contributions) {
    public RumorDiagnostic {
        Objects.requireNonNull(sender);
        Objects.requireNonNull(recipient);
        Objects.requireNonNull(subject);
        Objects.requireNonNull(secret);
        outcome = Objects.requireNonNullElse(outcome, "");
        contributions = List.copyOf(contributions);
    }
}
