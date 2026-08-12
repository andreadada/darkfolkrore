package com.darkfolklore.core.lifecycle;

/** Bounded retry policy for provider capabilities attached shortly after entity join. */
public final class InitialObservationPolicy {
    public static final int MAX_RETRY_TICKS = 200;

    private InitialObservationPolicy() {}

    public static boolean shouldAttempt(int currentTick, int firstTick, boolean sampledTick) {
        if (currentTick < firstTick) {
            return false;
        }
        return currentTick <= firstTick + MAX_RETRY_TICKS || sampledTick;
    }

    public static boolean shouldRetain(int currentTick, int firstTick, boolean observed) {
        return !observed && currentTick < firstTick + MAX_RETRY_TICKS;
    }
}
