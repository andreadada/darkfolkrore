package com.darkfolklore.core.predation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PredationSessionTest {
    @Test
    void normalHuntCanProgressThroughPursuitAttackAndFeed() {
        PredationSession session = new PredationSession(UUID.randomUUID(), false,
                PredatorKind.WILD_VAMPIRISM, true, 10, 250);
        assertEquals(PredationPhase.TARGET_SELECTED, session.phase());
        session.transition(PredationPhase.PURSUING, "pursuing");
        session.transition(PredationPhase.ATTACKING, "range");
        session.transition(PredationPhase.FEEDING, "confirmed");
        assertEquals(PredationPhase.FEEDING, session.phase());
    }

    @Test
    void providerCanConfirmFeedAsynchronouslyFromAnyActiveHuntPhase() {
        PredationSession session = new PredationSession(UUID.randomUUID(), false,
                PredatorKind.MCA_VAMPIRE, false, 10, 250);
        session.transition(PredationPhase.FEEDING, "native event arrived before director sample");
        assertEquals(PredationPhase.FEEDING, session.phase());
    }

    @Test
    void invalidBackwardsTransitionFailsLoudly() {
        PredationSession session = new PredationSession(UUID.randomUUID(), false,
                PredatorKind.WILD_VAMPIRISM, true, 10, 250);
        session.transition(PredationPhase.ATTACKING, "range");
        assertThrows(IllegalStateException.class,
                () -> session.transition(PredationPhase.TARGET_SELECTED, "backwards"));
    }
}
