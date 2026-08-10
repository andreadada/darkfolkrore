package com.darkfolklore.core.contracts;

public enum ContractStatus {
    OFFERED,
    INVESTIGATING,
    IDENTIFIED,
    HUNTED,
    COMPLETE,
    EXPIRED,
    CANCELLED;

    public boolean terminal() {
        return this == COMPLETE || this == EXPIRED || this == CANCELLED;
    }
}
