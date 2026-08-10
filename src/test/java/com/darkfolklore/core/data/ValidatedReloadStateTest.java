package com.darkfolklore.core.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedReloadStateTest {
    @Test
    void invalidOrMissingCandidateRetainsWholePreviousSnapshot() {
        ValidatedReloadState<String> state = new ValidatedReloadState<>("known-good");
        assertFalse(state.apply("partial-invalid", List.of("duplicate canonical id")));
        assertEquals("known-good", state.get());
        assertFalse(state.apply(null, List.of("parse failure")));
        assertEquals("known-good", state.get());
    }

    @Test
    void validatedCandidateSwapsAsOneReference() {
        ValidatedReloadState<String> state = new ValidatedReloadState<>("old");
        assertTrue(state.apply("new", List.of()));
        assertEquals("new", state.get());
    }
}
