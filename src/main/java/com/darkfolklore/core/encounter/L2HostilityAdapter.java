package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.config.FolkloreConfig;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Optional exact-version bridge for L2 Hostility. The integration never links against L2 at compile time and
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
                setLevel.invoke(cap, entity, minimumLevel);
                syncToClient.invoke(cap, entity);
                current = ((Number) getLevel.invoke(cap)).intValue();
            }
            return new ApplyResult(Status.APPLIED, current, "minimum difficulty satisfied");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            available = false;
            detail = "runtime bridge failed: " + exception.getClass().getSimpleName();
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
            Method type = Arrays.stream(mobAttachment.getClass().getMethods())
                    .filter(method -> method.getName().equals("type") && method.getParameterCount() == 0)
                    .findFirst().orElseThrow(() -> new NoSuchMethodException("LHMiscs.MOB.type"));
            attachmentType = type.invoke(mobAttachment);
            getExisting = Arrays.stream(attachmentType.getClass().getMethods())
                    .filter(method -> method.getName().equals("getExisting") && method.getParameterCount() == 1)
                    .findFirst().orElseThrow(() -> new NoSuchMethodException("MOB.type().getExisting"));

            Class<?> cap = Class.forName("dev.xkmc.l2hostility.content.capability.mob.MobTraitCap", false, loader);
            isInitialized = cap.getMethod("isInitialized");
            getLevel = cap.getMethod("getLevel");
            setLevel = cap.getMethod("setLevel", LivingEntity.class, int.class);
            syncToClient = cap.getMethod("syncToClient", LivingEntity.class);
            available = true;
            detail = "audited L2 Hostility " + actual + " attachment bridge active";
            DarkFolkloreCore.LOGGER.info("[compat/l2hostility] {}", detail);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            detail = "initialization failed: " + exception.getClass().getSimpleName();
            DarkFolkloreCore.LOGGER.warn("[compat/l2hostility] Optional integration disabled fail-closed", exception);
        }
    }

    public enum Status { APPLIED, RETRY, DISABLED, FAILED }

    public record ApplyResult(Status status, int resultingLevel, String detail) {
        public boolean retry() { return status == Status.RETRY; }
    }
}
