package com.darkfolklore.core.compat.l2hostility;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

/**
 * Dormant L2 Hostility integration boundary.
 *
 * <p>0.8 deliberately performs no L2 runtime mutation, even when an existing config file still contains
 * {@code l2HostilityScaling=true}. The bridge stays in place only so diagnostics and a future exact-JAR audit
 * have a stable integration point.</p>
 */
public final class L2HostilityBridge {
    public static final L2HostilityBridge INSTANCE = new L2HostilityBridge();
    private static final String MOD_ID = "l2hostility";
    private static final boolean POLICY_ENABLED = false;

    private L2HostilityBridge() {}

    public ApplyResult apply(LivingEntity entity, String profile) {
        return new ApplyResult(Status.DISABLED, 0,
                POLICY_ENABLED ? "integration has no active implementation" : "disabled by Dark Folklore 0.8 policy");
    }

    public String diagnostics() {
        String installed = ModList.get().isLoaded(MOD_ID)
                ? ModList.get().getModContainerById(MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown")
                : "absent";
        return "DISABLED_BY_DARKFOLKLORE_POLICY installed=" + installed;
    }

    public void reset() {
        // No runtime state exists while the integration is policy-disabled.
    }

    public record ApplyResult(Status status, int level, String detail) {}
    public enum Status { DISABLED, APPLIED, ABSENT, NO_PROFILE, NOT_APPLICABLE, ERROR }
}
