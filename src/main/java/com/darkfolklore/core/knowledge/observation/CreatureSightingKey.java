package com.darkfolklore.core.knowledge.observation;

import java.util.Objects;
import java.util.UUID;

/** Observer-specific knowledge that a canonical creature concept was seen. */
public record CreatureSightingKey(UUID observer, String concept) {
    public CreatureSightingKey {
        Objects.requireNonNull(observer, "observer");
        if (concept == null || !concept.contains(":")) {
            throw new IllegalArgumentException("concept must be namespaced");
        }
    }
}
