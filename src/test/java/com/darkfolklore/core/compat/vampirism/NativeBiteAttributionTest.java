package com.darkfolklore.core.compat.vampirism;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeBiteAttributionTest {
    @Test
    void onlyTheBracketedAttackerCooldownTransitionConfirmsAFeed() {
        UUID predator = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        var attempt = new NativeBiteAttribution.Attempt(predator, target, true);

        assertTrue(NativeBiteAttribution.confirmed(attempt, predator, target, false));
        assertFalse(NativeBiteAttribution.confirmed(attempt, UUID.randomUUID(), target, false));
        assertFalse(NativeBiteAttribution.confirmed(attempt, predator, UUID.randomUUID(), false));
        assertFalse(NativeBiteAttribution.confirmed(attempt, predator, target, true));
        assertFalse(NativeBiteAttribution.confirmed(
                new NativeBiteAttribution.Attempt(predator, target, false), predator, target, false));
    }

    @Test
    void pendingAttemptIsSingleUseAndServerStopCleanupClearsAbandonedEvents() {
        var pending = new NativeBiteAttribution.PendingAttempts<Object>();
        Object firstEvent = new Object();
        Object abandonedEvent = new Object();
        var attempt = new NativeBiteAttribution.Attempt(UUID.randomUUID(), UUID.randomUUID(), true);

        pending.capture(firstEvent, attempt);
        assertSame(attempt, pending.consume(firstEvent));
        assertNull(pending.consume(firstEvent));

        pending.capture(abandonedEvent, attempt);
        assertEquals(1, pending.size());
        pending.clear();
        assertEquals(0, pending.size());
    }
}
