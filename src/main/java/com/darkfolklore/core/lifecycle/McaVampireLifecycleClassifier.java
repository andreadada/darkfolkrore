package com.darkfolklore.core.lifecycle;

import com.darkfolklore.core.compat.McaVampireLifecycleBridge;

/** Pure transition classifier so lifecycle semantics can be regression-tested without optional provider classes. */
public final class McaVampireLifecycleClassifier {
    private McaVampireLifecycleClassifier() {}

    public static McaVampireLifecycleState state(McaVampireLifecycleBridge.Snapshot snapshot) {
        if (snapshot == null || !snapshot.available() || !snapshot.mcaVillager()) return McaVampireLifecycleState.UNAVAILABLE;
        if (snapshot.curing()) return McaVampireLifecycleState.CURING;
        if (snapshot.converted()) return McaVampireLifecycleState.VAMPIRE;
        if (snapshot.infected()) return McaVampireLifecycleState.INFECTED;
        return McaVampireLifecycleState.HUMAN;
    }

    public static McaVampireLifecycleTransition transition(McaVampireLifecycleBridge.Snapshot previous,
                                                           McaVampireLifecycleBridge.Snapshot current,
                                                           boolean recentBirthObservation) {
        McaVampireLifecycleState before = state(previous);
        McaVampireLifecycleState after = state(current);
        if (before == McaVampireLifecycleState.UNAVAILABLE || after == McaVampireLifecycleState.UNAVAILABLE) {
            return McaVampireLifecycleTransition.NONE;
        }
        if (before == after) return McaVampireLifecycleTransition.NONE;

        if (after == McaVampireLifecycleState.VAMPIRE) {
            if (recentBirthObservation && current.inheritanceProcessed() && current.source().isEmpty()) {
                return McaVampireLifecycleTransition.INHERITED_VAMPIRE;
            }
            if (current.biteWasConversionCause()) return McaVampireLifecycleTransition.NATIVE_BITE_CONVERTED;
            if (before == McaVampireLifecycleState.CURING) return McaVampireLifecycleTransition.CURE_CANCELLED;
            return McaVampireLifecycleTransition.CONVERTED;
        }
        if (after == McaVampireLifecycleState.INFECTED && before == McaVampireLifecycleState.HUMAN) {
            return McaVampireLifecycleTransition.INFECTION_STARTED;
        }
        if (after == McaVampireLifecycleState.CURING && before == McaVampireLifecycleState.VAMPIRE) {
            return McaVampireLifecycleTransition.CURE_STARTED;
        }
        if (after == McaVampireLifecycleState.HUMAN) {
            if (before == McaVampireLifecycleState.CURING) return McaVampireLifecycleTransition.CURED;
            if (before == McaVampireLifecycleState.INFECTED) return McaVampireLifecycleTransition.INFECTION_CLEARED;
            if (before == McaVampireLifecycleState.VAMPIRE) return McaVampireLifecycleTransition.VAMPIRISM_CLEARED;
        }
        return McaVampireLifecycleTransition.UNCLASSIFIED;
    }
}
