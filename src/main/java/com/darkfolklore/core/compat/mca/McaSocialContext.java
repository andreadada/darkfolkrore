package com.darkfolklore.core.compat.mca;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** A read-only snapshot of the social evidence used for one observer/source pair. */
public record McaSocialContext(
        McaRelationshipCategory relationship,
        OptionalInt playerAffinityHearts,
        Optional<String> observerPersonality,
        Optional<String> sourcePersonality,
        String detail
) {
    public McaSocialContext {
        Objects.requireNonNull(relationship, "relationship");
        Objects.requireNonNull(playerAffinityHearts, "playerAffinityHearts");
        Objects.requireNonNull(observerPersonality, "observerPersonality");
        Objects.requireNonNull(sourcePersonality, "sourcePersonality");
        Objects.requireNonNull(detail, "detail");
    }

    public static McaSocialContext unavailable(String detail) {
        return new McaSocialContext(McaRelationshipCategory.UNKNOWN, OptionalInt.empty(), Optional.empty(),
                Optional.empty(), detail);
    }

    public static McaSocialContext notApplicable(String detail) {
        return new McaSocialContext(McaRelationshipCategory.NOT_APPLICABLE, OptionalInt.empty(), Optional.empty(),
                Optional.empty(), detail);
    }
}
