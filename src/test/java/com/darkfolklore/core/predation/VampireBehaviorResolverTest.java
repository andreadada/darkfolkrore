package com.darkfolklore.core.predation;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VampireBehaviorResolverTest {
    @Test
    void auditedMcaPersonalitiesMapWithoutRerollingProviderFacts() {
        UUID id = UUID.fromString("12345678-1234-5678-9abc-def012345678");
        assertEquals(VampireBehaviorProfile.PREDATOR,
                VampireBehaviorResolver.resolve(PredatorKind.MCA_VAMPIRE, id, Optional.of("EXTROVERTED"), true).profile());
        assertEquals(VampireBehaviorProfile.CAUTIOUS,
                VampireBehaviorResolver.resolve(PredatorKind.MCA_VAMPIRE, id, Optional.of("INTROVERTED"), true).profile());
        assertEquals(VampireBehaviorProfile.CAUTIOUS,
                VampireBehaviorResolver.resolve(PredatorKind.MCA_VAMPIRE, id, Optional.of("ANXIOUS"), true).profile());
        assertEquals(VampireBehaviorProfile.CONTROLLED,
                VampireBehaviorResolver.resolve(PredatorKind.MCA_VAMPIRE, id, Optional.of("RELAXED"), true).profile());
    }

    @Test
    void wildProfileIsStableAcrossRepeatedResolution() {
        UUID id = UUID.fromString("cfbb17c1-f67f-4c0d-936e-684c71ae1337");
        VampireBehaviorProfile first = VampireBehaviorResolver.stableProfile(id);
        for (int i = 0; i < 20; i++) assertEquals(first, VampireBehaviorResolver.stableProfile(id));
    }

    @Test
    void stableDistributionCanRepresentEverySupportedArchetype() {
        EnumSet<VampireBehaviorProfile> seen = EnumSet.noneOf(VampireBehaviorProfile.class);
        for (long i = 0; i < 2048 && seen.size() < VampireBehaviorProfile.values().length; i++) {
            seen.add(VampireBehaviorResolver.stableProfile(new UUID(i * 0x9E3779B97F4A7C15L, ~i)));
        }
        assertEquals(EnumSet.allOf(VampireBehaviorProfile.class), seen);
    }

    @Test
    void disabledPersonalityModifiersUseStableIdentityFallback() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000321");
        assertEquals(VampireBehaviorResolver.stableProfile(id),
                VampireBehaviorResolver.resolve(PredatorKind.MCA_VAMPIRE, id, Optional.of("EXTROVERTED"), false).profile());
    }
}
