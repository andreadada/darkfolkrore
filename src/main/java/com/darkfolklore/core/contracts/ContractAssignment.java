package com.darkfolklore.core.contracts;

import com.darkfolklore.core.persistence.WorldPosition;

import java.util.Objects;
import java.util.UUID;

public record ContractAssignment(
        UUID player,
        MonsterContract contract,
        WorldPosition investigationCenter,
        String villageKey,
        int requiredDistinctClues
) {
    public ContractAssignment {
        Objects.requireNonNull(player);
        Objects.requireNonNull(contract);
        Objects.requireNonNull(investigationCenter);
        villageKey = Objects.requireNonNullElse(villageKey, "");
        requiredDistinctClues = Math.max(1, Math.min(8, requiredDistinctClues));
    }
}
