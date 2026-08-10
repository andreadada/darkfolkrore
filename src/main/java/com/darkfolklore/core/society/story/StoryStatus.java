package com.darkfolklore.core.society.story;

public enum StoryStatus {
    INCIDENT,
    INVESTIGATING,
    CONFRONTATION,
    RESOLVED,
    EXPIRED;

    public boolean terminal() { return this == RESOLVED || this == EXPIRED; }
}
