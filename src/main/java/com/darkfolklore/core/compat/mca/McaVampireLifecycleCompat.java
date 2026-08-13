package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.CompatCapabilityCircuit;
import com.darkfolklore.core.compat.McaVampireLifecycleBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-mostly lifecycle bridge. State reads and the single idempotent provider mutation have separate circuits.
 */
public final class McaVampireLifecycleCompat implements McaVampireLifecycleBridge {
    private final CompatCapabilityCircuit reads = new CompatCapabilityCircuit("lifecycle-reads");
    private final CompatCapabilityCircuit mutation = new CompatCapabilityCircuit("native-ai-mutation");
    private final Method isMcaVillager;
    private final Method capabilityGet;
    private final Method isInfected;
    private final Method isConverted;
    private final Method isCuringVampire;
    private final Method isFactionInheritanceProcessed;
    private final Method isBiteWasConversionCause;
    private final Method areAiGoalsAdded;
    private final Method getSource;
    private final Method registerGoalsIfNeeded;

    public McaVampireLifecycleCompat() throws ReflectiveOperationException {
        ClassLoader loader = McaVampireLifecycleCompat.class.getClassLoader();
        Class<?> stateService = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireStateService", false, loader);
        Class<?> capabilities = Class.forName("com.guilh.mca_vampirism_compat.capability.ModCapabilities", false, loader);
        Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
        isMcaVillager = stateService.getMethod("isMcaVillager", Entity.class);
        capabilityGet = capabilities.getMethod("get", Entity.class);
        isInfected = state.getMethod("isInfected");
        isConverted = state.getMethod("isConverted");
        isCuringVampire = state.getMethod("isCuringVampire");
        isFactionInheritanceProcessed = state.getMethod("isFactionInheritanceProcessed");
        isBiteWasConversionCause = state.getMethod("isBiteWasConversionCause");
        areAiGoalsAdded = state.getMethod("areAiGoalsAdded");
        getSource = state.getMethod("getSource");
        reads.markReady("provider lifecycle state members resolved");

        Method mutationMethod = null;
        try {
            Class<?> ai = Class.forName("com.guilh.mca_vampirism_compat.ai.McaVampireAi", false, loader);
            mutationMethod = ai.getMethod("registerGoalsIfNeeded", LivingEntity.class);
            mutation.markReady("idempotent native AI member resolved");
        } catch (ReflectiveOperationException | LinkageError exception) {
            mutation.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Native AI mutation unavailable; reads remain active: {}",
                    exception.getClass().getSimpleName());
        }
        registerGoalsIfNeeded = mutationMethod;
    }

    @Override
    public boolean runtimeAvailable() { return reads.available(); }

    public boolean mutationAvailable() { return mutation.available() && registerGoalsIfNeeded != null; }

    public String diagnosticDetail() { return reads.detail() + ", " + mutation.detail(); }

    @Override
    public Snapshot snapshot(Entity entity) {
        if (!reads.available()) return Snapshot.unavailable("lifecycle read circuit open");
        try {
            boolean mca = (boolean) isMcaVillager.invoke(null, entity);
            if (!mca) {
                return new Snapshot(true, false, false, false, false,
                        false, false, false, Optional.empty(), "non-MCA entity");
            }
            Object value = capabilityGet.invoke(null, entity);
            if (!(value instanceof Optional<?> optional) || optional.isEmpty()) {
                return Snapshot.unavailable("MCA vampire capability absent");
            }
            Object state = optional.get();
            Object sourceValue = getSource.invoke(state);
            Optional<UUID> source = sourceValue instanceof Optional<?> sourceOptional
                    ? sourceOptional.filter(UUID.class::isInstance).map(UUID.class::cast)
                    : Optional.empty();
            return new Snapshot(true, true,
                    (boolean) isInfected.invoke(state),
                    (boolean) isConverted.invoke(state),
                    (boolean) isCuringVampire.invoke(state),
                    (boolean) isFactionInheritanceProcessed.invoke(state),
                    (boolean) isBiteWasConversionCause.invoke(state),
                    (boolean) areAiGoalsAdded.invoke(state),
                    source,
                    "audited MCA Vamp Compat lifecycle state");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            reads.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Lifecycle read failed; reads fail closed", exception);
            return Snapshot.unavailable("MCA Vamp Compat lifecycle query failed");
        }
    }

    @Override
    public boolean ensureNativeAi(LivingEntity entity) {
        if (!mutationAvailable()) return false;
        try {
            return (boolean) registerGoalsIfNeeded.invoke(null, entity);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            mutation.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Native AI mutation disabled; state reads remain active",
                    exception);
            return false;
        }
    }
}
