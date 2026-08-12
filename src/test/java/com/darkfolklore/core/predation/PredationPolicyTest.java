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

    private static PredationPolicy.Candidate candidate(boolean animal, boolean mca, int witnesses, boolean isolated) {
        return new PredationPolicy.Candidate(animal, mca, true, false, false,
                false, false, false, true, witnesses, 2.0D, isolated);
    }
}
