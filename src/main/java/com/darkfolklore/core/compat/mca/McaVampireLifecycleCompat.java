package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.McaVampireLifecycleBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exact read-mostly adapter for MCA Reborn x Vampirism Compat 2.0.12. */
public final class McaVampireLifecycleCompat implements McaVampireLifecycleBridge {
    private final AtomicBoolean failureLogged = new AtomicBoolean();
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
        Class<?> ai = Class.forName("com.guilh.mca_vampirism_compat.ai.McaVampireAi", false, loader);

        isMcaVillager = stateService.getMethod("isMcaVillager", Entity.class);
        capabilityGet = capabilities.getMethod("get", Entity.class);
        isInfected = state.getMethod("isInfected");
        isConverted = state.getMethod("isConverted");
        isCuringVampire = state.getMethod("isCuringVampire");
        isFactionInheritanceProcessed = state.getMethod("isFactionInheritanceProcessed");
        isBiteWasConversionCause = state.getMethod("isBiteWasConversionCause");
        areAiGoalsAdded = state.getMethod("areAiGoalsAdded");
        getSource = state.getMethod("getSource");
        registerGoalsIfNeeded = ai.getMethod("registerGoalsIfNeeded", LivingEntity.class);
    }

    @Override
    public boolean runtimeAvailable() { return true; }

    @Override
    public Snapshot snapshot(Entity entity) {
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
                    "exact MCA Vamp Compat 2.0.12 state");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("snapshot", exception);
            return Snapshot.unavailable("MCA Vamp Compat lifecycle query failed");
        }
    }

    @Override
    public boolean ensureNativeAi(LivingEntity entity) {
        try {
            return (boolean) registerGoalsIfNeeded.invoke(null, entity);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("native AI registration", exception);
            return false;
        }
    }

    private void warnOnce(String operation, Exception exception) {
        if (failureLogged.compareAndSet(false, true)) {
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] {} failed; lifecycle observation fails closed",
                    operation, exception);
        }
    }
}
