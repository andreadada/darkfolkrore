package com.darkfolklore.core.compat.mcacapitals;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/** Immutable, dependency-free snapshot safe to retain outside the optional adapter. */
public record PoliticalContext(
        PoliticalLookupStatus status,
        PoliticalRole role,
        Optional<UUID> capitalId,
        OptionalInt mcaVillageId,
        Optional<String> capitalState,
        Optional<String> exactTitle,
        String detail
) {
    public PoliticalContext {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(capitalId, "capitalId");
        Objects.requireNonNull(mcaVillageId, "mcaVillageId");
        Objects.requireNonNull(capitalState, "capitalState");
        Objects.requireNonNull(exactTitle, "exactTitle");
        Objects.requireNonNull(detail, "detail");
    }

    public static PoliticalContext disabled(String detail) {
        return empty(PoliticalLookupStatus.DISABLED, detail);
    }

    public static PoliticalContext queryFailed(String detail) {
        return empty(PoliticalLookupStatus.QUERY_FAILED, detail);
    }

    public static PoliticalContext notPolitical() {
        return empty(PoliticalLookupStatus.NOT_POLITICAL, "no MCA Capitals political role");
    }

    private static PoliticalContext empty(PoliticalLookupStatus status, String detail) {
        return new PoliticalContext(status, PoliticalRole.NONE, Optional.empty(), OptionalInt.empty(),
                Optional.empty(), Optional.empty(), detail);
    }
}
