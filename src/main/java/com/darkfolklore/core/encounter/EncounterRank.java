package com.darkfolklore.core.encounter;

public enum EncounterRank {
    SUPERNATURAL(1), RARE(2), DREAD(3), LEGENDARY(4), MYTHIC(5);
    private final int weight;
    EncounterRank(int weight) { this.weight = weight; }
    public int weight() { return weight; }
}
