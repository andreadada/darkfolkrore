package com.darkfolklore.core.investigation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.magic.MagicTradition;
import com.darkfolklore.core.traits.CreatureTrait;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Curated investigation semantics for one canonical creature concept.
 * The profile describes evidence and research behavior only; it never replaces
 * the owning mod's creature mechanics.
 */
public record InvestigationProfile(
        String concept,
        Set<CreatureTrait> creatureTraits,
        Set<EvidenceType> signatures,
        Map<MagicTradition, EvidenceType> analysisResults,
        List<EvidenceType> incidentEvidence,
        int requiredEvidence,
        int trackingRadius
) {
    public InvestigationProfile {
        if (concept == null || !concept.contains(":")) {
            throw new IllegalArgumentException("concept must be namespaced");
        }
        creatureTraits = Set.copyOf(Objects.requireNonNull(creatureTraits));
        signatures = Set.copyOf(Objects.requireNonNull(signatures));
        analysisResults = Map.copyOf(Objects.requireNonNull(analysisResults));
        incidentEvidence = List.copyOf(Objects.requireNonNull(incidentEvidence));
        if (creatureTraits.isEmpty()) throw new IllegalArgumentException("creatureTraits cannot be empty");
        if (signatures.isEmpty()) throw new IllegalArgumentException("signatures cannot be empty");
        if (incidentEvidence.isEmpty()) throw new IllegalArgumentException("incidentEvidence cannot be empty");
        if (!signatures.containsAll(analysisResults.values())) {
            throw new IllegalArgumentException("analysisResults must resolve to declared signatures");
        }
        if (!signatures.containsAll(incidentEvidence)) {
            throw new IllegalArgumentException("incidentEvidence must be declared signatures");
        }
        requiredEvidence = Math.max(2, Math.min(8, requiredEvidence));
        trackingRadius = Math.max(16, Math.min(192, trackingRadius));
    }
}
