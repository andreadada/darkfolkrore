package com.darkfolklore.core.compat;

import com.darkfolklore.core.predation.PredatorKind;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.UUID;

/**
 * Exact-provider bridge used by the bounded predation director.
 *
 * <p>Core decides whether a feeding attempt is socially reasonable; provider code remains authoritative for
 * infection, conversion and cure state. Blood operations use Vampirism's exact audited creature attachment.</p>
 */
public interface VampirePredationBridge {
    VampirePredationBridge DISABLED = new VampirePredationBridge() {};

    default boolean runtimeAvailable() { return false; }

    /** Read-only per-capability health for diagnostics; an empty map means the bridge does not expose subcircuits. */
    default Map<String, Boolean> circuitStatus() { return Map.of(); }

    default PredatorKind predatorKind(Mob entity) { return PredatorKind.NONE; }

    default boolean wantsBlood(Mob entity) { return false; }

    default boolean canWildFeed(Mob predator, LivingEntity target) { return false; }

    /**
     * Gives a wild Vampirism mob a bounded combat-target hint for a provider-valid prey target. Implementations
     * must never steal a different live combat target. This hook is intentionally unavailable to MCA vampires,
     * whose target selection/navigation remain owned by MCA Vamp Compat.
     */
    default boolean requestWildHuntTarget(Mob predator, LivingEntity target) { return false; }

    /** Clears only a target previously selected by Dark Folklore for the expected victim. */
    default void clearWildHuntTarget(Mob predator, UUID expectedTarget) {}

    /** Performs a real Vampirism blood drain and emits the provider BloodDrinkEvent. */
    default boolean performWildFeed(Mob predator, LivingEntity target) { return false; }

    /** True only when MCA Vamp Compat itself accepts the target for its autonomous infection-bite path. */
    default boolean canMcaVampireTarget(Mob predator, LivingEntity target) { return false; }

    /**
     * MCA Vamp Compat 2.0.12 has no native animal-feeding goal. This exact adapter may drain an animal through
     * Vampirism's audited ExtendedCreature blood attachment, without infecting/replacing the animal.
     */
    default boolean canMcaAnimalFeed(Mob predator, LivingEntity target) { return false; }

    default boolean performMcaAnimalFeed(Mob predator, LivingEntity target) { return false; }

    default boolean wasRecentlyBitten(LivingEntity entity) { return false; }

    default void clearRuntimeState() {}

    default ProviderSnapshot providerSnapshot(Entity entity) {
        return ProviderSnapshot.unavailable("predation bridge disabled");
    }

    record ProviderSnapshot(boolean available, boolean mcaVillager, boolean vampire, boolean infected,
                            boolean converted, boolean curing, boolean recentBite, boolean aiGoalsAdded,
                            String detail) {
        public static ProviderSnapshot unavailable(String detail) {
            return new ProviderSnapshot(false, false, false, false, false, false, false, false, detail);
        }
    }
}
