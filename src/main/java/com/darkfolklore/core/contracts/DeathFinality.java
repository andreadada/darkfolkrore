package com.darkfolklore.core.contracts;

/**
 * Small policy helper for death-dependent contract mutations.
 *
 * A LivingDeathEvent can still be cancelled by another listener after Dark Folklore observes it.
 * Therefore a death is considered final only after event dispatch has completed and the entity
 * is still non-alive. This also rejects rescue/resurrection flows that leave the entity alive.
 */
public final class DeathFinality {
    private DeathFinality() {}

    public static boolean confirmed(boolean eventCanceled, boolean entityAlive) {
        return !eventCanceled && !entityAlive;
    }
}
