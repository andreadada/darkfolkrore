package com.darkfolklore.core.society.organization;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record OrganizationEvent(OrganizationEventType type, long gameTime,
                                Optional<UUID> actor, Optional<UUID> subject, String detail) {
    public OrganizationEvent {
        Objects.requireNonNull(type, "type");
        actor = actor == null ? Optional.empty() : actor;
        subject = subject == null ? Optional.empty() : subject;
        detail = Objects.requireNonNullElse(detail, "");
        if (detail.length() > 160) detail = detail.substring(0, 160);
    }

    public static OrganizationEvent of(OrganizationEventType type, long gameTime,
                                       UUID actor, UUID subject, String detail) {
        return new OrganizationEvent(type, gameTime, Optional.ofNullable(actor), Optional.ofNullable(subject), detail);
    }
}
