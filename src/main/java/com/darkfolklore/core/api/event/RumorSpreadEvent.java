package com.darkfolklore.core.api.event;

import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeRecord;
import net.neoforged.bus.api.Event;

import java.util.UUID;

public final class RumorSpreadEvent extends Event {
    private final UUID sender;
    private final UUID recipient;
    private final UUID subject;
    private final SecretType secret;
    private final SocialKnowledgeRecord knowledge;

    public RumorSpreadEvent(UUID sender, UUID recipient, UUID subject,
                            SecretType secret, SocialKnowledgeRecord knowledge) {
        this.sender = sender; this.recipient = recipient; this.subject = subject;
        this.secret = secret; this.knowledge = knowledge;
    }

    public UUID sender() { return sender; }
    public UUID recipient() { return recipient; }
    public UUID subject() { return subject; }
    public SecretType secret() { return secret; }
    public SocialKnowledgeRecord knowledge() { return knowledge; }
}
