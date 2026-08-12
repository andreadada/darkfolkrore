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
    void providerDirectConversionIsObservedWithoutManufacturingAnInfectionEdge() {
        UUID source = UUID.randomUUID();
        var human = snapshot(false, false, false, false, false, Optional.empty());
        var direct = snapshot(false, true, false, false, false, Optional.of(source));
        var infected = snapshot(true, false, false, false, false, Optional.of(source));

        assertEquals(McaVampireLifecycleTransition.CONVERTED,
                McaVampireLifecycleClassifier.transition(human, direct, false));
        assertEquals(McaVampireLifecycleTransition.CONVERTED,
                McaVampireLifecycleClassifier.transition(infected, direct, false));
        assertEquals(Optional.of(source), direct.source());
    }

    @Test
    void providerInheritanceNeedsRecentBirthContextAndNoFabricatedSource() {
        var human = snapshot(false, false, false, true, false, Optional.empty());
        var inherited = snapshot(false, true, false, true, false, Optional.empty());
        assertEquals(McaVampireLifecycleTransition.INHERITED_VAMPIRE,
                McaVampireLifecycleClassifier.transition(human, inherited, true));
        assertEquals(McaVampireLifecycleTransition.CONVERTED,
                McaVampireLifecycleClassifier.transition(human, inherited, false));
        assertEquals(Optional.empty(), inherited.source());
    }

    @Test
    void reloadBaselineDoesNotReplayConversionButRecentBirthCanStillObserveInheritance() {
        var loadedVampire = snapshot(false, true, false, false, false, Optional.of(UUID.randomUUID()));
        var loadedHuman = snapshot(false, false, false, false, false, Optional.empty());
        var newbornInherited = snapshot(false, true, false, true, false, Optional.empty());

        assertEquals(McaVampireLifecycleTransition.NONE,
                McaVampireLifecycleClassifier.initialTransition(loadedVampire, false));
        assertEquals(McaVampireLifecycleTransition.NONE,
                McaVampireLifecycleClassifier.initialTransition(loadedHuman, false));
        assertEquals(McaVampireLifecycleTransition.INHERITED_VAMPIRE,
                McaVampireLifecycleClassifier.initialTransition(newbornInherited, true));
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
    void cureCancellationWinsOverProviderMetadataRetainedFromTheOriginalConversion() {
        UUID source = UUID.randomUUID();
        var curing = snapshot(false, true, true, true, true, Optional.of(source));
        var cancelled = snapshot(false, true, false, true, true, Optional.of(source));

        assertEquals(McaVampireLifecycleTransition.CURE_CANCELLED,
                McaVampireLifecycleClassifier.transition(curing, cancelled, true));
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
