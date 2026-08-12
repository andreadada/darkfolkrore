package com.darkfolklore.core.lifecycle;

import java.util.Optional;
import java.util.UUID;

/** Pure provenance sanitizer: provider conversion UUIDs are sources, never inferred biological parents. */
final class ProviderLineagePolicy {
    private ProviderLineagePolicy() {}

    static Optional<UUID> validSource(UUID descendant, Optional<UUID> providerSource) {
        if (descendant == null || providerSource == null) return Optional.empty();
        return providerSource.filter(source -> !source.equals(descendant));
    }
}
