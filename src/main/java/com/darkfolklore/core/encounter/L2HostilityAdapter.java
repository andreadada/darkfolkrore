package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.config.FolkloreConfig;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Optional exact-version bridge for L2 Hostility. Dark Folklore only requests a difficulty floor: L2 itself owns
 * health scaling, equipment and trait generation. The integration never links against L2 at compile time and
 * unknown versions fail closed. Entity attachment initialization is asynchronous, so callers may safely retry.
 */
public final class L2HostilityAdapter {
    public static final L2HostilityAdapter INSTANCE = new L2HostilityAdapter();
    public static final String MOD_ID = "l2hostility";
    public static final String TESTED_VERSION = "3.0.18";

    private boolean initialized;
    private boolean available;
    private String detail = "not initialized";
    private Object attachmentType;
    private Method getExisting;
    private Method isInitialized;
    private Method getLevel;
    private Method reinit;
    private Method setLevel;
    private Method syncToClient;

    private L2HostilityAdapter() {}

    public ApplyResult applyMinimum(LivingEntity entity, int minimumLevel) {
        if (!FolkloreConfig.L2_HOSTILITY_INTEGRATION.get() || minimumLevel <= 0) {
            return new ApplyResult(Status.DISABLED, 0, "integration disabled or no level requested");
        }
        ensureInitialized();
        if (!available) return new ApplyResult(Status.DISABLED, 0, detail);
        try {
            Object optionalValue = getExisting.invoke(attachmentType, entity);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) {
                return new ApplyResult(Status.RETRY, 0, "L2 mob attachment not available yet");
            }
            Object cap = optional.get();
            if (!(boolean) isInitialized.invoke(cap)) {
                return new ApplyResult(Status.RETRY, 0, "L2 mob attachment has not completed initialization");
            }
            int current = ((Number) getLevel.invoke(cap)).intValue();
            if (current < minimumLevel) {
                // Re-run L2's own initialization around the requested floor so L2, not Dark Folklore, selects
                // level-derived health/equipment/traits. A final setLevel only enforces the floor if variation
                // or an entity-specific cap produced a lower result.
                reinit.invoke(cap, entity, minimumLevel, false);
                current = ((Number) getLevel.invoke(cap)).intValue();
                if (current < minimumLevel) {
                    setLevel.invoke(cap, entity, minimumLevel);
                    current = ((Number) getLevel.invoke(cap)).intValue();
                }
                syncToClient.invoke(cap, entity);
            }
            return new ApplyResult(Status.APPLIED, current, "L2 difficulty floor satisfied; L2 owns combat scaling");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            available = false;
            detail = "runtime bridge failed: " + failureName(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/l2hostility] Runtime integration disabled fail-closed", exception);
            return new ApplyResult(Status.FAILED, 0, detail);
        }
    }

    public synchronized void clearRuntimeState() {
        initialized = false;
        available = false;
        detail = "not initialized";
        attachmentType = null;
        getExisting = null;
        isInitialized = null;
        getLevel = null;
        reinit = null;
        setLevel = null;
        syncToClient = null;
    }

    public String diagnosticDetail() {
        ensureInitialized();
        return detail;
    }

    private synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        Optional<? extends net.neoforged.fml.ModContainer> container = ModList.get().getModContainerById(MOD_ID);
        if (container.isEmpty()) {
            detail = "optional mod is not installed";
            return;
        }
        String actual = container.get().getModInfo().getVersion().toString();
        if (!TESTED_VERSION.equals(actual)) {
            detail = "untested version " + actual + " (audited " + TESTED_VERSION + ")";
            return;
        }
        try {
            ClassLoader loader = L2HostilityAdapter.class.getClassLoader();
            Class<?> misc = Class.forName("dev.xkmc.l2hostility.init.registrate.LHMiscs", false, loader);
            Field mobField = misc.getField("MOB");
            Object mobAttachment = mobField.get(null);

            // L2Core's AttVal is public, but its concrete Val implementation can be package-private. Resolving
            // type() from mobAttachment.getClass() therefore yields a Method whose declaring class is inaccessible
            // to Dark Folklore and Java 21 correctly throws IllegalAccessException. Resolve the same public method
            // from the public AttVal interface instead.
            Class<?> attVal = Class.forName("dev.xkmc.l2core.init.reg.simple.AttVal", false, loader);
            Method type = attVal.getMethod("type");
            attachmentType = type.invoke(mobAttachment);

            // Likewise resolve getExisting from L2Core's public holder class, never from an implementation class.
            Class<?> holder = Class.forName("dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder", false, loader);
            getExisting = Arrays.stream(holder.getMethods())
                    .filter(method -> method.getName().equals("getExisting") && method.getParameterCount() == 1)
                    .findFirst().orElseThrow(() -> new NoSuchMethodException("GeneralCapabilityHolder.getExisting"));

            Class<?> cap = Class.forName("dev.xkmc.l2hostility.content.capability.mob.MobTraitCap", false, loader);
            isInitialized = cap.getMethod("isInitialized");
            getLevel = cap.getMethod("getLevel");
            reinit = cap.getMethod("reinit", LivingEntity.class, int.class, boolean.class);
            setLevel = cap.getMethod("setLevel", LivingEntity.class, int.class);
            syncToClient = cap.getMethod("syncToClient", LivingEntity.class);
            available = true;
            detail = "audited L2 Hostility " + actual + " level bridge active; L2 owns combat scaling";
            DarkFolkloreCore.LOGGER.info("[compat/l2hostility] {}", detail);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            detail = "initialization failed: " + failureName(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/l2hostility] Optional integration disabled fail-closed", exception);
        }
    }

    private static String failureName(Throwable throwable) {
        Throwable cause = throwable;
        if (throwable instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            cause = invocation.getCause();
        }
        return cause.getClass().getSimpleName();
    }

    public enum Status { APPLIED, RETRY, DISABLED, FAILED }

    public record ApplyResult(Status status, int resultingLevel, String detail) {
        public boolean retry() { return status == Status.RETRY; }
    }
}
