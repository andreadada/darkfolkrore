package com.darkfolklore.core.contracts;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import net.minecraft.resources.ResourceLocation;
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
        if (!validConcept(targetConcept)) throw new IllegalArgumentException("targetConcept must be a valid namespaced resource location");
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

    /** Legacy threshold path retained for compatibility and tests when Living Folklore is disabled. */
    public boolean addEvidence(EvidenceType type, int requiredDistinctClues) {
        if (status != ContractStatus.INVESTIGATING) return false;
        boolean changed = recordEvidence(type);
        if (changed && evidence.size() >= Math.max(1, requiredDistinctClues)) status = ContractStatus.IDENTIFIED;
        return changed;
    }

    public boolean recordEvidence(EvidenceType type) {
        if (status != ContractStatus.INVESTIGATING && status != ContractStatus.IDENTIFIED) return false;
        return evidence.add(Objects.requireNonNull(type));
    }

    /** 0.10 path: evidence is recorded first, then the independent hypothesis assessment may identify the case. */
    public boolean identify() { return transition(ContractStatus.INVESTIGATING, ContractStatus.IDENTIFIED); }
    public boolean markHunted() { return transition(ContractStatus.IDENTIFIED, ContractStatus.HUNTED); }
    public boolean complete() { return transition(ContractStatus.HUNTED, ContractStatus.COMPLETE); }

    public boolean expire(long now) {
        if (!status.terminal() && now >= expiresAt) { status = ContractStatus.EXPIRED; return true; }
        return false;
    }

    public void restore(ContractStatus status, Set<EvidenceType> evidence) {
        this.status = Objects.requireNonNull(status);
        this.evidence.clear(); this.evidence.addAll(evidence);
    }

    private boolean transition(ContractStatus expected, ContractStatus next) {
        if (status != expected) return false;
        status = next; return true;
    }

    private static boolean validConcept(String concept) {
        if (concept == null || !concept.contains(":")) return false;
        ResourceLocation id = ResourceLocation.tryParse(concept);
        return id != null && !id.getNamespace().isBlank() && !id.getPath().isBlank();
    }
}
