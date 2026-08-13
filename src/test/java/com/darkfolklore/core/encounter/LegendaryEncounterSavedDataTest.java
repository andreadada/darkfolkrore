package com.darkfolklore.core.encounter;

import com.darkfolklore.core.persistence.WorldPosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LegendaryEncounterSavedDataTest {
    @Test
    void storyManifestationAndBoundedParticipantsRoundTripThroughSchemaTwo() throws Exception {
        LegendaryEncounterSavedData original = new LegendaryEncounterSavedData();
        EncounterInstance encounter = new EncounterInstance(UUID.randomUUID(), "darkfolklore:test",
                "darkfolklore:wendigo", "cnc:wendigo", EncounterRank.LEGENDARY,
                EncounterSpawnMode.STORY_MANIFESTATION, EncounterOrigin.LOST_PERSON,
                new WorldPosition("minecraft:overworld", 4, 70, 8), "minecraft:overworld|0|0",
                10L, 20L, 1000L);
        UUID story = UUID.randomUUID();
        UUID manifestation = UUID.randomUUID();
        encounter.bindStory(story);
        encounter.bindManifestation(manifestation);
        encounter.transition(EncounterStage.OMENS, 30L);
        original.put(encounter);

        CompoundTag serialized = original.save(new CompoundTag(), null);
        assertEquals(2, serialized.getInt("schema"));

        Method load = LegendaryEncounterSavedData.class.getDeclaredMethod(
                "load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        LegendaryEncounterSavedData restored = (LegendaryEncounterSavedData) load.invoke(null, serialized, null);
        EncounterInstance value = restored.encounter(encounter.id()).orElseThrow();
        assertEquals(story, value.storyId().orElseThrow());
        assertEquals(manifestation, value.manifestationEntity().orElseThrow());
        assertTrue(value.participants().contains(manifestation));
    }
}
