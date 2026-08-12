package com.darkfolklore.core.api.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.Event;

/**
 * Posted one server tick after a {@code LivingDeathEvent}, but only when the original event
 * remained uncancelled and the entity is still non-alive. All Dark Folklore state that depends
 * on final death must be driven from this event rather than the cancellable provider callback.
 */
public final class ConfirmedLivingDeathEvent extends Event {
    private final MinecraftServer server;
    private final LivingEntity entity;
    private final DamageSource source;

    public ConfirmedLivingDeathEvent(MinecraftServer server, LivingEntity entity, DamageSource source) {
        this.server = server;
        this.entity = entity;
        this.source = source;
    }

    public MinecraftServer server() { return server; }
    public LivingEntity entity() { return entity; }
    public DamageSource source() { return source; }
}
