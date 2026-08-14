package com.darkfolklore.core.investigation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent link between a contract and the incident that created it.
 * Canonical target semantics remain in MonsterContract; this record only keeps
 * factual continuity metadata that 0.2/0.3 contracts did not persist.
 */
public record InvestigationCaseLink(
        Optional<UUID> storyId,
        Optional<UUID> culpritId,
        String observedImplementation,
        boolean culpritFallbackAllowed,
        boolean issuerFallbackAllowed
) {
    public InvestigationCaseLink {
        storyId = storyId == null ? Optional.empty() : storyId;
        culpritId = culpritId == null ? Optional.empty() : culpritId;
        observedImplementation = Objects.requireNonNullElse(observedImplementation, "");
        if (!observedImplementation.isBlank() && !observedImplementation.contains(":")) {
            throw new IllegalArgumentException("observedImplementation must be namespaced or blank");
        }
    }

    public static InvestigationCaseLink fromStory(UUID storyId, IncidentFact fact) {
        return new InvestigationCaseLink(Optional.ofNullable(storyId),
                fact == null ? Optional.empty() : fact.culpritId(),
                fact == null ? "" : fact.observedImplementation(), false, false);
    }

    /**
     * Replaces a previously unknown/legacy culprit with a newly observed exact manifestation.
     * A newly bound factual culprit always closes concept-level culprit fallback again.
     */
    public InvestigationCaseLink bindCulprit(UUID culprit, String implementation) {
        return new InvestigationCaseLink(storyId, Optional.of(Objects.requireNonNull(culprit)),
                Objects.requireNonNullElse(implementation, ""), false, issuerFallbackAllowed);
    }

    public InvestigationCaseLink allowCulpritFallback() {
        return culpritFallbackAllowed ? this : new InvestigationCaseLink(storyId, culpritId,
                observedImplementation, true, issuerFallbackAllowed);
    }

    public InvestigationCaseLink allowIssuerFallback() {
        return issuerFallbackAllowed ? this : new InvestigationCaseLink(storyId, culpritId,
                observedImplementation, culpritFallbackAllowed, true);
    }
}
