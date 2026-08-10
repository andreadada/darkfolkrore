package com.darkfolklore.core.data;

import java.util.List;
import java.util.Objects;

/** Tiny atomic swap gate: any validation error preserves the complete previous snapshot. */
public final class ValidatedReloadState<T> {
    private volatile T current;

    public ValidatedReloadState(T initial) {
        current = Objects.requireNonNull(initial, "initial");
    }

    public T get() { return current; }

    public boolean apply(T candidate, List<String> validationErrors) {
        Objects.requireNonNull(validationErrors, "validationErrors");
        if (candidate == null || !validationErrors.isEmpty()) return false;
        current = candidate;
        return true;
    }
}
