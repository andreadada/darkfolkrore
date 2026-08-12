package com.darkfolklore.core.compat.vampirism;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.VampirePredationBridge;
import com.darkfolklore.core.predation.PredationPolicy;
import com.darkfolklore.core.predation.PredatorKind;
import com.darkfolklore.core.predation.VampirePredationEngine;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.entity.vampire.IVampireMob;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.entity.ExtendedCreature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exact-version bridge for Vampirism 1.10.12 + MCA Vamp Compat 2.0.12. */
public final class VampirePredationCompat implements VampirePredationBridge {
    private enum Circuit { WILD_FEED, MCA_FACTS, MCA_TARGET, MCA_ANIMAL_FEED, MCA_NATIVE_BITE }

    private final EnumMap<Circuit, AtomicBoolean> circuits = new EnumMap<>(Circuit.class);
    private final EnumMap<Circuit, AtomicBoolean> failureLogged = new EnumMap<>(Circuit.class);
    private final NativeBiteAttribution.PendingAttempts<LivingIncomingDamageEvent> pendingNativeBites =
            new NativeBiteAttribution.PendingAttempts<>();
    private final Method mcaIsVillager;
    private final Method mcaIsVampire;
    private final Method targetEligible;
    private final Method recentlyBitten;
    private final Method capabilityGet;
    private final Method stateInfected;
    private final Method stateConverted;
    private final Method stateCuring;
    private final Method stateAiAdded;
    private final Method stateCanBite;
    private final Method stateMarkBite;
    private final Method biteCooldownTicks;

