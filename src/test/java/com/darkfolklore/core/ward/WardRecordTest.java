package com.darkfolklore.core.ward;

import com.darkfolklore.core.persistence.WorldPosition;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WardRecordTest {
    @Test
    void generalWardAppliesToSpecificQueriesAndExpires() {
        WardRecord ward = new WardRecord(UUID.randomUUID(), WardType.GENERAL,
                new WorldPosition("minecraft:overworld", 0, 64, 0), 9, 50, UUID.randomUUID(), 100, 200);
        assertTrue(ward.appliesTo(WardType.VAMPIRE));
        assertTrue(ward.active(199));
        assertFalse(ward.active(200));
    }

    @Test
    void typedWardDoesNotPretendToBeAnotherTradition() {
        WardRecord ward = new WardRecord(UUID.randomUUID(), WardType.VAMPIRE,
                new WorldPosition("minecraft:overworld", 0, 64, 0), 9, 70, UUID.randomUUID(), 100, 200);
        assertTrue(ward.appliesTo(WardType.VAMPIRE));
        assertFalse(ward.appliesTo(WardType.FAE));
    }
}
