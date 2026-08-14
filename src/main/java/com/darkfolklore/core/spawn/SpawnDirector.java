package com.darkfolklore.core.spawn;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;

/**
 * Owns hard semantic spawn-profile gates and encounter-pressure accounting.
 * The single probabilistic natural-spawn roll is owned by ThreatPolicyRuntime so a mob is never filtered twice.
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
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!FolkloreConfig.ENCOUNTER_DIRECTOR.get() || !(event.getLevel() instanceof ServerLevel level)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        SpawnProfile profile = FolkloreDataManager.INSTANCE.spawns().get(id).orElse(null);
        if (profile == null || profile.rarity().ordinal() < SpawnRarity.RARE.ordinal()) return;
        ServerPlayer nearest = level.players().stream().filter(player -> player.distanceToSqr(event.getEntity()) < 16384.0D)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(event.getEntity()))).orElse(null);
        if (nearest != null) {
            FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
            data.setEncounterPressure(nearest.getUUID(), data.encounterPressure(nearest.getUUID()) + 15);
        }
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
