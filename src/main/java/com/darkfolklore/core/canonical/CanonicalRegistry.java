package com.darkfolklore.core.canonical;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CanonicalRegistry {
    private volatile Map<String, CanonicalDefinition> byConcept = Map.of();
    private volatile Map<String, CanonicalDefinition> byImplementation = Map.of();

    public void replace(Collection<CanonicalDefinition> definitions) {
        Map<String, CanonicalDefinition> concepts = new LinkedHashMap<>();
        Map<String, CanonicalDefinition> implementations = new LinkedHashMap<>();
        for (CanonicalDefinition definition : definitions) {
            if (concepts.putIfAbsent(definition.concept(), definition) != null) {
                throw new IllegalArgumentException("Duplicate canonical concept " + definition.concept());
            }
            if (!definition.canonicalId().isBlank()) {
                putUnique(implementations, definition.canonicalId(), definition);
            }
            for (String id : definition.implementations()) putUnique(implementations, id, definition);
        }
        byConcept = Map.copyOf(concepts);
        byImplementation = Map.copyOf(implementations);
    }

    private static void putUnique(Map<String, CanonicalDefinition> map, String id, CanonicalDefinition definition) {
        CanonicalDefinition previous = map.putIfAbsent(id, definition);
        if (previous != null && previous != definition) {
            throw new IllegalArgumentException("Registry ID " + id + " belongs to both "
                    + previous.concept() + " and " + definition.concept());
        }
    }

    public Optional<CanonicalDefinition> concept(String concept) {
        return Optional.ofNullable(byConcept.get(concept));
    }

    public Optional<CanonicalDefinition> resolve(String implementationId) {
        return Optional.ofNullable(byImplementation.get(implementationId));
    }

    public List<CanonicalDefinition> definitions() {
        return List.copyOf(byConcept.values());
    }
}
