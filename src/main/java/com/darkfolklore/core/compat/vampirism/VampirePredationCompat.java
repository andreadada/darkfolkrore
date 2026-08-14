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

/**
 * Predation bridge whose wild Vampirism capability is independent from the optional MCA provider surface.
 * MCA members are resolved in small runtime-probed groups; a provider mismatch cannot disable ordinary
 * {@link IVampireMob} recognition or feeding.
 */
public final class VampirePredationCompat implements VampirePredationBridge {
    private enum Circuit { WILD_FEED, MCA_FACTS, MCA_TARGET, MCA_ANIMAL_FEED, MCA_NATIVE_BITE }

    private final EnumMap<Circuit, AtomicBoolean> circuits = new EnumMap<>(Circuit.class);
    private final EnumMap<Circuit, AtomicBoolean> failureLogged = new EnumMap<>(Circuit.class);
    private final EnumMap<Circuit, Boolean> baseline = new EnumMap<>(Circuit.class);
    private final NativeBiteAttribution.PendingAttempts<LivingIncomingDamageEvent> pendingNativeBites =
            new NativeBiteAttribution.PendingAttempts<>();

    private final McaFactsMethods mcaFacts;
    private final McaTargetMethods mcaTarget;
    private final McaAnimalMethods mcaAnimal;
    private final String initializationDetail;

    /** Compatibility constructor retained for reflective callers; MCA surface is probed when present. */
    public VampirePredationCompat() {
        this(true);
    }

    /**
     * @param enableMcaBridge true only when CompatibilityManager has admitted the MCA/MCA-Vamp provider stack.
     */
    public VampirePredationCompat(boolean enableMcaBridge) {
        for (Circuit circuit : Circuit.values()) {
            circuits.put(circuit, new AtomicBoolean(false));
            failureLogged.put(circuit, new AtomicBoolean(false));
            baseline.put(circuit, false);
        }

        // Vampirism 1.10.12 is the only prerequisite for the wild path. Never tie this to MCA compatibility.
        baseline.put(Circuit.WILD_FEED, true);

        ClassLoader loader = VampirePredationCompat.class.getClassLoader();
        McaFactsMethods facts = null;
        McaTargetMethods target = null;
        McaAnimalMethods animal = null;
        String detail;

        if (enableMcaBridge) {
            try {
                facts = resolveFacts(loader);
                baseline.put(Circuit.MCA_FACTS, true);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                DarkFolkloreCore.LOGGER.warn("[compat/vampire_predation/mca_facts] Runtime probe failed; wild Vampirism remains active",
                        exception);
            }

            if (facts != null) {
                try {
                    target = resolveTarget(loader);
                    baseline.put(Circuit.MCA_TARGET, true);
                    baseline.put(Circuit.MCA_NATIVE_BITE, true);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                    DarkFolkloreCore.LOGGER.warn("[compat/vampire_predation/mca_target] Runtime probe failed; MCA targeting disabled only",
                            exception);
                }

                if (target != null) {
                    try {
                        animal = resolveAnimal(loader);
                        baseline.put(Circuit.MCA_ANIMAL_FEED, true);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                        DarkFolkloreCore.LOGGER.warn("[compat/vampire_predation/mca_animal_feed] Runtime probe failed; MCA animal feeding disabled only",
                                exception);
                    }
                }
            }
            detail = "wild Vampirism active; MCA probes facts=" + baseline.get(Circuit.MCA_FACTS)
                    + ", target=" + baseline.get(Circuit.MCA_TARGET)
                    + ", animal=" + baseline.get(Circuit.MCA_ANIMAL_FEED)
                    + ", nativeBite=" + baseline.get(Circuit.MCA_NATIVE_BITE);
        } else {
            detail = "wild Vampirism active; MCA predation not admitted by compatibility manager";
        }

        mcaFacts = facts;
        mcaTarget = target;
        mcaAnimal = animal;
        initializationDetail = detail;
        restoreBaseline();
    }

    @Override
    public boolean runtimeAvailable() {
        return wildRuntimeAvailable() || mcaRuntimeAvailable();
    }

    @Override
    public boolean wildRuntimeAvailable() {
        return circuit(Circuit.WILD_FEED);
    }

    @Override
    public boolean mcaRuntimeAvailable() {
        return mcaFacts != null && mcaTarget != null
                && circuit(Circuit.MCA_FACTS) && circuit(Circuit.MCA_TARGET);
    }

