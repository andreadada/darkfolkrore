package com.darkfolklore.core.investigation;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvestigationCaseLinkTest {
    @Test
    void linkKeepsStoryCulpritAndObservedImplementationSeparateFromFallbackPolicy() {
        UUID story = UUID.randomUUID();
        UUID culprit = UUID.randomUUID();
        IncidentFact fact = new IncidentFact(Optional.of(culprit), "eidolon_repraised:wraith", 120L);

        InvestigationCaseLink link = InvestigationCaseLink.fromStory(story, fact);
        assertEquals(Optional.of(story), link.storyId());
        assertEquals(Optional.of(culprit), link.culpritId());
        assertEquals("eidolon_repraised:wraith", link.observedImplementation());
        assertFalse(link.culpritFallbackAllowed());
        assertFalse(link.issuerFallbackAllowed());

        InvestigationCaseLink culpritFallback = link.allowCulpritFallback();
        assertTrue(culpritFallback.culpritFallbackAllowed());
        assertFalse(culpritFallback.issuerFallbackAllowed());
        assertEquals(link.storyId(), culpritFallback.storyId());
        assertEquals(link.culpritId(), culpritFallback.culpritId());

        InvestigationCaseLink bothFallbacks = culpritFallback.allowIssuerFallback();
        assertTrue(bothFallbacks.culpritFallbackAllowed());
        assertTrue(bothFallbacks.issuerFallbackAllowed());
        assertEquals("eidolon_repraised:wraith", bothFallbacks.observedImplementation());
    }

    @Test
    void missingLegacyFactProducesSafeEmptyContinuityMetadata() {
        UUID story = UUID.randomUUID();
        InvestigationCaseLink link = InvestigationCaseLink.fromStory(story, null);
        assertEquals(Optional.of(story), link.storyId());
        assertTrue(link.culpritId().isEmpty());
        assertTrue(link.observedImplementation().isBlank());
        assertFalse(link.culpritFallbackAllowed());
        assertFalse(link.issuerFallbackAllowed());
    }
}
