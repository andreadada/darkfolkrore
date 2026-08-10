package com.darkfolklore.core.society.organization;

import com.darkfolklore.core.knowledge.social.SecretType;

import java.util.Objects;
import java.util.UUID;

/** One organization-level claim about a subject. */
public record OrganizationIntelKey(UUID subject, SecretType secret) {
    public OrganizationIntelKey {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(secret, "secret");
    }
}
