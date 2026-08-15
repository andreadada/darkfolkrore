package com.darkfolklore.core.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-mostly exact-provider lifecycle contract for MCA Vamp Compat.
 *
 * <p>Dark Folklore observes provider facts. It never force-infects, force-converts, force-cures, or runs
 * inheritance decisions through this bridge. The only mutation exposed is the provider's idempotent native-AI
 * registration for an already factual MCA vampire.</p>
 */
public interface McaVampireLifecycleBridge {
    McaVampireLifecycleBridge DISABLED = new McaVampireLifecycleBridge() {};

    default boolean runtimeAvailable() { return false; }

    default Snapshot snapshot(Entity entity) {
        return Snapshot.unavailable("lifecycle bridge disabled");
    }

    default boolean ensureNativeAi(LivingEntity entity) { return false; }

    default void clearRuntimeState() {}

    record Snapshot(boolean available,
                    boolean mcaVillager,
                    boolean infected,
                    boolean converted,
                    boolean curing,
                    boolean inheritanceProcessed,
                    boolean biteWasConversionCause,
                    boolean aiGoalsAdded,
                    Optional<UUID> source,
                    String detail) {
        public Snapshot {
            source = source == null ? Optional.empty() : source;
            detail = detail == null ? "" : detail;
        }

        public static Snapshot unavailable(String detail) {
            return new Snapshot(false, false, false, false, false, false,
                    false, false, Optional.empty(), detail);
        }

        public boolean factualVampire() { return available && mcaVillager && converted && !curing; }
    }
}