    public VampirePredationCompat() throws ReflectiveOperationException {
        for (Circuit circuit : Circuit.values()) {
            circuits.put(circuit, new AtomicBoolean(true));
            failureLogged.put(circuit, new AtomicBoolean(false));
        }
        ClassLoader loader = VampirePredationCompat.class.getClassLoader();
        Class<?> stateService = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireStateService", false, loader);
        Class<?> targetUtil = Class.forName("com.guilh.mca_vampirism_compat.util.McaVampireTargetUtil", false, loader);
        Class<?> bite = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireBiteService", false, loader);
        Class<?> capabilities = Class.forName("com.guilh.mca_vampirism_compat.capability.ModCapabilities", false, loader);
        Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
        Class<?> config = Class.forName("com.guilh.mca_vampirism_compat.config.McaVampirismCompatConfig", false, loader);

        mcaIsVillager = stateService.getMethod("isMcaVillager", Entity.class);
        mcaIsVampire = stateService.getMethod("isVampire", Entity.class);
        targetEligible = targetUtil.getMethod("isInfectionBiteTarget", Mob.class, LivingEntity.class);
        recentlyBitten = bite.getMethod("wasRecentlyBitten", LivingEntity.class);
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
    public boolean runtimeAvailable() {
        return circuits.values().stream().anyMatch(AtomicBoolean::get);
    }

    @Override
    public Map<String, Boolean> circuitStatus() {
        EnumMap<Circuit, Boolean> snapshot = new EnumMap<>(Circuit.class);
        circuits.forEach((key, value) -> snapshot.put(key, value.get()));
        return snapshot.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> entry.getKey().name().toLowerCase(java.util.Locale.ROOT), Map.Entry::getValue));
    }

    @Override
    public PredatorKind predatorKind(Mob entity) {
        if (isMcaNamespace(entity)) {
            if (!circuit(Circuit.MCA_FACTS)) return PredatorKind.NONE;
            return invokeBoolean(Circuit.MCA_FACTS, mcaIsVillager, null, entity)
                    && invokeBoolean(Circuit.MCA_FACTS, mcaIsVampire, null, entity)
                    ? PredatorKind.MCA_VAMPIRE : PredatorKind.NONE;
        }
        return circuit(Circuit.WILD_FEED) && entity instanceof IVampireMob
                ? PredatorKind.WILD_VAMPIRISM : PredatorKind.NONE;
    }

    @Override
    public boolean wantsBlood(Mob entity) {
        try {
            if (isMcaNamespace(entity)) {
                if (!circuit(Circuit.MCA_FACTS)) return false;
                ProviderSnapshot snapshot = providerSnapshot(entity);
                return snapshot.available() && snapshot.vampire() && !snapshot.curing()
                        && mcaBiteReady(entity, Circuit.MCA_TARGET);
            }
            if (entity instanceof IVampireMob vampire) {
                if (!circuit(Circuit.WILD_FEED)) return false;
                return vampire.wantsBlood();
            }
            return false;
        } catch (RuntimeException | LinkageError exception) {
            fail(isMcaNamespace(entity) ? Circuit.MCA_TARGET : Circuit.WILD_FEED,
                    "feeding-pressure query", exception);
            return false;
        }
    }

    @Override
    public boolean canWildFeed(Mob predator, LivingEntity target) {
        if (isMcaNamespace(predator) || !circuit(Circuit.WILD_FEED)
                || !(predator instanceof IVampireMob vampire) || !target.isAlive()) return false;
        try {
            return ExtendedCreature.getSafe(target)
                    .map(creature -> creature.canBeBitten(vampire) && !creature.hasPoisonousBlood())
                    .orElse(false);
        } catch (RuntimeException | LinkageError exception) {
            fail(Circuit.WILD_FEED, "wild-feed query", exception);
            return false;
        }
    }

    @Override
    public boolean requestWildHuntTarget(Mob predator, LivingEntity target) {
        if (isMcaNamespace(predator) || !circuit(Circuit.WILD_FEED)
                || !(predator instanceof IVampireMob) || !target.isAlive()) return false;
        boolean providerEligible = canWildFeed(predator, target);
        LivingEntity current = predator.getTarget();
        boolean currentAlive = current != null && current.isAlive();
        boolean currentIsChosen = current != null && current.getUUID().equals(target.getUUID());
        if (!PredationPolicy.mayDirectWildHunt(providerEligible, true, currentAlive, currentIsChosen)) return false;
        predator.setTarget(target);
        LivingEntity applied = predator.getTarget();
        return applied != null && applied.getUUID().equals(target.getUUID());
    }

    @Override
    public void clearWildHuntTarget(Mob predator, UUID expectedTarget) {
        if (isMcaNamespace(predator) || !(predator instanceof IVampireMob)) return;
        LivingEntity current = predator.getTarget();
        if (current != null && current.getUUID().equals(expectedTarget)) predator.setTarget(null);
    }

    @Override
    public boolean performWildFeed(Mob predator, LivingEntity target) {
        if (isMcaNamespace(predator) || !circuit(Circuit.WILD_FEED)
                || !(predator instanceof IVampireMob vampire)) return false;
        try {
            return ExtendedCreature.getSafe(target).filter(creature -> creature.canBeBitten(vampire)
                            && !creature.hasPoisonousBlood())
                    .map(creature -> {
                        int amount = creature.onBite(vampire);
                        if (amount <= 0) return false;
                        vampire.drinkBlood(amount, creature.getBloodSaturation(), new EntityBloodContext(target));
                        return true;
                    }).orElse(false);
        } catch (RuntimeException | LinkageError exception) {
            fail(Circuit.WILD_FEED, "wild-feed action", exception);
            return false;
        }
    }

    @Override
    public boolean canMcaVampireTarget(Mob predator, LivingEntity target) {
        return isMcaNamespace(predator) && circuit(Circuit.MCA_TARGET)
                && invokeBoolean(Circuit.MCA_TARGET, targetEligible, null, predator, target);
    }

    @Override
    public boolean canMcaAnimalFeed(Mob predator, LivingEntity target) {
        if (!isMcaNamespace(predator) || !circuit(Circuit.MCA_ANIMAL_FEED) || !circuit(Circuit.MCA_FACTS)) return false;
        ProviderSnapshot snapshot = providerSnapshot(predator);
        if (predatorKind(predator) != PredatorKind.MCA_VAMPIRE || !snapshot.available() || snapshot.curing()
                || !target.isAlive() || !mcaBiteReady(predator, Circuit.MCA_ANIMAL_FEED)) return false;
        try {
            return ExtendedCreature.getSafe(target)
                    .map(creature -> creature.getBlood() > 0 && creature.getMaxBlood() > 0 && !creature.hasPoisonousBlood())
                    .orElse(false);
        } catch (RuntimeException | LinkageError exception) {
            fail(Circuit.MCA_ANIMAL_FEED, "MCA animal-feed query", exception);
            return false;
        }
    }

    @Override
    public boolean performMcaAnimalFeed(Mob predator, LivingEntity target) {
        if (!canMcaAnimalFeed(predator, target)) return false;
        try {
            return ExtendedCreature.getSafe(target).map(creature -> {
                int current = creature.getBlood();
                int amount = Math.max(1, Math.min(current, Math.max(1, creature.getMaxBlood() / 3)));
                if (!markMcaBite(predator)) return false;
                creature.setBlood(Math.max(0, current - amount));
                creature.sync();
                VampirePredationEngine.INSTANCE.onNativeFeed(predator, target, amount);
                return true;
            }).orElse(false);
        } catch (RuntimeException | LinkageError exception) {
            fail(Circuit.MCA_ANIMAL_FEED, "MCA animal-feed action", exception);
            return false;
        }
    }

    @Override
    public boolean wasRecentlyBitten(LivingEntity entity) {
        return isMcaNamespace(entity) && circuit(Circuit.MCA_FACTS)
                && invokeBoolean(Circuit.MCA_FACTS, recentlyBitten, null, entity);
    }

    @Override
    public ProviderSnapshot providerSnapshot(Entity entity) {
        if (!isMcaNamespace(entity)) {
            return new ProviderSnapshot(true, false, entity instanceof IVampireMob,
                    false, false, false, false, false, "non-MCA entity");
        }
        if (!circuit(Circuit.MCA_FACTS)) return ProviderSnapshot.unavailable("MCA fact circuit open");
        try {
            boolean mca = (boolean) mcaIsVillager.invoke(null, entity);
            if (!mca) return new ProviderSnapshot(true, false, false,
                    false, false, false, false, false, "MCA namespace entity is not an MCA villager");
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
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(Circuit.MCA_FACTS, "provider snapshot", exception);
            return ProviderSnapshot.unavailable("MCA Vamp Compat fact query failed");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void captureNativeMcaBite(LivingIncomingDamageEvent event) {
        if (!circuit(Circuit.MCA_NATIVE_BITE) || event.getEntity().level().isClientSide()) return;
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Mob predator) || event.getSource().getDirectEntity() != source) return;
        LivingEntity target = event.getEntity();
        if (predatorKind(predator) != PredatorKind.MCA_VAMPIRE || !canMcaVampireTarget(predator, target)) return;
        if (mcaBiteReady(predator, Circuit.MCA_NATIVE_BITE)) {
            pendingNativeBites.capture(event,
                    new NativeBiteAttribution.Attempt(predator.getUUID(), target.getUUID(), true));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void confirmNativeMcaBite(LivingIncomingDamageEvent event) {
        NativeBiteAttribution.Attempt attempt = pendingNativeBites.consume(event);
        if (attempt == null || !circuit(Circuit.MCA_NATIVE_BITE)) return;
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Mob predator) || event.getSource().getDirectEntity() != source) return;
        LivingEntity target = event.getEntity();
        boolean biteReadyAfter = mcaBiteReady(predator, Circuit.MCA_NATIVE_BITE);
        if (circuit(Circuit.MCA_NATIVE_BITE)
                && NativeBiteAttribution.confirmed(attempt, predator.getUUID(), target.getUUID(), biteReadyAfter)) {
            VampirePredationEngine.INSTANCE.onNativeFeed(predator, target, 1);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityDrinkBlood(BloodDrinkEvent.EntityDrinkBloodEvent event) {
        if (!runtimeAvailable()) return;
        LivingEntity predator = event.getVampire().asEntity();
        event.getBloodSource().getEntity().ifPresent(target ->
                VampirePredationEngine.INSTANCE.onNativeFeed(predator, target, event.getAmount()));
    }

    private boolean mcaBiteReady(Entity predator, Circuit owner) {
        if (!isMcaNamespace(predator) || !circuit(owner)) return false;
        try {
            Object optionalValue = capabilityGet.invoke(null, predator);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) return false;
            long now = predator.level().getGameTime();
            long cooldown = ((Number) biteCooldownTicks.invoke(null)).longValue();
            return (boolean) stateCanBite.invoke(optional.get(), now, cooldown);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(owner, "MCA bite cooldown query", exception);
            return false;
        }
    }

    private boolean markMcaBite(Entity predator) {
        if (!isMcaNamespace(predator) || !circuit(Circuit.MCA_ANIMAL_FEED)) return false;
        try {
            Object optionalValue = capabilityGet.invoke(null, predator);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) return false;
            stateMarkBite.invoke(optional.get(), predator.level().getGameTime());
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(Circuit.MCA_ANIMAL_FEED, "MCA bite cooldown mutation", exception);
            return false;
        }
    }

    private boolean invokeBoolean(Circuit owner, Method method, Object receiver, Object... arguments) {
        if (!circuit(owner)) return false;
        try {
            return (boolean) method.invoke(receiver, arguments);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(owner, method.getName(), exception);
            return false;
        }
    }

    private boolean circuit(Circuit circuit) {
        return circuits.get(circuit).get();
    }

    private static boolean isMcaNamespace(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && "mca".equals(id.getNamespace());
    }

    private void fail(Circuit circuit, String operation, Throwable exception) {
        circuits.get(circuit).set(false);
        if (failureLogged.get(circuit).compareAndSet(false, true)) {
            DarkFolkloreCore.LOGGER.warn("[compat/vampire_predation/{}] {} failed; only this capability fails closed",
                    circuit.name().toLowerCase(java.util.Locale.ROOT), operation, exception);
        }
    }

    @Override
    public void clearRuntimeState() {
        pendingNativeBites.clear();
        circuits.values().forEach(value -> value.set(true));
        failureLogged.values().forEach(value -> value.set(false));
    }

    private record EntityBloodContext(LivingEntity entity) implements IDrinkBloodContext {
        @Override public Optional<LivingEntity> getEntity() { return Optional.of(entity); }
        @Override public Optional<ItemStack> getStack() { return Optional.empty(); }
        @Override public Optional<BlockState> getBlockState() { return Optional.empty(); }
        @Override public Optional<net.minecraft.core.BlockPos> getBlockPos() { return Optional.empty(); }
    }
}
