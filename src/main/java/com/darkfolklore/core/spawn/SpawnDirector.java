package com.darkfolklore.core.spawn;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Owns hard semantic spawn-profile gates and encounter-pressure decay.
 * ThreatPolicyRuntime owns the single probabilistic natural-spawn roll and records pressure only after a confirmed
 * NATURAL spawn, so chunk reloads and provider-owned summons cannot manufacture encounter pressure.
 */
public final class SpawnDirector {
    public static final SpawnDirector INSTANCE = new SpawnDirector();
    private SpawnDirector() {}

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!FolkloreConfig.SPAWN_DIRECTOR.get() || event.getSpawnType() != MobSpawnType.NATURAL) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        SpawnProfile profile = FolkloreDataManager.INSTANCE.spawns().get(id).orElse(null);
        if (profile == null) return;
        if (!hardGateAllows(profile, event.getLevel().getLevel().isNight())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static boolean hardGateAllows(SpawnProfile profile, boolean night) {
        boolean suppressionEnabled = !profile.canonicalizationSuppression() || FolkloreConfig.CANONICALIZATION.get();
        return (profile.naturalSpawnEnabled() || !suppressionEnabled) && (!profile.nocturnal() || night);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        int decayInterval = Math.max(1, FolkloreConfig.ENCOUNTER_COOLDOWN.get() / 100);
        if (!FolkloreConfig.ENCOUNTER_DIRECTOR.get()
                || event.getServer().overworld().getGameTime() % decayInterval != 0) return;
        FolkloreSavedData data = FolkloreSavedData.get(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            int pressure = data.encounterPressure(player.getUUID());
            if (pressure > 0) data.setEncounterPressure(player.getUUID(), pressure - 1);
        }
    }
}
