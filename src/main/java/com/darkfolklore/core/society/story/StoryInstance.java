package com.darkfolklore.core.society.story;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class StoryInstance {
    private final UUID id;
    private final String template;
    private final String concept;
    private final long createdAt;
    private final long expiresAt;
    private final LinkedHashSet<UUID> actors = new LinkedHashSet<>();
    private StoryStatus status = StoryStatus.INCIDENT;

    public StoryInstance(UUID id, String template, String concept, long createdAt, long expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.template = Objects.requireNonNull(template);
        this.concept = Objects.requireNonNull(concept);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID id() { return id; }
    public String template() { return template; }
    public String concept() { return concept; }
    public long createdAt() { return createdAt; }
    public long expiresAt() { return expiresAt; }
    public Set<UUID> actors() { return Set.copyOf(actors); }
    public StoryStatus status() { return status; }
    public void addActor(UUID actor) { actors.add(Objects.requireNonNull(actor)); }

    public boolean advance(StoryStatus next) {
        boolean allowed = switch (status) {
            case INCIDENT -> next == StoryStatus.INVESTIGATING || next == StoryStatus.EXPIRED;
            case INVESTIGATING -> next == StoryStatus.CONFRONTATION || next == StoryStatus.RESOLVED || next == StoryStatus.EXPIRED;
            case CONFRONTATION -> next == StoryStatus.RESOLVED || next == StoryStatus.EXPIRED;
            case RESOLVED, EXPIRED -> false;
        };
        if (allowed) status = next;
        return allowed;
    }

    public boolean expire(long now) {
        return now >= expiresAt && !status.terminal() && advance(StoryStatus.EXPIRED);
    }

    public void restore(StoryStatus status, Set<UUID> actors) {
        this.status = Objects.requireNonNull(status);
        this.actors.clear();
        this.actors.addAll(actors);
    }
}
