package com.darkfolklore.core.investigation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.persistence.WorldPosition;
import com.darkfolklore.core.society.story.PersistentStory;
import com.darkfolklore.core.society.story.StoryInstance;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentContinuityTest {
    @Test
    void simultaneousSameConceptEvidenceCannotCrossLinkAnotherIncident() {
        UUID culprit = UUID.randomUUID();
        StoryInstance story = new StoryInstance(UUID.randomUUID(), "body_discovered",
                "darkfolklore:vampire", 100L, 1000L);
        story.addActor(culprit);
        PersistentStory persistent = new PersistentStory(story,
                new WorldPosition("minecraft:overworld", 0, 64, 0), "village");

        assertTrue(IncidentContinuity.matches(persistent, evidence(culprit,
                new WorldPosition("minecraft:overworld", 2, 64, 2))));
        assertFalse(IncidentContinuity.matches(persistent, evidence(UUID.randomUUID(),
                new WorldPosition("minecraft:overworld", 2, 64, 2))));
        assertFalse(IncidentContinuity.matches(persistent, evidence(culprit,
                new WorldPosition("minecraft:the_nether", 2, 64, 2))));
        assertFalse(IncidentContinuity.matches(persistent, evidence(culprit,
                new WorldPosition("minecraft:overworld", 100, 64, 100))));
    }

    private static EvidenceRecord evidence(UUID subject, WorldPosition position) {
        return new EvidenceRecord(UUID.randomUUID(), EvidenceType.BLOOD, "darkfolklore:vampire",
                Optional.of(subject), position, 100L, 1000L, Optional.empty());
    }
}
