package com.darkfolklore.core.society.organization;

import java.util.Objects;
import java.util.Set;

/** Validated organization defaults loaded atomically with the other society data. */
public record OrganizationArchetypeDefinition(OrganizationType type, int baseInfluence, int maxMembers,
                                              boolean autoFound, boolean publicRevealAuthority,
                                              Set<OrganizationObjective> objectives) {
    public OrganizationArchetypeDefinition {
        Objects.requireNonNull(type, "type");
        objectives = Set.copyOf(objectives);
        if (baseInfluence < 0 || baseInfluence > 100) {
            throw new IllegalArgumentException("organization base influence must be 0..100");
        }
        if (maxMembers < 1 || maxMembers > 256) {
            throw new IllegalArgumentException("organization max members must be 1..256");
        }
        if (objectives.isEmpty()) throw new IllegalArgumentException("organization objectives cannot be empty");
    }
}
