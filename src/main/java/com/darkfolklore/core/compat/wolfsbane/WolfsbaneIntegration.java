package com.darkfolklore.core.compat.wolfsbane;

import com.darkfolklore.core.DarkFolkloreCore;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Fail-closed bootstrap for the implementation-bound Wolfsbane bridge.
 *
 * <p>The event listener class that links Werewolves is loaded reflectively exactly once and only
 * after both installed versions and all audited registry IDs have been verified. This keeps the
 * core loadable when either optional mod is absent or upgraded.</p>
 */
public final class WolfsbaneIntegration {
    private static final String BRIDGE_CLASS =
            "com.darkfolklore.core.compat.wolfsbane.WerewolvesEnchantedWolfsbaneBridge";

    private static volatile Snapshot snapshot = Snapshot.beforeSetup();
    private static Object registeredListener;
    private static boolean initialized;

    private WolfsbaneIntegration() {}

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(WolfsbaneIntegration::initialize);
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        String werewolves = installedVersion("werewolves").orElse("-");
        String enchanted = installedVersion("enchanted").orElse("-");
        if ("-".equals(werewolves) || "-".equals(enchanted)) {
            snapshot = new Snapshot(State.DISABLED, werewolves, enchanted,
                    "Both optional mods are required; native bridge was not loaded", false, false, false);
            DarkFolkloreCore.LOGGER.info("[compat/wolfsbane] status={} werewolves={} enchanted={}",
                    snapshot.state(), werewolves, enchanted);
            return;
        }
        if (!WolfsbaneSemantics.supportsNativeBridge(werewolves, enchanted)) {
            snapshot = new Snapshot(State.UNTESTED_VERSION, werewolves, enchanted,
                    "Exact implementation signatures were not audited; native bridge was not loaded",
                    false, false, false);
            DarkFolkloreCore.LOGGER.warn("[compat/wolfsbane] status={} testedWerewolves={} actualWerewolves={} "
                            + "testedEnchanted={} actualEnchanted={}", snapshot.state(),
                    WolfsbaneSemantics.WEREWOLVES_VERSION, werewolves,
                    WolfsbaneSemantics.ENCHANTED_VERSION, enchanted);
            return;
        }
        if (!hasAuditedRegistrySurface()) {
            snapshot = new Snapshot(State.ERROR, werewolves, enchanted,
                    "An audited Wolfsbane registry ID is missing; native bridge failed closed",
                    false, false, false);
            DarkFolkloreCore.LOGGER.error("[compat/wolfsbane] Exact versions are present but an audited registry ID is missing");
            return;
        }

        try {
            Class<?> bridgeType = Class.forName(BRIDGE_CLASS, true, WolfsbaneIntegration.class.getClassLoader());
            registeredListener = bridgeType.getConstructor().newInstance();
            NeoForge.EVENT_BUS.register(registeredListener);
            snapshot = new Snapshot(State.ACTIVE, werewolves, enchanted,
                    "Exact-version event bridge registered", true, true, true);
            DarkFolkloreCore.LOGGER.info("[compat/wolfsbane] status={} diffuser=true contact=true finder=true",
                    snapshot.state());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : exception;
            Object failedListener = registeredListener;
            registeredListener = null;
            if (failedListener != null) {
                try {
                    NeoForge.EVENT_BUS.unregister(failedListener);
                } catch (RuntimeException cleanupFailure) {
                    cause.addSuppressed(cleanupFailure);
                }
            }
            snapshot = new Snapshot(State.ERROR, werewolves, enchanted,
                    "Native bridge failed to load: " + cause.getClass().getSimpleName(), false, false, false);
            DarkFolkloreCore.LOGGER.error("[compat/wolfsbane] Exact-version bridge failed closed", cause);
        }
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    private static Optional<String> installedVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    private static boolean hasAuditedRegistrySurface() {
        return hasItem(WolfsbaneSemantics.CANONICAL_ITEM)
                && hasItem(WolfsbaneSemantics.CANONICAL_SEEDS)
                && hasItem(WolfsbaneSemantics.LEGACY_ITEM)
                && hasItem(WolfsbaneSemantics.FINDER_ITEM)
                && hasBlock(WolfsbaneSemantics.CANONICAL_BLOCK)
                && hasBlock(WolfsbaneSemantics.LEGACY_BLOCK)
                && hasBlock(WolfsbaneSemantics.DIFFUSER_NORMAL_BLOCK)
                && hasBlock(WolfsbaneSemantics.DIFFUSER_LONG_BLOCK)
                && hasBlock(WolfsbaneSemantics.DIFFUSER_IMPROVED_BLOCK)
                && BuiltInRegistries.MOB_EFFECT.containsKey(
                        ResourceLocation.parse(WolfsbaneSemantics.WEREWOLVES_EFFECT))
                && BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(
                        ResourceLocation.parse(WolfsbaneSemantics.DIFFUSER_BLOCK_ENTITY));
    }

    private static boolean hasItem(String id) {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id));
    }

    private static boolean hasBlock(String id) {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id));
    }

    public enum State {
        BEFORE_SETUP,
        DISABLED,
        UNTESTED_VERSION,
        ACTIVE,
        ERROR
    }

    /** Only implementation-bound, runtime-validated claims are exposed as active flags. */
    public record Snapshot(
            State state,
            String actualWerewolvesVersion,
            String actualEnchantedVersion,
            String detail,
            boolean diffuserFuelBridge,
            boolean canonicalCropContactEffect,
            boolean finderCropLocator
    ) {
        private static Snapshot beforeSetup() {
            return new Snapshot(State.BEFORE_SETUP, "-", "-", "Common setup has not run",
                    false, false, false);
        }
    }
}
