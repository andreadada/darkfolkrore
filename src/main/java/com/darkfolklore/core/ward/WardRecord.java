package com.darkfolklore.core.ward;

import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Objects;
import java.util.UUID;

public record WardRecord(
        UUID id,
        WardType type,
        WorldPosition anchor,
        int radius,
        int strength,
        UUID creator,
        long createdAt,
        long expiresAt
) {
    public WardRecord {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        Objects.requireNonNull(anchor);
        Objects.requireNonNull(creator);
        radius = Math.max(3, Math.min(32, radius));
        strength = Math.max(1, Math.min(100, strength));
        createdAt = Math.max(0L, createdAt);
        expiresAt = Math.max(createdAt + 1L, expiresAt);
    }

    public boolean active(long now) { return now < expiresAt; }
    public boolean appliesTo(WardType requested) { return type == WardType.GENERAL || type == requested; }
}
