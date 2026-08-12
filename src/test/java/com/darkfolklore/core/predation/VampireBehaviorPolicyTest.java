package com.darkfolklore.core.predation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VampireBehaviorPolicyTest {
    private static final UUID PREDATOR = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID VICTIM = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void controlledAndCautiousProfilesFavorAnimalOrLowExposureFeeding() {
        var human = new VampireBehaviorPolicy.CandidateContext(false, true, false, 4, false, 60, 60);
        var animal = new VampireBehaviorPolicy.CandidateContext(true, false, true, 0, false, 60, 60);
        assertTrue(VampireBehaviorPolicy.preference(VampireBehaviorProfile.CONTROLLED, animal).scoreAdjustment()
                > VampireBehaviorPolicy.preference(VampireBehaviorProfile.CONTROLLED, human).scoreAdjustment());
        assertTrue(VampireBehaviorPolicy.preference(VampireBehaviorProfile.CAUTIOUS, animal).scoreAdjustment()
                > VampireBehaviorPolicy.preference(VampireBehaviorProfile.CAUTIOUS, human).scoreAdjustment());
    }

    @Test
    void recruiterPrefersIsolatedHumansAndNeverIntentionallyKills() {
        var isolatedHuman = new VampireBehaviorPolicy.CandidateContext(false, true, true, 0, false, 20, 20);
        var exposedHuman = new VampireBehaviorPolicy.CandidateContext(false, true, false, 5, false, 20, 20);
        assertTrue(VampireBehaviorPolicy.preference(VampireBehaviorProfile.RECRUITER, isolatedHuman).scoreAdjustment()
                > VampireBehaviorPolicy.preference(VampireBehaviorProfile.RECRUITER, exposedHuman).scoreAdjustment());
        assertEquals(VampirePredationIntent.RECRUIT,
                VampireBehaviorPolicy.intent(VampireBehaviorProfile.RECRUITER, false, false, true,
                        PREDATOR, VICTIM, 10, VampireBehaviorPolicy.DEFAULT_RATES));
        assertEquals(VampirePredationIntent.NONE,
                VampireBehaviorPolicy.intent(VampireBehaviorProfile.RECRUITER, false, false, false,
                        PREDATOR, VICTIM, 10, VampireBehaviorPolicy.DEFAULT_RATES));
    }

    @Test
    void ripperCanOverfeedOrHuntWithoutHungerButOnlyThroughExplicitRates() {
        var forced = new VampireBehaviorPolicy.Rates(0, 1, 1, 0, 2);
        assertEquals(VampirePredationIntent.OVERFEED,
                VampireBehaviorPolicy.intent(VampireBehaviorProfile.RIPPER, false, false, true,
                        PREDATOR, VICTIM, 5, forced));
        assertEquals(VampirePredationIntent.KILL_FOR_SPORT,
                VampireBehaviorPolicy.intent(VampireBehaviorProfile.RIPPER, false, false, false,
                        PREDATOR, VICTIM, 5, forced));
        var disabled = new VampireBehaviorPolicy.Rates(0, 0, 0, 0, 2);
        assertEquals(VampirePredationIntent.NONE,
                VampireBehaviorPolicy.intent(VampireBehaviorProfile.RIPPER, false, false, false,
                        PREDATOR, VICTIM, 5, disabled));
    }

    @Test
    void vengefulSportKillRequiresAConfirmedIdentityWitness() {
        var forced = new VampireBehaviorPolicy.Rates(0, 0, 0, 1, 2);
        assertEquals(VampirePredationIntent.KILL_FOR_SPORT,
                VampireBehaviorPolicy.intent(VampireBehaviorProfile.VENGEFUL, false, true, false,
                        PREDATOR, VICTIM, 1, forced));
        assertEquals(VampirePredationIntent.NONE,
                VampireBehaviorPolicy.intent(VampireBehaviorProfile.VENGEFUL, false, false, false,
                        PREDATOR, VICTIM, 1, forced));
    }

    @Test
    void controlledAndCautiousNeverReceiveIntentionalLethalIntent() {
        for (VampireBehaviorProfile profile : new VampireBehaviorProfile[]{
                VampireBehaviorProfile.CONTROLLED, VampireBehaviorProfile.CAUTIOUS}) {
            assertEquals(VampirePredationIntent.FEED,
                    VampireBehaviorPolicy.intent(profile, false, true, true,
                            PREDATOR, VICTIM, 77, VampireBehaviorPolicy.DEFAULT_RATES));
        }
    }

    @Test
    void perDayIntentRollIsDeterministicInsteadOfRerollingEveryScan() {
        VampirePredationIntent first = VampireBehaviorPolicy.intent(VampireBehaviorProfile.PREDATOR,
                false, false, true, PREDATOR, VICTIM, 42, VampireBehaviorPolicy.DEFAULT_RATES);
        for (int i = 0; i < 30; i++) {
            assertEquals(first, VampireBehaviorPolicy.intent(VampireBehaviorProfile.PREDATOR,
                    false, false, true, PREDATOR, VICTIM, 42, VampireBehaviorPolicy.DEFAULT_RATES));
        }
    }

    @Test
    void behaviorRatesClampUnsafeConfigurationValues() {
        var rates = new VampireBehaviorPolicy.Rates(-2, 4, 3, -1, 99);
        assertEquals(0.0D, rates.predatorKillChance());
        assertEquals(1.0D, rates.ripperOverfeedChance());
        assertEquals(1.0D, rates.ripperSportKillChance());
        assertEquals(0.0D, rates.vengefulKillChance());
        assertEquals(8, rates.maxRipperExtraFeeds());
    }
}
