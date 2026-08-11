package com.darkfolklore.core.contracts;

import com.darkfolklore.core.knowledge.social.EvidenceType;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class MonsterContract {
    private final UUID id;
    private final UUID issuer;
    private final String targetConcept;
    private final long expiresAt;
    private ContractStatus status = ContractStatus.OFFERED;
    private final EnumSet<EvidenceType> evidence = EnumSet.noneOf(EvidenceType.class);

    public MonsterContract(UUID id, UUID issuer, String targetConcept, long expiresAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.issuer = Objects.requireNonNull(issuer, "issuer");
        if (targetConcept == null || !targetConcept.contains(":")) {
            throw new IllegalArgumentException("targetConcept must be namespaced");
        }
        this.targetConcept = targetConcept;
        this.expiresAt = expiresAt;
    }

    public UUID id() { return id; }
    public UUID issuer() { return issuer; }
    public String targetConcept() { return targetConcept; }
    public long expiresAt() { return expiresAt; }
    public ContractStatus status() { return status; }
    public Set<EvidenceType> evidence() { return Set.copyOf(evidence); }

    public boolean start() { return transition(ContractStatus.OFFERED, ContractStatus.INVESTIGATING); }

    public boolean addEvidence(EvidenceType type, int requiredDistinctClues) {
        if (status != ContractStatus.INVESTIGATING) return false;
        boolean changed = recordEvidence(type);
        if (evidence.size() >= Math.max(1, requiredDistinctClues)) status = ContractStatus.IDENTIFIED;
        return changed;
    }

    /**
     * Records additional research evidence after the target has already been identified.
     * This is used by the 0.3 occult-analysis loop without changing the historical
     * INVESTIGATING -> IDENTIFIED threshold behavior.
     */
    public boolean recordEvidence(EvidenceType type) {
        if (status != ContractStatus.INVESTIGATING && status != ContractStatus.IDENTIFIED) return false;
        return evidence.add(Objects.requireNonNull(type));
    }

    public boolean markHunted() { return transition(ContractStatus.IDENTIFIED, ContractStatus.HUNTED); }
    public boolean complete() { return transition(ContractStatus.HUNTED, ContractStatus.COMPLETE); }

    public boolean expire(long now) {
        if (!status.terminal() && now >= expiresAt) {
            status = ContractStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public void restore(ContractStatus status, Set<EvidenceType> evidence) {
        this.status = Objects.requireNonNull(status);
        this.evidence.clear();
        this.evidence.addAll(evidence);
    }

    private boolean transition(ContractStatus expected, ContractStatus next) {
        if (status != expected) return false;
        status = next;
        return true;
    }
}
