package com.darkfolklore.core.encounter;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EncounterDefinition(String id, String concept, String implementation, EncounterRank rank,
        EncounterSpawnMode spawnMode, Set<EncounterOrigin> origins, int minimumOmens, boolean nightOnly,
        int minimumPlayerDistance, int maximumPlayerDistance, long omenIntervalTicks, long regionalCooldownTicks,
        long lifetimeTicks, List<EvidenceType> omenEvidence, String combatProfile) {
    public EncounterDefinition {
        id = requireResourceLocation(id, "id");
        concept = requireResourceLocation(concept, "concept");
        implementation = requireResourceLocation(implementation, "implementation");
        Objects.requireNonNull(rank);
        Objects.requireNonNull(spawnMode);
        origins = Set.copyOf(origins == null || origins.isEmpty() ? Set.of(EncounterOrigin.UNKNOWN) : origins);
        // The current state machine always emits at least one omen before eligibility; accepting zero would lie.
        minimumOmens = Math.max(1, Math.min(8, minimumOmens));
        minimumPlayerDistance = Math.max(8, minimumPlayerDistance);
        maximumPlayerDistance = Math.max(minimumPlayerDistance + 8, maximumPlayerDistance);
        omenIntervalTicks = Math.max(200L, omenIntervalTicks);
        regionalCooldownTicks = Math.max(1200L, regionalCooldownTicks);
        lifetimeTicks = Math.max(2400L, lifetimeTicks);
        omenEvidence = List.copyOf(omenEvidence == null ? List.of() : omenEvidence);
        combatProfile = combatProfile == null ? "" : combatProfile;
    }

    private static String requireResourceLocation(String value, String field) {
        if (value == null || ResourceLocation.tryParse(value) == null) {
            throw new IllegalArgumentException(field + " must be a valid namespaced resource location");
        }
        return value;
    }
}