    @Override
    public String runtimeDetail() {
        return initializationDetail;
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
            if (!circuit(Circuit.MCA_FACTS) || mcaFacts == null) return PredatorKind.NONE;
            return invokeBoolean(Circuit.MCA_FACTS, mcaFacts.mcaIsVillager(), null, entity)
                    && invokeBoolean(Circuit.MCA_FACTS, mcaFacts.mcaIsVampire(), null, entity)
                    ? PredatorKind.MCA_VAMPIRE : PredatorKind.NONE;
        }
        return wildRuntimeAvailable() && entity instanceof IVampireMob
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
                if (!wildRuntimeAvailable()) return false;
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
        if (isMcaNamespace(predator) || !wildRuntimeAvailable()
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
        if (isMcaNamespace(predator) || !wildRuntimeAvailable()
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
    public boolean requestWildCombatTarget(Mob predator, LivingEntity target) {
        if (isMcaNamespace(predator) || !wildRuntimeAvailable()
                || !(predator instanceof IVampireMob) || !target.isAlive()) return false;
        LivingEntity current = predator.getTarget();
        boolean currentAlive = current != null && current.isAlive();
        boolean currentIsChosen = current != null && current.getUUID().equals(target.getUUID());
        if (currentAlive && !currentIsChosen) return false;
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
        if (isMcaNamespace(predator) || !wildRuntimeAvailable()
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
        return isMcaNamespace(predator) && mcaTarget != null && circuit(Circuit.MCA_TARGET)
                && invokeBoolean(Circuit.MCA_TARGET, mcaTarget.targetEligible(), null, predator, target);
    }

    @Override
    public boolean canMcaAnimalFeed(Mob predator, LivingEntity target) {
        if (!isMcaNamespace(predator) || mcaAnimal == null || !circuit(Circuit.MCA_ANIMAL_FEED)
                || !circuit(Circuit.MCA_FACTS)) return false;
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
        return isMcaNamespace(entity) && mcaFacts != null && mcaFacts.recentlyBitten() != null
                && circuit(Circuit.MCA_FACTS)
                && invokeBoolean(Circuit.MCA_FACTS, mcaFacts.recentlyBitten(), null, entity);
    }

    @Override
    public ProviderSnapshot providerSnapshot(Entity entity) {
        if (!isMcaNamespace(entity)) {
            boolean available = wildRuntimeAvailable();
            return new ProviderSnapshot(available, false, available && entity instanceof IVampireMob,
                    false, false, false, false, false,
                    available ? "Vampirism wild-mob bridge active" : "wild Vampirism circuit open");
        }
        if (!circuit(Circuit.MCA_FACTS) || mcaFacts == null) {
            return ProviderSnapshot.unavailable("MCA fact circuit unavailable");
        }
        try {
            boolean mca = (boolean) mcaFacts.mcaIsVillager().invoke(null, entity);
            if (!mca) return new ProviderSnapshot(true, false, false,
                    false, false, false, false, false, "MCA namespace entity is not an MCA villager");
            boolean vampire = (boolean) mcaFacts.mcaIsVampire().invoke(null, entity);
            Object optionalValue = mcaFacts.capabilityGet().invoke(null, entity);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) {
                return new ProviderSnapshot(true, true, vampire, false, false, false, false, false,
                        "MCA capability absent");
            }
            Object state = optional.get();
            boolean recent = entity instanceof LivingEntity living && mcaFacts.recentlyBitten() != null
                    && (boolean) mcaFacts.recentlyBitten().invoke(null, living);
            return new ProviderSnapshot(true, true, vampire,
                    (boolean) mcaFacts.stateInfected().invoke(state),
                    (boolean) mcaFacts.stateConverted().invoke(state),
                    (boolean) mcaFacts.stateCuring().invoke(state), recent,
                    (boolean) mcaFacts.stateAiAdded().invoke(state),
                    "runtime-probed MCA Vamp Compat state");
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
        if (!isMcaNamespace(predator) || !circuit(owner) || mcaFacts == null || mcaTarget == null) return false;
        try {
            Object optionalValue = mcaFacts.capabilityGet().invoke(null, predator);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) return false;
            long now = predator.level().getGameTime();
            long cooldown = ((Number) mcaTarget.biteCooldownTicks().invoke(null)).longValue();
            return (boolean) mcaTarget.stateCanBite().invoke(optional.get(), now, cooldown);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(owner, "MCA bite cooldown query", exception);
            return false;
        }
    }

    private boolean markMcaBite(Entity predator) {
        if (!isMcaNamespace(predator) || !circuit(Circuit.MCA_ANIMAL_FEED)
                || mcaFacts == null || mcaAnimal == null) return false;
        try {
            Object optionalValue = mcaFacts.capabilityGet().invoke(null, predator);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) return false;
            mcaAnimal.stateMarkBite().invoke(optional.get(), predator.level().getGameTime());
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(Circuit.MCA_ANIMAL_FEED, "MCA bite cooldown mutation", exception);
            return false;
        }
    }

    private boolean invokeBoolean(Circuit owner, Method method, Object receiver, Object... arguments) {
        if (!circuit(owner) || method == null) return false;
        try {
            return (boolean) method.invoke(receiver, arguments);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(owner, method.getName(), exception);
            return false;
        }
    }

    private boolean circuit(Circuit circuit) {
        AtomicBoolean state = circuits.get(circuit);
        return state != null && state.get();
    }

    private static boolean isMcaNamespace(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && "mca".equals(id.getNamespace());
    }

    private void fail(Circuit circuit, String operation, Throwable exception) {
        disable(circuit);
        if (circuit == Circuit.MCA_FACTS) {
            disable(Circuit.MCA_TARGET);
            disable(Circuit.MCA_ANIMAL_FEED);
            disable(Circuit.MCA_NATIVE_BITE);
        } else if (circuit == Circuit.MCA_TARGET) {
            disable(Circuit.MCA_ANIMAL_FEED);
            disable(Circuit.MCA_NATIVE_BITE);
        }
        if (failureLogged.get(circuit).compareAndSet(false, true)) {
            DarkFolkloreCore.LOGGER.warn("[compat/vampire_predation/{}] {} failed; only this capability and its dependents fail closed",
                    circuit.name().toLowerCase(java.util.Locale.ROOT), operation, exception);
        }
    }

    private void disable(Circuit circuit) {
        AtomicBoolean state = circuits.get(circuit);
        if (state != null) state.set(false);
    }

    private void restoreBaseline() {
        for (Circuit circuit : Circuit.values()) {
            circuits.get(circuit).set(Boolean.TRUE.equals(baseline.get(circuit)));
        }
    }

    @Override
    public void clearRuntimeState() {
        pendingNativeBites.clear();
        restoreBaseline();
        failureLogged.values().forEach(value -> value.set(false));
    }

    private static McaFactsMethods resolveFacts(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> stateService = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireStateService", false, loader);
        Class<?> capabilities = Class.forName("com.guilh.mca_vampirism_compat.capability.ModCapabilities", false, loader);
        Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);

        Method recent = null;
        try {
            Class<?> bite = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireBiteService", false, loader);
            recent = bite.getMethod("wasRecentlyBitten", LivingEntity.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            DarkFolkloreCore.LOGGER.info("[compat/vampire_predation/mca_facts] Optional recent-bite probe unavailable: {}",
                    exception.getClass().getSimpleName());
        }

        return new McaFactsMethods(
                stateService.getMethod("isMcaVillager", Entity.class),
                stateService.getMethod("isVampire", Entity.class),
                recent,
                capabilities.getMethod("get", Entity.class),
                state.getMethod("isInfected"),
                state.getMethod("isConverted"),
                state.getMethod("isCuringVampire"),
                state.getMethod("areAiGoalsAdded")
        );
    }

    private static McaTargetMethods resolveTarget(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> targetUtil = Class.forName("com.guilh.mca_vampirism_compat.util.McaVampireTargetUtil", false, loader);
        Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
        Class<?> config = Class.forName("com.guilh.mca_vampirism_compat.config.McaVampirismCompatConfig", false, loader);
        return new McaTargetMethods(
                targetUtil.getMethod("isInfectionBiteTarget", Mob.class, LivingEntity.class),
                state.getMethod("canBite", long.class, long.class),
                config.getMethod("biteCooldownTicks")
        );
    }

    private static McaAnimalMethods resolveAnimal(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
        return new McaAnimalMethods(state.getMethod("markBite", long.class));
    }

    private record McaFactsMethods(Method mcaIsVillager, Method mcaIsVampire, Method recentlyBitten,
                                   Method capabilityGet, Method stateInfected, Method stateConverted,
                                   Method stateCuring, Method stateAiAdded) {}

    private record McaTargetMethods(Method targetEligible, Method stateCanBite, Method biteCooldownTicks) {}

    private record McaAnimalMethods(Method stateMarkBite) {}

    private record EntityBloodContext(LivingEntity entity) implements IDrinkBloodContext {
        @Override public Optional<LivingEntity> getEntity() { return Optional.of(entity); }
        @Override public Optional<ItemStack> getStack() { return Optional.empty(); }
        @Override public Optional<BlockState> getBlockState() { return Optional.empty(); }
        @Override public Optional<net.minecraft.core.BlockPos> getBlockPos() { return Optional.empty(); }
    }
}
