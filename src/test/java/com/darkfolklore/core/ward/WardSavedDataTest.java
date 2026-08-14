package com.darkfolklore.core.ward;

import com.darkfolklore.core.persistence.WorldPosition;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WardSavedDataTest {
    @Test
    void sameCreatorMayRefreshThresholdButAnotherCreatorCannotOverwriteActiveWard() {
        WardSavedData data = new WardSavedData();
        UUID creator = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        WorldPosition anchor = new WorldPosition("minecraft:overworld", 10, 64, 10);

        assertTrue(data.add(new WardRecord(UUID.randomUUID(), WardType.VAMPIRE, anchor,
                9, 70, creator, 100L, 1000L)));
        assertTrue(data.add(new WardRecord(UUID.randomUUID(), WardType.SPIRIT, anchor,
                9, 65, creator, 200L, 1200L)));
        assertEquals(1, data.wards().size());
        assertEquals(WardType.SPIRIT, data.wards().iterator().next().type());

        assertFalse(data.add(new WardRecord(UUID.randomUUID(), WardType.GENERAL, anchor,
                9, 60, other, 300L, 1300L)));
        assertEquals(1, data.wards().size());
        assertEquals(creator, data.wards().iterator().next().creator());
    }

    @Test
    void expiredForeignWardMayBeReplacedWithoutStacking() {
        WardSavedData data = new WardSavedData();
        WorldPosition anchor = new WorldPosition("minecraft:overworld", 0, 64, 0);
        assertTrue(data.add(new WardRecord(UUID.randomUUID(), WardType.UNDEAD, anchor,
                9, 70, UUID.randomUUID(), 0L, 100L)));
        UUID replacementCreator = UUID.randomUUID();
        assertTrue(data.add(new WardRecord(UUID.randomUUID(), WardType.GENERAL, anchor,
                9, 60, replacementCreator, 200L, 1000L)));
        assertEquals(1, data.wards().size());
        assertEquals(replacementCreator, data.wards().iterator().next().creator());
    }
}
