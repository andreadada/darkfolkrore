package com.darkfolklore.core.investigation;

import com.darkfolklore.core.society.story.PersistentStory;

/** Pure incident/evidence binding policy used when upgrading legacy story rows. */
public final class IncidentContinuity {
    private IncidentContinuity() {}

    public static boolean matches(PersistentStory story, EvidenceRecord evidence) {
        if (!evidence.concept().equals(story.story().concept()) || evidence.subject().isEmpty()) return false;
        if (!story.story().actors().contains(evidence.subject().get())) return false;
        if (Math.abs(evidence.createdAt() - story.story().createdAt()) > 20L) return false;
        if (!evidence.position().dimension().equals(story.location().dimension())) return false;
        return evidence.position().distanceSquared(story.location().blockPos()) <= 1024.0D;
    }
}
