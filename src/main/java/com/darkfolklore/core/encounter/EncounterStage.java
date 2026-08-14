package com.darkfolklore.core.encounter;

public enum EncounterStage {
    DORMANT, ORIGIN, OMENS, INVESTIGATING, ELIGIBLE, MANIFESTED, ACTIVE, ESCAPED, RESOLVED, EXPIRED;
    public boolean terminal() { return this == ESCAPED || this == RESOLVED || this == EXPIRED; }
}
