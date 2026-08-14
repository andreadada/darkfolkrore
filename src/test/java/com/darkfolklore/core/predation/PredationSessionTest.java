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
    void ripperCanProgressFromFeedToOverfeedAndThenLethalCombat() {
        PredationSession session = new PredationSession(UUID.randomUUID(), false,
                PredatorKind.WILD_VAMPIRISM, true, 10, 250,
                VampireBehaviorProfile.RIPPER, VampirePredationIntent.OVERFEED);
        session.transition(PredationPhase.ATTACKING, "range");
        session.transition(PredationPhase.FEEDING, "first provider feed");
        assertEquals(1, session.recordConfirmedFeed(40));
        assertEquals(40, session.lastFeedAt());
        session.transition(PredationPhase.OVERFEEDING, "keeps drinking");
        session.transition(PredationPhase.KILLING, "continues as combat");
        assertEquals(PredationPhase.KILLING, session.phase());
        assertEquals(VampireBehaviorProfile.RIPPER, session.behaviorProfile());
        assertEquals(VampirePredationIntent.OVERFEED, session.intent());
    }

    @Test
    void recruiterSessionRetainsExplicitNonlethalIntent() {
        PredationSession session = new PredationSession(UUID.randomUUID(), false,
                PredatorKind.WILD_VAMPIRISM, true, 10, 250,
                VampireBehaviorProfile.RECRUITER, VampirePredationIntent.RECRUIT);
        session.transition(PredationPhase.FEEDING, "provider feed");
        assertEquals(VampirePredationIntent.RECRUIT, session.intent());
        assertFalse(session.intent().lethal());
    }

    @Test
    void sessionExpiryCanBeExtendedOnlyForward() {
        PredationSession session = new PredationSession(UUID.randomUUID(), false,
                PredatorKind.WILD_VAMPIRISM, true, 10, 250,
                VampireBehaviorProfile.PREDATOR, VampirePredationIntent.KILL_AFTER_FEED);
        session.extendUntil(500);
        session.extendUntil(100);
        assertEquals(500, session.expiresAt());
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
