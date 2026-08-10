package com.darkfolklore.core.spawn;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SpawnProfileRegistry {
    private volatile Map<String, SpawnProfile> profiles = Map.of();

    public void replace(Collection<SpawnProfile> profiles) {
        Map<String, SpawnProfile> next = new LinkedHashMap<>();
        for (SpawnProfile profile : profiles) {
            if (next.putIfAbsent(profile.entityId(), profile) != null) {
                throw new IllegalArgumentException("Duplicate spawn profile " + profile.entityId());
            }
        }
        this.profiles = Map.copyOf(next);
    }

    public Optional<SpawnProfile> get(String entityId) {
        return Optional.ofNullable(profiles.get(entityId));
    }

    public Map<String, SpawnProfile> profiles() {
        return profiles;
    }
}
