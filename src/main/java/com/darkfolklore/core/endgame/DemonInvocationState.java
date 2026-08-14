package com.darkfolklore.core.endgame;

public enum DemonInvocationState {
    ACTIVE(false),
    COMPLETED(true),
    FAILED(true);

    private final boolean terminal;

    DemonInvocationState(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean terminal() {
        return terminal;
    }
}
