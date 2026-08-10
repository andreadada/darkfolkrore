package com.darkfolklore.core.api.event;

import com.darkfolklore.core.knowledge.social.*;
import net.neoforged.bus.api.Event;

import java.util.UUID;

public final class SecretDiscoveredEvent extends Event {
    private final UUID observer;
    private final UUID subject;
    private final SecretType secret;
    private final SocialKnowledgeRecord knowledge;

    public SecretDiscoveredEvent(UUID observer, UUID subject, SecretType secret, SocialKnowledgeRecord knowledge) {
        this.observer = observer; this.subject = subject; this.secret = secret; this.knowledge = knowledge;
    }

    public UUID observer() { return observer; }
    public UUID subject() { return subject; }
    public SecretType secret() { return secret; }
    public SocialKnowledgeRecord knowledge() { return knowledge; }
}
