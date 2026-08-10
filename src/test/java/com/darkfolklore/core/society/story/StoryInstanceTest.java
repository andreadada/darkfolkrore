package com.darkfolklore.core.society.story;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoryInstanceTest {
    @Test
    void enforcesStateMachine() {
        StoryInstance story = new StoryInstance(UUID.randomUUID(), "drained_animal",
                "darkfolklore:vampire", 0, 1000);
        assertFalse(story.advance(StoryStatus.CONFRONTATION));
        assertTrue(story.advance(StoryStatus.INVESTIGATING));
        assertTrue(story.advance(StoryStatus.CONFRONTATION));
        assertTrue(story.advance(StoryStatus.RESOLVED));
        assertFalse(story.advance(StoryStatus.EXPIRED));
    }
}
