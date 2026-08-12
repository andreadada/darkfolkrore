package com.darkfolklore.core.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderLineagePolicyTest {
    @Test
    void validProviderConversionSourceIsPreservedExactly() {
        UUID descendant = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        assertEquals(Optional.of(source), ProviderLineagePolicy.validSource(descendant, Optional.of(source)));
    }

    @Test
    void absentAndMalformedSelfSourceDoNotBecomeLineage() {
        UUID descendant = UUID.randomUUID();
        assertEquals(Optional.empty(), ProviderLineagePolicy.validSource(descendant, Optional.empty()));
        assertEquals(Optional.empty(), ProviderLineagePolicy.validSource(descendant, Optional.of(descendant)));
    }
}
