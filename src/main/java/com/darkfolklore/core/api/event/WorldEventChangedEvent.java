package com.darkfolklore.core.api.event;

import com.darkfolklore.core.world.WorldEventType;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public final class WorldEventChangedEvent extends Event {
    private final ServerLevel level;
    private final WorldEventType type;
    private final boolean active;

    public WorldEventChangedEvent(ServerLevel level, WorldEventType type, boolean active) {
        this.level = level; this.type = type; this.active = active;
    }

    public ServerLevel level() { return level; }
    public WorldEventType type() { return type; }
    public boolean active() { return active; }
}
