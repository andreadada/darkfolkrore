package com.darkfolklore.core.reputation;

import java.util.EnumMap;
import java.util.Map;

public final class ReputationLedger {
    private final EnumMap<ReputationFaction, Integer> values = new EnumMap<>(ReputationFaction.class);

    public int get(ReputationFaction faction) {
        return values.getOrDefault(faction, 0);
    }

    public int add(ReputationFaction faction, int delta) {
        int value = Math.max(-100, Math.min(100, get(faction) + delta));
        values.put(faction, value);
        return value;
    }

    public Map<ReputationFaction, Integer> snapshot() {
        return Map.copyOf(values);
    }
}
