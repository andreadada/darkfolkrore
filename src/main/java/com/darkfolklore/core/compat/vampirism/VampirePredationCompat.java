package com.darkfolklore.core.compat.vampirism;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.VampirePredationBridge;
import com.darkfolklore.core.predation.PredatorKind;
import com.darkfolklore.core.predation.VampirePredationEngine;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.entity.vampire.IVampireMob;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.entity.ExtendedCreature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exact-version bridge for Vampirism 1.10.12 + MCA Vamp Compat 2.0.12. */
public final class VampirePredationCompat implements VampirePredationBridge {
    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private final Method mcaIsVillager;
    private final Method mcaIsVampire;
    private final Method targetEligible;
    private final Method ensureAi;
    private final Method recentlyBitten;
    private final Method canReceiveInfection;
    private final Method capabilityGet;
    private final Method stateInfected;
    private final Method stateConverted;
    private final Method stateCuring;
    private final Method stateAiAdded;
    private final Method stateCanBite;
    private final Method stateMarkBite;
    private final Method biteCooldownTicks;

    public VampirePredationCompat() throws ReflectiveOperationException {
        ClassLoader loader = VampirePredationCompat.class.getClassLoader();
        Class<?> stateService = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireStateService", false, loader);
        Class<?> targetUtil = Class.forName("com.guilh.mca_vampirism_compat.util.McaVampireTargetUtil", false, loader);
        Class<?> ai = Class.forName("com.guilh.mca_vampirism_compat.ai.McaVampireAi", false, loader);
        Class<?> bite = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireBiteService", false, loader);
        Class<?> capabilities = Class.forName("com.guilh.mca_vampirism_compat.capability.ModCapabilities", false, loader);
        Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
        Class<?> config = Class.forName("com.guilh.mca_vampirism_compat.config.McaVampirismCompatConfig", false, loader);

        mcaIsVillager = stateService.getMethod("isMcaVillager", Entity.class);
        mcaIsVampire = stateService.getMethod("isVampire", Entity.class);
        targetEligible = targetUtil.getMethod("isInfectionBiteTarget", Mob.class, LivingEntity.class);
        ensureAi = ai.getMethod("registerGoalsIfNeeded", LivingEntity.class);
        recentlyBitten = bite.getMethod("wasRecentlyBitten", LivingEntity.class);
        canReceiveInfection = bite.getMethod("canMcaVillagerReceiveInfection", LivingEntity.class);
        capabilityGet = capabilities.getMethod("get", Entity.class);
        stateInfected = state.getMethod("isInfected");
        stateConverted = state.getMethod("isConverted");
        stateCuring = state.getMethod("isCuringVampire");
        stateAiAdded = state.getMethod("areAiGoalsAdded");
        stateCanBite = state.getMethod("canBite", long.class, long.class);
        stateMarkBite = state.getMethod("markBite", long.class);
        biteCooldownTicks = config.getMethod("biteCooldownTicks");
    }

    @Override
    public boolean runtimeAvailable() { return true; }

    @Override
    public PredatorKind predatorKind(Mob entity) {
        if (queryBoolean(mcaIsVillager, null, entity) && queryBoolean(mcaIsVampire, null, entity)) {
            return PredatorKind.MCA_VAMPIRE;
        }
        return entity instanceof IVampireMob ? PredatorKind.WILD_VAMPIRISM : PredatorKind.NONE;
    }

    @Override
    public boolean wantsBlood(Mob entity) {
        if (entity instanceof IVampireMob vampire) return vampire.wantsBlood();
        ProviderSnapshot snapshot = providerSnapshot(entity);
        return snapshot.available() && snapshot.vampire() && !snapshot.curing() && mcaBiteReady(entity);
    }

    @Override
    public boolean canWildFeed(Mob predator, LivingEntity target) {
        if (!(predator instanceof IVampireMob vampire) || !target.isAlive()) return false;
        return ExtendedCreature.getSafe(target)
                .map(creature -> creature.canBeBitten(vampire) && !creature.hasPoisonousBlood())
                .orElse(false);
    }

    @Override
    public boolean performWildFeed(Mob predator, LivingEntity target) {
        if (!(predator instanceof IVampireMob vampire)) return false;
        return ExtendedCreature.getSafe(target).filter(creature -> creature.canBeBitten(vampire)
                        && !creature.hasPoisonousBlood())
                .map(creature -> {
                    int amount = creature.onBite(vampire);
                    if (amount <= 0) return false;
                    vampire.drinkBlood(amount, creature.getBloodSaturation(), new EntityBloodContext(target));
                    return true;
                }).orElse(false);
    }

    @Override
    public boolean canMcaVampireTarget(Mob predator, LivingEntity target) {
        return invokeBoolean(targetEligible, null, predator, target);
    }

