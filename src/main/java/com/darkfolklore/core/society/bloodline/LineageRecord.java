package com.darkfolklore.core.society.bloodline;

import com.darkfolklore.core.knowledge.social.SecretType;

import java.util.Objects;
import java.util.UUID;

public record LineageRecord(UUID descendant, UUID source, SecretType type, long recordedAt) {
    public LineageRecord {
        Objects.requireNonNull(descendant);
        Objects.requireNonNull(source);
        Objects.requireNonNull(type);
        if (descendant.equals(source)) throw new IllegalArgumentException("Self-lineage is invalid");
    }
}
