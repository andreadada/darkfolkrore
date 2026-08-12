package com.darkfolklore.core.compat;

import com.darkfolklore.core.predation.PredatorKind;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Exact-provider bridge used by the bounded predation director.
 *
 * <p>Core decides whether a feeding attempt is socially reasonable; provider code remains authoritative for
 * blood drain, infection, conversion, cure state and MCA vampire bite mechanics.</p>
 */
public interface VampirePredationBridge {
    VampirePredationBridge DISABLED = new VampirePredationBridge() {};

    default boolean runtimeAvailable() { return false; }

    default PredatorKind predatorKind(Mob entity) { return PredatorKind.NONE; }

    default boolean wantsBlood(Mob entity) { return false; }

    default boolean canWildFeed(Mob predator, LivingEntity target) { return false; }

    /** Performs a real Vampirism blood drain and emits the provider BloodDrinkEvent. */
    default boolean performWildFeed(Mob predator, LivingEntity target) { return false; }

    /** True only when MCA Vamp Compat itself accepts the target for its autonomous infection-bite path. */
    default boolean canMcaVampireTarget(Mob predator, LivingEntity target) { return false; }

    /** Idempotently asks MCA Vamp Compat to install its native vampire AI goals. */
    default boolean ensureMcaNativeAi(LivingEntity entity) { return false; }

    default boolean wasRecentlyBitten(LivingEntity entity) { return false; }

    default boolean canReceiveMcaInfection(LivingEntity entity) { return false; }

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
