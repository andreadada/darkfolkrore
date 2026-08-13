package com.darkfolklore.core.compat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tiny independent circuit breaker used by optional compatibility capabilities.
 * A failure in one capability must never disable unrelated safe reads.
 */
public final class CompatCapabilityCircuit {
    private final String name;
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final AtomicReference<String> detail = new AtomicReference<>("ready");

    public CompatCapabilityCircuit(String name) {
        this.name = name == null || name.isBlank() ? "capability" : name;
    }

    public boolean available() {
        return available.get();
    }

    public String detail() {
        return name + ": " + detail.get();
    }

    public void markReady(String message) {
        if (!available.get()) return;
        detail.set(message == null || message.isBlank() ? "ready" : message);
    }

    public void fail(Throwable failure) {
        String message = failure == null ? "unknown failure" : failure.getClass().getSimpleName();
        fail(message);
    }

    public void fail(String message) {
        available.set(false);
        detail.set(message == null || message.isBlank() ? "failed" : message);
    }
}
