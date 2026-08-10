package com.darkfolklore.core.api.event;

import com.darkfolklore.core.knowledge.lore.LoreProgress;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class KnowledgeChangedEvent extends Event {
    private final ServerPlayer player;
    private final String concept;
    private final LoreProgress before;
    private final LoreProgress after;

    public KnowledgeChangedEvent(ServerPlayer player, String concept, LoreProgress before, LoreProgress after) {
        this.player = player; this.concept = concept; this.before = before; this.after = after;
    }

    public ServerPlayer player() { return player; }
    public String concept() { return concept; }
    public LoreProgress before() { return before; }
    public LoreProgress after() { return after; }
}
