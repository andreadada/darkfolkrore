package com.darkfolklore.core.lifecycle;

import com.darkfolklore.core.compat.McaVampireLifecycleBridge;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McaVampireLifecycleClassifierTest {
    @Test
    void infectionAndNativeBiteConversionRemainDistinct() {
        var human = snapshot(false, false, false, false, false, Optional.empty());
        var infected = snapshot(true, false, false, false, false, Optional.of(UUID.randomUUID()));
        var convertedByBite = snapshot(false, true, false, false, true, infected.source());

        assertEquals(McaVampireLifecycleTransition.INFECTION_STARTED,
                McaVampireLifecycleClassifier.transition(human, infected, false));
        assertEquals(McaVampireLifecycleTransition.NATIVE_BITE_CONVERTED,
                McaVampireLifecycleClassifier.transition(infected, convertedByBite, false));
    }

    @Test
    void providerInheritanceNeedsRecentBirthContextAndNoFabricatedSource() {
        var human = snapshot(false, false, false, true, false, Optional.empty());
        var inherited = snapshot(false, true, false, true, false, Optional.empty());
        assertEquals(McaVampireLifecycleTransition.INHERITED_VAMPIRE,
                McaVampireLifecycleClassifier.transition(human, inherited, true));
        assertEquals(McaVampireLifecycleTransition.CONVERTED,
                McaVampireLifecycleClassifier.transition(human, inherited, false));
    }

    @Test
    void cureStartCancelAndCompletionAreObservableWithoutOwningCure() {
        var vampire = snapshot(false, true, false, false, false, Optional.empty());
        var curing = snapshot(false, true, true, false, false, Optional.empty());
        var human = snapshot(false, false, false, false, false, Optional.empty());

        assertEquals(McaVampireLifecycleTransition.CURE_STARTED,
                McaVampireLifecycleClassifier.transition(vampire, curing, false));
        assertEquals(McaVampireLifecycleTransition.CURE_CANCELLED,
                McaVampireLifecycleClassifier.transition(curing, vampire, false));
        assertEquals(McaVampireLifecycleTransition.CURED,
                McaVampireLifecycleClassifier.transition(curing, human, false));
    }

    @Test
    void missedCureSamplingStillReportsFactualVampirismCleared() {
        var vampire = snapshot(false, true, false, false, false, Optional.empty());
        var human = snapshot(false, false, false, false, false, Optional.empty());
        assertEquals(McaVampireLifecycleTransition.VAMPIRISM_CLEARED,
                McaVampireLifecycleClassifier.transition(vampire, human, false));
    }

    @Test
    void clearedPreconversionInfectionIsNotMisreportedAsCure() {
        var infected = snapshot(true, false, false, false, false, Optional.empty());
        var human = snapshot(false, false, false, false, false, Optional.empty());
        assertEquals(McaVampireLifecycleTransition.INFECTION_CLEARED,
                McaVampireLifecycleClassifier.transition(infected, human, false));
    }

    @Test
    void unsupportedAndStableSnapshotsDoNotManufactureTransitions() {
        var unavailable = McaVampireLifecycleBridge.Snapshot.unavailable("test");
        var human = snapshot(false, false, false, false, false, Optional.empty());
        assertEquals(McaVampireLifecycleTransition.NONE,
                McaVampireLifecycleClassifier.transition(unavailable, human, false));
        assertEquals(McaVampireLifecycleTransition.NONE,
                McaVampireLifecycleClassifier.transition(human, human, false));
    }

    private static McaVampireLifecycleBridge.Snapshot snapshot(boolean infected, boolean converted,
                                                               boolean curing, boolean inheritanceProcessed,
                                                               boolean biteCause, Optional<UUID> source) {
        return new McaVampireLifecycleBridge.Snapshot(true, true, infected, converted, curing,
                inheritanceProcessed, biteCause, converted, source, "test");
    }
}
