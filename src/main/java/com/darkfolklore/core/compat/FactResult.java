package com.darkfolklore.core.compat;

public enum FactResult {
    TRUE,
    FALSE,
    UNKNOWN,
    NOT_APPLICABLE;

    public boolean isTrue() {
        return this == TRUE;
    }

    public static FactResult of(boolean value) {
        return value ? TRUE : FALSE;
    }
}
