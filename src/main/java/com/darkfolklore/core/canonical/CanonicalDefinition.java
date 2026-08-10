package com.darkfolklore.core.canonical;

import java.util.List;
import java.util.Objects;

public record CanonicalDefinition(
        String concept,
        CanonicalKind kind,
        String canonicalId,
        List<String> implementations,
        CanonicalPolicy policy,
        String reason
) {
    public CanonicalDefinition {
        concept = requireId(concept, "concept");
        kind = Objects.requireNonNull(kind, "kind");
        canonicalId = canonicalId == null ? "" : canonicalId.trim();
        implementations = List.copyOf(implementations == null ? List.of() : implementations);
        policy = Objects.requireNonNull(policy, "policy");
        reason = Objects.requireNonNullElse(reason, "").trim();
        if (canonicalId.isEmpty() && policy != CanonicalPolicy.KEEP_DISTINCT && policy != CanonicalPolicy.DEFERRED_UNSAFE) {
            throw new IllegalArgumentException("canonicalId is required for policy " + policy);
        }
    }

    private static String requireId(String value, String name) {
        if (value == null || value.isBlank() || !value.contains(":")) {
            throw new IllegalArgumentException(name + " must be a namespaced ID");
        }
        return value.trim();
    }

    public boolean containsImplementation(String registryId) {
        return canonicalId.equals(registryId) || implementations.contains(registryId);
    }
}
