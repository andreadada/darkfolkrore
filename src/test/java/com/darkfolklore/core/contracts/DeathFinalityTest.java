package com.darkfolklore.core.contracts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathFinalityTest {
    @Test
    void cancelledDeathNeverBecomesFinalEvenIfEntityWasMomentarilyNonAlive() {
        assertFalse(DeathFinality.confirmed(true, false));
    }

    @Test
    void rescuedOrResurrectedEntityNeverBecomesFinal() {
        assertFalse(DeathFinality.confirmed(false, true));
    }

    @Test
    void onlyUncancelledNonAliveEntityIsConfirmedDead() {
        assertTrue(DeathFinality.confirmed(false, false));
    }
}
