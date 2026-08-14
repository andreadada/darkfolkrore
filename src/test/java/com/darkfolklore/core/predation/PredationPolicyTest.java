package com.darkfolklore.core.predation;

import com.darkfolklore.core.compat.FactResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredationPolicyTest {
    @Test
    void lowRiskMcaVampirePrefersIsolatedCivilian() {
        var context = new PredationPolicy.Context(PredatorKind.MCA_VAMPIRE, true, 5, 0);
        var civilian = PredationPolicy.score(context, candidate(false, true, 0, true));
        var animal = PredationPolicy.score(context, candidate(true, false, 0, true));
        assertTrue(civilian.eligible());
        assertTrue(animal.eligible());
        assertTrue(civilian.score() > animal.score());
    }

    @Test
    void highSuspicionPushesMcaVampireTowardAnimals() {
        var context = new PredationPolicy.Context(PredatorKind.MCA_VAMPIRE, true, 85, 90);
        var civilian = PredationPolicy.score(context, candidate(false, true, 0, true));
        var animal = PredationPolicy.score(context, candidate(true, false, 0, true));
        assertTrue(animal.eligible());
        assertTrue(!civilian.eligible() || animal.score() > civilian.score());
    }

    @Test
    void witnessesMakeCivilianPredationRiskier() {
        var context = new PredationPolicy.Context(PredatorKind.MCA_VAMPIRE, true, 10, 10);
        var hidden = PredationPolicy.score(context, candidate(false, true, 0, true));
        var exposed = PredationPolicy.score(context, candidate(false, true, 5, false));
        assertTrue(hidden.score() > exposed.score());
    }

    @Test
    void familyChildrenHuntersAndNamedNonMcaRemainProtected() {
        var context = new PredationPolicy.Context(PredatorKind.MCA_VAMPIRE, true, 0, 0);
        assertFalse(PredationPolicy.score(context, new PredationPolicy.Candidate(false, true, true,
                true, false, false, false, false, true, 0, 2, true)).eligible());
        assertFalse(PredationPolicy.score(context, new PredationPolicy.Candidate(false, true, true,
                false, true, false, false, false, true, 0, 2, true)).eligible());
        assertFalse(PredationPolicy.score(context, new PredationPolicy.Candidate(false, true, true,
                false, false, false, true, false, true, 0, 2, true)).eligible());
        assertFalse(PredationPolicy.score(context, new PredationPolicy.Candidate(true, false, true,
                false, false, false, false, true, true, 0, 2, true)).eligible());
    }

    @Test
    void wildVampiresReactLessStronglyToSocialRisk() {
        var context = new PredationPolicy.Context(PredatorKind.WILD_VAMPIRISM, true, 80, 80);
        var civilian = PredationPolicy.score(context, candidate(false, true, 0, true));
        assertTrue(civilian.eligible());
    }

    @Test
    void wildHuntMayAcquireOrReassertChosenTargetButNeverStealLiveCombatTarget() {
        assertTrue(PredationPolicy.mayDirectWildHunt(true, true, false, false));
        assertTrue(PredationPolicy.mayDirectWildHunt(true, true, true, true));
        assertFalse(PredationPolicy.mayDirectWildHunt(true, true, true, false));
        assertFalse(PredationPolicy.mayDirectWildHunt(false, true, false, false));
        assertFalse(PredationPolicy.mayDirectWildHunt(true, false, false, false));
    }

    @Test
    void daytimePredationFailsClosed() {
        var context = new PredationPolicy.Context(PredatorKind.WILD_VAMPIRISM, false, 0, 0);
        assertFalse(PredationPolicy.score(context, candidate(false, true, 0, true)).eligible());
    }

    @Test
    void unknownProviderFactsNeverBecomeMundanePreyFacts() {
        assertTrue(PredationPolicy.factsKnown(FactResult.FALSE, FactResult.FALSE, FactResult.TRUE));
        assertFalse(PredationPolicy.factsKnown(FactResult.UNKNOWN, FactResult.FALSE, FactResult.FALSE));
        assertFalse(PredationPolicy.factsKnown(FactResult.NOT_APPLICABLE, FactResult.FALSE, FactResult.FALSE));
    }

    @Test
    void everyMcaVampirePreyKindRequiresProviderApproval() {
        var context = new PredationPolicy.Context(PredatorKind.MCA_VAMPIRE, true, 0, 0);
        var rejectedCivilian = new PredationPolicy.Candidate(false, true, true, false, false,
                false, false, false, false, 0, 2, true);
        var rejectedAnimal = new PredationPolicy.Candidate(true, false, true, false, false,
                false, false, false, false, 0, 2, true);

        assertFalse(PredationPolicy.score(context, rejectedCivilian).eligible());
        assertFalse(PredationPolicy.score(context, rejectedAnimal).eligible());
        assertTrue(PredationPolicy.score(context, candidate(false, true, 0, true)).eligible());
        assertTrue(PredationPolicy.score(context, candidate(true, false, 0, true)).eligible());
    }

    @Test
    void curingOrNonProviderOwnedTargetCannotContinueMcaSession() {
        assertTrue(PredationPolicy.mayContinueMcaSession(true, true, false, true, true));
        assertFalse(PredationPolicy.mayContinueMcaSession(true, true, true, true, true));
        assertFalse(PredationPolicy.mayContinueMcaSession(false, true, false, true, true));
        assertFalse(PredationPolicy.mayContinueMcaSession(true, false, false, true, true));
        assertFalse(PredationPolicy.mayContinueMcaSession(true, true, false, false, true));
        assertFalse(PredationPolicy.mayContinueMcaSession(true, true, false, true, false));
    }

    private static PredationPolicy.Candidate candidate(boolean animal, boolean mca, int witnesses, boolean isolated) {
        return new PredationPolicy.Candidate(animal, mca, true, false, false,
                false, false, false, true, witnesses, 2.0D, isolated);
    }
}
