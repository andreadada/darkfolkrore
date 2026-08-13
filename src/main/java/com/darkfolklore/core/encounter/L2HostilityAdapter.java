package com.darkfolklore.core.encounter;

import net.minecraft.world.entity.LivingEntity;

public final class L2HostilityAdapter {
    public static final L2HostilityAdapter INSTANCE = new L2HostilityAdapter();
    private L2HostilityAdapter() {}

    public void applyMinimum(LivingEntity entity, int minimumLevel) {
    }
}
