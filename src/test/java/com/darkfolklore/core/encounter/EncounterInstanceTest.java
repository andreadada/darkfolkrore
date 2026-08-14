package com.darkfolklore.core.encounter;

import com.darkfolklore.core.persistence.WorldPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EncounterInstanceTest {
    @Test
    void encounterProgressionIsForwardOnlyAndTerminal() {
        EncounterInstance instance = instance("darkfolklore:wendigo", "cnc:wendigo");
        assertEquals(EncounterStage.ORIGIN, instance.stage());
        assertTrue(instance.transition(EncounterStage.OMENS, 30));
        assertFalse(instance.transition(EncounterStage.ORIGIN, 40));
        assertTrue(instance.transition(EncounterStage.ELIGIBLE, 50));
        assertTrue(instance.transition(EncounterStage.MANIFESTED, 60));
        assertTrue(instance.transition(EncounterStage.ACTIVE, 70));
        instance.resolve("killed");
        assertEquals(EncounterStage.RESOLVED, instance.stage());
        assertFalse(instance.transition(EncounterStage.ACTIVE, 80));
    }

    @Test
    void originPersonIsNarrativeSnapshotOnly() {
        EncounterInstance instance = new EncounterInstance(UUID.randomUUID(), "darkfolklore:test", "darkfolklore:revenant",
                "graveyard:revenant", EncounterRank.DREAD, EncounterSpawnMode.STORY_MANIFESTATION,
                EncounterOrigin.VIOLENT_DEATH, new WorldPosition("minecraft:overworld", 1, 65, 1), "minecraft:overworld|0|0",
                10, 20, 1000);
        UUID person = UUID.randomUUID();
        instance.setOriginPerson(new PersonSnapshot(person, "Gerry", java.util.Optional.empty(), "minecraft:overworld|0|0", false));
        assertEquals(person, instance.originPerson().orElseThrow().personId());
        assertTrue(instance.manifestationEntity().isEmpty());
    }

    @Test
    void stageReschedulePreservesBoundStoryAndParticipantsStayBounded() {
        EncounterInstance instance = instance("darkfolklore:wendigo", "cnc:wendigo");
        UUID story = UUID.randomUUID();
        instance.bindStory(story);
        instance.transition(EncounterStage.OMENS, 30);
        instance.restoreStage(EncounterStage.OMENS, 1, null, null, "delayed", 80);
        assertEquals(story, instance.storyId().orElseThrow());

        ArrayList<UUID> ids = new ArrayList<>();
        for (int i = 0; i < EncounterInstance.MAX_PARTICIPANTS + 20; i++) ids.add(UUID.randomUUID());
        instance.restoreParticipants(ids);
        assertEquals(EncounterInstance.MAX_PARTICIPANTS, instance.participants().size());
        assertFalse(instance.addParticipant(UUID.randomUUID()));
    }

    private static EncounterInstance instance(String concept, String implementation) {
        return new EncounterInstance(UUID.randomUUID(), "darkfolklore:test", concept,
                implementation, EncounterRank.LEGENDARY, EncounterSpawnMode.STORY_MANIFESTATION,
                EncounterOrigin.LOST_PERSON, new WorldPosition("minecraft:overworld", 0, 64, 0), "minecraft:overworld|0|0",
                10, 20, 1000);
    }
}
