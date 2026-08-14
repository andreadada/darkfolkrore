package com.darkfolklore.core.compat.l2hostility;

import com.darkfolklore.core.encounter.L2HostilityAdapter;
import net.minecraft.world.entity.LivingEntity;

/**
 * Compatibility facade retained for the 0.8 diagnostics/API surface.
 * Active combat scaling is now owned by ThreatPolicyRuntime, which requests L2 Hostility level floors through
 * the audited L2HostilityAdapter. This facade deliberately does not translate legacy profile names into levels.
 */
public final class L2HostilityBridge {
    public static final L2HostilityBridge INSTANCE = new L2HostilityBridge();

    private L2HostilityBridge() {}

    public ApplyResult apply(LivingEntity entity, String profile) {
        return new ApplyResult(Status.DISABLED, 0,
                "legacy profile mutation disabled; encounter level floors are handled by ThreatPolicyRuntime");
    }

    public String diagnostics() {
        return L2HostilityAdapter.INSTANCE.diagnosticDetail();
    }

    public void reset() {
        L2HostilityAdapter.INSTANCE.clearRuntimeState();
    }

    public record ApplyResult(Status status, int level, String detail) {}
    public enum Status { DISABLED, APPLIED, ABSENT, NO_PROFILE, NOT_APPLICABLE, ERROR }
}
