package com.darkfolklore.core.api.event;

import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

public final class WitnessEvent extends Event {
    private final ServerLevel level;
    private final Entity actor;
    private final LivingEntity observer;
    private final SecretType secret;
    private final SocialKnowledgeState state;

    public WitnessEvent(ServerLevel level, Entity actor, LivingEntity observer,
                        SecretType secret, SocialKnowledgeState state) {
        this.level = level; this.actor = actor; this.observer = observer; this.secret = secret; this.state = state;
    }

    public ServerLevel level() { return level; }
    public Entity actor() { return actor; }
    public LivingEntity observer() { return observer; }
    public SecretType secret() { return secret; }
    public SocialKnowledgeState state() { return state; }
}
