package com.darkfolklore.core.spawn;

public enum SpawnRarity {
    COMMON(1.0F),
    UNCOMMON(0.65F),
    RARE(0.30F),
    VERY_RARE(0.12F),
    LEGENDARY(0.04F);

    private final float naturalChance;

    SpawnRarity(float naturalChance) {
        this.naturalChance = naturalChance;
    }

    public float naturalChance() {
        return naturalChance;
    }
}
