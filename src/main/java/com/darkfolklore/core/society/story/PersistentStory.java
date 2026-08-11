package com.darkfolklore.core.society.story;

import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Objects;

public record PersistentStory(StoryInstance story, WorldPosition location, String villageKey) {
    public PersistentStory {
        Objects.requireNonNull(story);
        Objects.requireNonNull(location);
        villageKey = Objects.requireNonNullElse(villageKey, "");
    }
}
