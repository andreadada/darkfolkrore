package com.darkfolklore.core.endgame;

import com.darkfolklore.core.persistence.WorldPosition;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ForbiddenEndgamePolicyTest {
    @Test
    void witchingHourIsBounded() {
        assertFalse(ForbiddenEndgameEngine.isWitchingHour(17499L));
        assertTrue(ForbiddenEndgameEngine.isWitchingHour(17500L));
        assertTrue(ForbiddenEndgameEngine.isWitchingHour(18500L));
        assertFalse(ForbiddenEndgameEngine.isWitchingHour(18501L));
        assertTrue(ForbiddenEndgameEngine.isWitchingHour(41500L));
    }

    @Test
    void invocationParticipantsAreBounded() {
        DemonInvocationSite site = new DemonInvocationSite(UUID.randomUUID(), UUID.randomUUID(),
                new WorldPosition("minecraft:overworld", 0, 64, 0), 100L, UUID.randomUUID());
        for (int i = 1; i < DemonInvocationSite.MAX_PARTICIPANTS; i++) assertTrue(site.addParticipant(UUID.randomUUID()));
        assertEquals(DemonInvocationSite.MAX_PARTICIPANTS, site.participants().size());
        assertFalse(site.addParticipant(UUID.randomUUID()));
        site.complete(500L);
        assertEquals(DemonInvocationState.COMPLETED, site.state());
        assertTrue(site.state().terminal());
    }
}
