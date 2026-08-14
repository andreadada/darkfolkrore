package com.darkfolklore.core.living.casebook;

public enum CaseStage {
    OPEN,
    INVESTIGATING,
    IDENTIFIED,
    PREPARED,
    HUNTED,
    RESOLVED,
    DISMISSED,
    EXPIRED;

    public boolean terminal() {
        return this == RESOLVED || this == DISMISSED || this == EXPIRED;
    }

    public boolean mayAdvanceTo(CaseStage next) {
        if (next == null || terminal() || next == this) return false;
        if (next == DISMISSED || next == EXPIRED) return true;
        return next.ordinal() > ordinal() && !next.terminal() || next == RESOLVED;
    }
}
