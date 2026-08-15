package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid = DarkFolkloreCore.MOD_ID)
public final class EncounterEventRegistrar {
    private EncounterEventRegistrar() {}

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        ThreatPolicyRuntime.INSTANCE.onPositionCheck(event);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        ThreatPolicyRuntime.INSTANCE.onEntityJoin(event);
    }
}
