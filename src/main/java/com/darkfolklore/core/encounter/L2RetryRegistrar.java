package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = DarkFolkloreCore.MOD_ID)
public final class L2RetryRegistrar {
    private L2RetryRegistrar() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        ThreatPolicyRuntime.INSTANCE.onEntityTick(event);
    }
}
