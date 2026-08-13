package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.persistence.WorldPosition;
import java.util.*;

public final class InvestigationCaseRecord {
    public static final int HARD_MAX_NOTES = 256;
    private final UUID id;
    private final UUID player;
    private final UUID contractId;
    private final Optional<UUID> storyId;
    private final CaseOrigin origin;
    private final WorldPosition anchor;
    private final long createdAt;
    private long updatedAt;
    private long expiresAt;
    private CaseStage stage;
    private final EnumSet<EvidenceType> evidence = EnumSet.noneOf(EvidenceType.class);
    private final ArrayDeque<CaseNote> notes = new ArrayDeque<>();
    private String identifiedConcept = "";

    public InvestigationCaseRecord(UUID id, UUID player, UUID contractId, Optional<UUID> storyId,
                                   CaseOrigin origin, WorldPosition anchor, long createdAt, long expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.player = Objects.requireNonNull(player);
        this.contractId = Objects.requireNonNull(contractId);
        this.storyId = storyId == null ? Optional.empty() : storyId;
        this.origin = Objects.requireNonNull(origin);
        this.anchor = Objects.requireNonNull(anchor);
        this.createdAt = Math.max(0L, createdAt);
        this.updatedAt = this.createdAt;
        this.expiresAt = Math.max(this.createdAt, expiresAt);
        this.stage = CaseStage.INVESTIGATING;
    }

    public UUID id() { return id; }
    public UUID player() { return player; }
    public UUID contractId() { return contractId; }
    public Optional<UUID> storyId() { return storyId; }
    public CaseOrigin origin() { return origin; }
    public WorldPosition anchor() { return anchor; }
    public long createdAt() { return createdAt; }
    public long updatedAt() { return updatedAt; }
    public long expiresAt() { return expiresAt; }
    public CaseStage stage() { return stage; }
    public Set<EvidenceType> evidence() { return Set.copyOf(evidence); }
    public List<CaseNote> notes() { return List.copyOf(notes); }
    public Optional<String> identifiedConcept() { return identifiedConcept.isBlank() ? Optional.empty() : Optional.of(identifiedConcept); }

    public boolean addEvidence(EvidenceType type, long now) {
        boolean changed = evidence.add(Objects.requireNonNull(type));
        if (changed) updatedAt = Math.max(updatedAt, now);
        return changed;
    }

    public void addNote(CaseNote note, int configuredLimit) {
        notes.addLast(Objects.requireNonNull(note));
        int limit = Math.max(1, Math.min(HARD_MAX_NOTES, configuredLimit));
        while (notes.size() > limit) notes.removeFirst();
        updatedAt = Math.max(updatedAt, note.gameTime());
    }

    public boolean advance(CaseStage next, long now) {
        if (!stage.mayAdvanceTo(next)) return false;
        stage = next;
        updatedAt = Math.max(updatedAt, now);
        return true;
    }

    public boolean identify(String concept, long now) {
        if (concept == null || !concept.contains(":")) return false;
        if (stage.ordinal() > CaseStage.IDENTIFIED.ordinal() && !stage.terminal()) return false;
        identifiedConcept = concept;
        if (stage.ordinal() < CaseStage.IDENTIFIED.ordinal()) stage = CaseStage.IDENTIFIED;
        updatedAt = Math.max(updatedAt, now);
        return true;
    }

    public void extendExpiry(long value) { expiresAt = Math.max(expiresAt, value); }

    public void restore(CaseStage restoredStage, Collection<EvidenceType> restoredEvidence,
                        Collection<CaseNote> restoredNotes, String restoredConcept, long restoredUpdatedAt) {
        stage = restoredStage == null ? CaseStage.INVESTIGATING : restoredStage;
        evidence.clear();
        if (restoredEvidence != null) evidence.addAll(restoredEvidence);
        notes.clear();
        if (restoredNotes != null) restoredNotes.stream().skip(Math.max(0, restoredNotes.size() - HARD_MAX_NOTES)).forEach(notes::addLast);
        identifiedConcept = Objects.requireNonNullElse(restoredConcept, "");
        updatedAt = Math.max(createdAt, restoredUpdatedAt);
    }
}
