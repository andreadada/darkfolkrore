package com.darkfolklore.core.endgame;

import com.darkfolklore.core.persistence.WorldPosition;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistent state for one player-built invocation site. */
public final class DemonInvocationSite {
    public static final int MAX_PARTICIPANTS = 16;
    private final UUID id;
    private final UUID owner;
    private final WorldPosition anchor;
    private final long createdAt;
    private final LinkedHashSet<UUID> participants = new LinkedHashSet<>();
    private DemonInvocationState state;
    private UUID currentBoss;
    private long completedAt;

    public DemonInvocationSite(UUID id, UUID owner, WorldPosition anchor, long createdAt, UUID initialBoss) {
        this.id = id;
        this.owner = owner;
        this.anchor = anchor;
        this.createdAt = createdAt;
        this.state = DemonInvocationState.ACTIVE;
        this.currentBoss = initialBoss;
        if (initialBoss != null) participants.add(initialBoss);
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public WorldPosition anchor() { return anchor; }
    public long createdAt() { return createdAt; }
    public DemonInvocationState state() { return state; }
    public Optional<UUID> currentBoss() { return Optional.ofNullable(currentBoss); }
    public Set<UUID> participants() { return Set.copyOf(participants); }
    public long completedAt() { return completedAt; }

    public boolean addParticipant(UUID entity) {
        if (entity == null) return false;
        if (participants.contains(entity)) { currentBoss = entity; return true; }
        if (participants.size() >= MAX_PARTICIPANTS) return false;
        participants.add(entity);
        currentBoss = entity;
        return true;
    }

    public void complete(long now) { state = DemonInvocationState.COMPLETED; completedAt = Math.max(0L, now); }
    public void fail(long now) { state = DemonInvocationState.FAILED; completedAt = Math.max(0L, now); }

    void restore(DemonInvocationState restoredState, UUID restoredCurrentBoss, long restoredCompletedAt,
                 Collection<UUID> restoredParticipants) {
        state = restoredState == null ? DemonInvocationState.ACTIVE : restoredState;
        currentBoss = restoredCurrentBoss;
        completedAt = Math.max(0L, restoredCompletedAt);
        participants.clear();
        if (restoredParticipants != null) {
            for (UUID participant : restoredParticipants) {
                if (participant == null || participants.size() >= MAX_PARTICIPANTS) break;
                participants.add(participant);
            }
        }
        if (currentBoss != null && participants.size() < MAX_PARTICIPANTS) participants.add(currentBoss);
    }
}