    @Override
    public boolean canMcaAnimalFeed(Mob predator, LivingEntity target) {
        if (predatorKind(predator) != PredatorKind.MCA_VAMPIRE || !target.isAlive() || !mcaBiteReady(predator)) return false;
        return ExtendedCreature.getSafe(target)
                .map(creature -> creature.getBlood() > 0 && creature.getMaxBlood() > 0 && !creature.hasPoisonousBlood())
                .orElse(false);
    }

    @Override
    public boolean performMcaAnimalFeed(Mob predator, LivingEntity target) {
        if (!canMcaAnimalFeed(predator, target)) return false;
        return ExtendedCreature.getSafe(target).map(creature -> {
            int current = creature.getBlood();
            int amount = Math.max(1, Math.min(current, Math.max(1, creature.getMaxBlood() / 3)));
            if (!markMcaBite(predator)) return false;
            creature.setBlood(Math.max(0, current - amount));
            creature.sync();
            VampirePredationEngine.INSTANCE.onNativeFeed(predator, target, amount);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean ensureMcaNativeAi(LivingEntity entity) {
        return invokeBoolean(ensureAi, null, entity);
    }

    @Override
    public boolean wasRecentlyBitten(LivingEntity entity) {
        return invokeBoolean(recentlyBitten, null, entity);
    }

    @Override
    public boolean canReceiveMcaInfection(LivingEntity entity) {
        return invokeBoolean(canReceiveInfection, null, entity);
    }

    @Override
    public ProviderSnapshot providerSnapshot(Entity entity) {
        try {
            boolean mca = (boolean) mcaIsVillager.invoke(null, entity);
            if (!mca) return new ProviderSnapshot(true, false, entity instanceof IVampireMob,
                    false, false, false, false, false, "non-MCA entity");
            boolean vampire = (boolean) mcaIsVampire.invoke(null, entity);
            Object optionalValue = capabilityGet.invoke(null, entity);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) {
                return new ProviderSnapshot(true, true, vampire, false, false, false, false, false,
                        "MCA capability absent");
            }
            Object state = optional.get();
            boolean recent = entity instanceof LivingEntity living && (boolean) recentlyBitten.invoke(null, living);
            return new ProviderSnapshot(true, true, vampire,
                    (boolean) stateInfected.invoke(state), (boolean) stateConverted.invoke(state),
                    (boolean) stateCuring.invoke(state), recent, (boolean) stateAiAdded.invoke(state),
                    "exact MCA Vamp Compat 2.0.12 state");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("provider snapshot", exception);
            return ProviderSnapshot.unavailable("MCA Vamp Compat query failed");
        }
    }

    @SubscribeEvent
    public void onEntityDrinkBlood(BloodDrinkEvent.EntityDrinkBloodEvent event) {
        LivingEntity predator = event.getVampire().asEntity();
        event.getBloodSource().getEntity().ifPresent(target ->
                VampirePredationEngine.INSTANCE.onNativeFeed(predator, target, event.getAmount()));
    }

    private boolean mcaBiteReady(Entity predator) {
        try {
            Object optionalValue = capabilityGet.invoke(null, predator);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) return false;
            long now = predator.level().getGameTime();
            long cooldown = ((Number) biteCooldownTicks.invoke(null)).longValue();
            return (boolean) stateCanBite.invoke(optional.get(), now, cooldown);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("MCA bite cooldown query", exception);
            return false;
        }
    }

    private boolean markMcaBite(Entity predator) {
        try {
            Object optionalValue = capabilityGet.invoke(null, predator);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) return false;
            stateMarkBite.invoke(optional.get(), predator.level().getGameTime());
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("MCA bite cooldown mutation", exception);
            return false;
        }
    }

    private boolean queryBoolean(Method method, Object receiver, Object argument) {
        return invokeBoolean(method, receiver, argument);
    }

    private boolean invokeBoolean(Method method, Object receiver, Object... arguments) {
        try {
            return (boolean) method.invoke(receiver, arguments);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce(method.getName(), exception);
            return false;
        }
    }

    private void warnOnce(String operation, Exception exception) {
        if (failureLogged.compareAndSet(false, true)) {
            DarkFolkloreCore.LOGGER.warn("[compat/vampire_predation] {} failed; predation fails closed", operation, exception);
        }
    }

    private record EntityBloodContext(LivingEntity entity) implements IDrinkBloodContext {
        @Override public Optional<LivingEntity> getEntity() { return Optional.of(entity); }
        @Override public Optional<ItemStack> getStack() { return Optional.empty(); }
        @Override public Optional<BlockState> getBlockState() { return Optional.empty(); }
        @Override public Optional<net.minecraft.core.BlockPos> getBlockPos() { return Optional.empty(); }
    }
}
