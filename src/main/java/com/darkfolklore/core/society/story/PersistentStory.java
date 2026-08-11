package com.darkfolklore.core.society.story;

import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent story envelope. 0.3.1 can retain the factual culprit and concrete
 * provider implementation for incident stories without changing StoryInstance's
 * generic actor model used by non-investigation stories.
 */
public record PersistentStory(
        StoryInstance story,
        WorldPosition location,
        String villageKey,
        Optional<UUID> culpritId,
        String observedImplementation
) {
    public PersistentStory {
        Objects.requireNonNull(story);
        Objects.requireNonNull(location);
        villageKey = Objects.requireNonNullElse(villageKey, "");
        culpritId = culpritId == null ? Optional.empty() : culpritId;
        observedImplementation = Objects.requireNonNullElse(observedImplementation, "");
        if (!observedImplementation.isBlank() && !observedImplementation.contains(":")) {
            throw new IllegalArgumentException("observedImplementation must be namespaced or blank");
        }
    }

    /** Backwards-compatible envelope for stories that do not own a factual culprit. */
    public PersistentStory(StoryInstance story, WorldPosition location, String villageKey) {
        this(story, location, villageKey, Optional.empty(), "");
    }
}
