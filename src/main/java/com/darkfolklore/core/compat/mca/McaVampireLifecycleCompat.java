package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.CompatCapabilityCircuit;
import com.darkfolklore.core.compat.McaVampireLifecycleBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-mostly lifecycle bridge with independent core-state, metadata, AI-state and mutation capabilities.
 * Infection/conversion/cure observation remains usable when an optional lineage/cause member changes.
 */
public final class McaVampireLifecycleCompat implements McaVampireLifecycleBridge {
    private final CompatCapabilityCircuit coreReads = new CompatCapabilityCircuit("lifecycle-core");
    private final CompatCapabilityCircuit metadataReads = new CompatCapabilityCircuit("lifecycle-metadata");
    private final CompatCapabilityCircuit aiStateReads = new CompatCapabilityCircuit("lifecycle-ai-state");
    private final CompatCapabilityCircuit mutation = new CompatCapabilityCircuit("native-ai-mutation");

    private Method isMcaVillager;
    private Method capabilityGet;
    private Method isInfected;
    private Method isConverted;
    private Method isCuringVampire;

    private Method isFactionInheritanceProcessed;
    private Method isBiteWasConversionCause;
    private Method getSource;

    private Method areAiGoalsAdded;
    private Method registerGoalsIfNeeded;

    public McaVampireLifecycleCompat() {
        ClassLoader loader = McaVampireLifecycleCompat.class.getClassLoader();
        Class<?> state = null;
        try {
            Class<?> stateService = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireStateService", false, loader);
            Class<?> capabilities = Class.forName("com.guilh.mca_vampirism_compat.capability.ModCapabilities", false, loader);
            state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
            isMcaVillager = stateService.getMethod("isMcaVillager", Entity.class);
            capabilityGet = capabilities.getMethod("get", Entity.class);
            isInfected = state.getMethod("isInfected");
            isConverted = state.getMethod("isConverted");
            isCuringVampire = state.getMethod("isCuringVampire");
            coreReads.markReady("infection/conversion/cure members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            coreReads.fail(exception);
            metadataReads.fail("core lifecycle unavailable");
            aiStateReads.fail("core lifecycle unavailable");
            mutation.fail("core lifecycle unavailable");
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Core lifecycle state unavailable", exception);
            return;
        }

        try {
            isFactionInheritanceProcessed = state.getMethod("isFactionInheritanceProcessed");
            isBiteWasConversionCause = state.getMethod("isBiteWasConversionCause");
            getSource = state.getMethod("getSource");
            metadataReads.markReady("inheritance/cause/provenance members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            metadataReads.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Supplemental lifecycle metadata unavailable; core state remains active: {}",
                    exception.getClass().getSimpleName());
        }

        try {
            areAiGoalsAdded = state.getMethod("areAiGoalsAdded");
            aiStateReads.markReady("native AI state member resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            aiStateReads.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Native AI state read unavailable; core lifecycle remains active: {}",
                    exception.getClass().getSimpleName());
        }

        try {
            Class<?> ai = Class.forName("com.guilh.mca_vampirism_compat.ai.McaVampireAi", false, loader);
            registerGoalsIfNeeded = ai.getMethod("registerGoalsIfNeeded", LivingEntity.class);
            mutation.markReady("idempotent native AI member resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            mutation.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Native AI mutation unavailable; state reads remain active: {}",
                    exception.getClass().getSimpleName());
        }
    }

    @Override
    public boolean runtimeAvailable() { return coreReads.available(); }

    public boolean metadataAvailable() { return metadataReads.available(); }

    public boolean aiStateAvailable() { return aiStateReads.available() && areAiGoalsAdded != null; }

    public boolean mutationAvailable() { return mutation.available() && registerGoalsIfNeeded != null; }

    public Map<String, Boolean> circuitStatus() {
        return Map.of(
                "core", coreReads.available(),
                "metadata", metadataReads.available(),
                "ai_state", aiStateAvailable(),
                "ai_mutation", mutationAvailable());
    }

    public String diagnosticDetail() {
        return coreReads.detail() + ", " + metadataReads.detail() + ", " + aiStateReads.detail() + ", " + mutation.detail();
    }

    @Override
    public Snapshot snapshot(Entity entity) {
        if (!coreReads.available()) return Snapshot.unavailable("lifecycle core read circuit open");
        try {
            boolean mca = (boolean) isMcaVillager.invoke(null, entity);
            if (!mca) {
                return new Snapshot(true, false, false, false, false,
                        false, false, false, Optional.empty(), "non-MCA entity; " + diagnosticDetail());
            }
            Object value = capabilityGet.invoke(null, entity);
            if (!(value instanceof Optional<?> optional) || optional.isEmpty()) {
                return Snapshot.unavailable("MCA vampire capability absent; " + diagnosticDetail());
            }
            Object state = optional.get();

            boolean infected = (boolean) isInfected.invoke(state);
            boolean converted = (boolean) isConverted.invoke(state);
            boolean curing = (boolean) isCuringVampire.invoke(state);

            boolean inheritanceProcessed = false;
            boolean biteWasConversionCause = false;
            Optional<UUID> source = Optional.empty();
            if (metadataReads.available()) {
                try {
                    inheritanceProcessed = (boolean) isFactionInheritanceProcessed.invoke(state);
                    biteWasConversionCause = (boolean) isBiteWasConversionCause.invoke(state);
                    Object sourceValue = getSource.invoke(state);
                    source = sourceValue instanceof Optional<?> sourceOptional
                            ? sourceOptional.filter(UUID.class::isInstance).map(UUID.class::cast)
                            : Optional.empty();
                } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                    failSupplement(metadataReads, "supplemental lifecycle metadata query", exception);
                }
            }

            boolean aiGoalsAdded = false;
            if (aiStateAvailable()) {
                try {
                    aiGoalsAdded = (boolean) areAiGoalsAdded.invoke(state);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                    failSupplement(aiStateReads, "native AI state query", exception);
                }
            }

            return new Snapshot(true, true, infected, converted, curing,
                    inheritanceProcessed, biteWasConversionCause, aiGoalsAdded, source,
                    "MCA Vamp Compat lifecycle; " + diagnosticDetail());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            coreReads.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] Core lifecycle read failed; core reads fail closed",
                    exception);
            return Snapshot.unavailable("MCA Vamp Compat core lifecycle query failed");
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

    private void failSupplement(CompatCapabilityCircuit circuit, String operation, Throwable exception) {
        boolean first = circuit.available();
        circuit.fail(exception);
        if (first) {
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp_lifecycle] {} failed; core infection/conversion/cure reads remain active",
                    operation, exception);
        }
    }
}
