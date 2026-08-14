package com.darkfolklore.core.predation;

/** Intent attached to one bounded Dark Folklore predation session. */
public enum VampirePredationIntent {
    /** Ordinary feeding: stop after one successful provider-confirmed feed. */
    FEED,
    /** Intentionally nonlethal human bite; provider still independently decides infection/conversion. */
    RECRUIT,
    /** Continue drinking after the first successful feed, then become lethal if the victim survives. */
    OVERFEED,
    /** Feed once, then keep the same victim as a combat target. */
    KILL_AFTER_FEED,
    /** Hunt/kill without requiring current feeding pressure. No infection mutation is attempted by Core. */
    KILL_FOR_SPORT,
    /** MCA Vamp Compat owns the complete human-target and bite decision. */
    PROVIDER_OWNED,
    /** No valid behavioral motive for this candidate in the current context. */
    NONE;

    public boolean lethal() {
        return this == OVERFEED || this == KILL_AFTER_FEED || this == KILL_FOR_SPORT;
    }

    public boolean postFeedAggression() {
        return this == OVERFEED || this == KILL_AFTER_FEED;
    }

    public boolean providerOwned() {
        return this == PROVIDER_OWNED;
    }
}
