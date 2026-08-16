package com.darkfolklore.core.world;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.api.event.WorldEventChangedEvent;
import com.darkfolklore.core.config.FolkloreConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

public final class WorldEventDirector {
    public static final WorldEventDirector INSTANCE = new WorldEventDirector();
    private final Map<ResourceKey<Level>, EnumSet<WorldEventType>> active = new HashMap<>();

    private WorldEventDirector() {}

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!FolkloreConfig.WORLD_EVENTS.get() || event.getServer().getTickCount() % 200 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            set(level, WorldEventType.FULL_MOON, level.isNight() && level.getMoonPhase() == 0);
            long time = Math.floorMod(level.getDayTime(), 24000L);
            set(level, WorldEventType.WITCHING_HOUR, time >= 17500L && time <= 18500L);
        }
    }

    public boolean isActive(ServerLevel level, WorldEventType type) {
        return active.getOrDefault(level.dimension(), EnumSet.noneOf(WorldEventType.class)).contains(type);
    }

    public Set<WorldEventType> active(ServerLevel level) {
        return Set.copyOf(active.getOrDefault(level.dimension(), EnumSet.noneOf(WorldEventType.class)));
    }

    /** Prevents dimension-key state from one integrated/dedicated server leaking into the next server lifecycle. */
    public void clearRuntimeState() {
        active.clear();
    }

    private void set(ServerLevel level, WorldEventType type, boolean enabled) {
        EnumSet<WorldEventType> values = active.computeIfAbsent(level.dimension(), ignored -> EnumSet.noneOf(WorldEventType.class));
        boolean changed = enabled ? values.add(type) : values.remove(type);
        if (changed) {
            DarkFolkloreCore.LOGGER.info("[world_event] {} {} in {}", type, enabled ? "started" : "ended",
                    level.dimension().location());
            NeoForge.EVENT_BUS.post(new WorldEventChangedEvent(level, type, enabled));
        }
    }
}
