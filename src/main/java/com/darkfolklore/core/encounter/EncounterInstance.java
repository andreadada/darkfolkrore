package com.darkfolklore.core.encounter;

import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Mutable persisted state for one story-backed provider manifestation. */
public final class EncounterInstance {
    public static final int MAX_PARTICIPANTS = 16;

    private final UUID id;
    private final String definitionId;
    private final String concept;
    private final String implementation;
    private final EncounterRank rank;
    private final EncounterSpawnMode spawnMode;
    private final EncounterOrigin origin;
    private final WorldPosition anchor;
    private final String region;
    private final long createdAt;
    private final long expiresAt;
    private final LinkedHashSet<UUID> participants = new LinkedHashSet<>();

    private long nextStageAt;
    private EncounterStage stage;
    private int omensCompleted;
    private UUID manifestationEntity;
    private UUID storyId;
    private PersonSnapshot originPerson;
    private String resolution = "";

    public EncounterInstance(UUID id, String definitionId, String concept, String implementation,
                             EncounterRank rank, EncounterSpawnMode spawnMode, EncounterOrigin origin,
                             WorldPosition anchor, String region, long createdAt, long nextStageAt, long expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.definitionId = requireNamespaced(definitionId);
        this.concept = requireNamespaced(concept);
        this.implementation = requireNamespaced(implementation);
        this.rank = Objects.requireNonNull(rank);
        this.spawnMode = Objects.requireNonNull(spawnMode);
        this.origin = Objects.requireNonNull(origin);
        this.anchor = Objects.requireNonNull(anchor);
        this.region = Objects.requireNonNull(region);
        this.createdAt = Math.max(0L, createdAt);
        this.nextStageAt = Math.max(this.createdAt, nextStageAt);
        this.expiresAt = Math.max(this.nextStageAt + 1L, expiresAt);
        this.stage = EncounterStage.ORIGIN;
    }

    private static String requireNamespaced(String value) {
        if (value == null || !value.contains(":")) throw new IllegalArgumentException("namespaced id required");
        return value;
    }

    public UUID id() { return id; }
    public String definitionId() { return definitionId; }
    public String concept() { return concept; }
    public String implementation() { return implementation; }
    public EncounterRank rank() { return rank; }
    public EncounterSpawnMode spawnMode() { return spawnMode; }
    public EncounterOrigin origin() { return origin; }
    public WorldPosition anchor() { return anchor; }
    public String region() { return region; }
    public long createdAt() { return createdAt; }
    public long nextStageAt() { return nextStageAt; }
    public long expiresAt() { return expiresAt; }
    public EncounterStage stage() { return stage; }
    public int omensCompleted() { return omensCompleted; }
    public Optional<UUID> manifestationEntity() { return Optional.ofNullable(manifestationEntity); }
    public Optional<UUID> storyId() { return Optional.ofNullable(storyId); }
    public Optional<PersonSnapshot> originPerson() { return Optional.ofNullable(originPerson); }
    public Set<UUID> participants() { return Set.copyOf(participants); }
    public String resolution() { return resolution; }

    public boolean transition(EncounterStage next, long at) {
        if (stage.terminal() || next == null || next == stage || !valid(stage, next)) return false;
        stage = next;
        nextStageAt = Math.max(0L, at);
        return true;
    }

    private static boolean valid(EncounterStage from, EncounterStage to) {
        return switch (from) {
            case DORMANT -> to == EncounterStage.ORIGIN || to == EncounterStage.EXPIRED;
            case ORIGIN -> to == EncounterStage.OMENS || to == EncounterStage.EXPIRED;
            case OMENS -> to == EncounterStage.INVESTIGATING || to == EncounterStage.ELIGIBLE || to == EncounterStage.EXPIRED;
            case INVESTIGATING -> to == EncounterStage.OMENS || to == EncounterStage.ELIGIBLE || to == EncounterStage.EXPIRED;
            case ELIGIBLE -> to == EncounterStage.MANIFESTED || to == EncounterStage.EXPIRED;
            case MANIFESTED -> to == EncounterStage.ACTIVE || to == EncounterStage.ESCAPED || to == EncounterStage.RESOLVED;
            case ACTIVE -> to == EncounterStage.ESCAPED || to == EncounterStage.RESOLVED || to == EncounterStage.EXPIRED;
            case ESCAPED, RESOLVED, EXPIRED -> false;
        };
    }

    public int addOmen() { return ++omensCompleted; }

    public void restoreStage(EncounterStage stage, int omens, UUID entity, UUID persistedStoryId,
                             PersonSnapshot person, String resolution, long next) {
        this.stage = Objects.requireNonNull(stage);
        this.omensCompleted = Math.max(0, omens);
        this.manifestationEntity = entity;
        this.storyId = persistedStoryId;
        this.originPerson = person;
        this.resolution = resolution == null ? "" : resolution;
        this.nextStageAt = Math.max(0L, next);
        if (entity != null) addParticipant(entity);
    }

    /** Stage rescheduling must not erase an already-bound story identity. */
    public void restoreStage(EncounterStage stage, int omens, UUID entity, PersonSnapshot person,
                             String resolution, long next) {
        restoreStage(stage, omens, entity, this.storyId, person, resolution, next);
    }

    public void restoreParticipants(Collection<UUID> ids) {
        participants.clear();
        if (ids != null) {
            for (UUID id : ids) {
                if (participants.size() >= MAX_PARTICIPANTS) break;
                if (id != null) participants.add(id);
            }
        }
        if (manifestationEntity != null) addParticipant(manifestationEntity);
    }

    public void bindManifestation(UUID id) {
        manifestationEntity = Objects.requireNonNull(id);
        addParticipant(id);
    }

    public boolean addParticipant(UUID id) {
        Objects.requireNonNull(id);
        if (participants.contains(id)) return true;
        if (participants.size() >= MAX_PARTICIPANTS) return false;
        return participants.add(id);
    }

    public void clearManifestation() { manifestationEntity = null; }
    public void bindStory(UUID id) { storyId = Objects.requireNonNull(id); }
    public void setOriginPerson(PersonSnapshot person) { originPerson = person; }

    public void resolve(String reason) {
        stage = EncounterStage.RESOLVED;
        resolution = reason == null ? "resolved" : reason;
    }

    public void escape(String reason) {
        stage = EncounterStage.ESCAPED;
        resolution = reason == null ? "escaped" : reason;
    }

    public void expire(String reason) {
        if (!stage.terminal()) stage = EncounterStage.EXPIRED;
        resolution = reason == null ? "expired" : reason;
    }
}
