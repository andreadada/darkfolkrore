package com.darkfolklore.core.investigation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.persistence.WorldPosition;
import com.darkfolklore.core.society.story.PersistentStory;
import com.darkfolklore.core.society.story.StoryInstance;
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

    @Test
    void testimonyRemainsBoundToExactCulpritUntilConfirmedFallback() {
        UUID culprit = UUID.randomUUID();
        UUID otherVampire = UUID.randomUUID();
        InvestigationCaseLink link = InvestigationCaseLink.fromStory(UUID.randomUUID(),
                new IncidentFact(Optional.of(culprit), "mca:villager", 10L));

        assertTrue(InvestigationTargeting.matchesTestimonySubject(culprit, link));
        assertFalse(InvestigationTargeting.matchesTestimonySubject(otherVampire, link));
        assertTrue(InvestigationTargeting.matchesTestimonySubject(otherVampire, link.allowCulpritFallback()));
    }

    @Test
    void physicalEvidenceCannotCrossLinkAnotherSameConceptCulpritOrMissingExactStory() {
        UUID storyId = UUID.randomUUID();
        UUID culprit = UUID.randomUUID();
        InvestigationCaseLink link = InvestigationCaseLink.fromStory(storyId,
                new IncidentFact(Optional.of(culprit), "mca:villager", 100L));
        StoryInstance story = new StoryInstance(storyId, "feeding_assault",
                "darkfolklore:vampire", 100L, 1000L);
        story.addActor(culprit);
        PersistentStory persistent = new PersistentStory(story,
                new WorldPosition("minecraft:overworld", 0, 64, 0), "village");
        EvidenceRecord exact = evidence(culprit);
        EvidenceRecord unrelated = evidence(UUID.randomUUID());

        assertTrue(InvestigationTargeting.matchesEvidence("darkfolklore:vampire", exact, link, persistent));
        assertFalse(InvestigationTargeting.matchesEvidence("darkfolklore:vampire", unrelated, link, persistent));
        assertFalse(InvestigationTargeting.matchesEvidence("darkfolklore:vampire", exact, link, null));
        assertFalse(InvestigationTargeting.mayUseLegacyStoryFallback(link));
        assertTrue(InvestigationTargeting.mayUseLegacyStoryFallback(null));
    }

    private static EvidenceRecord evidence(UUID subject) {
        return new EvidenceRecord(UUID.randomUUID(), EvidenceType.BLOOD, "darkfolklore:vampire",
                Optional.of(subject), new WorldPosition("minecraft:overworld", 1, 64, 1),
                100L, 1000L, Optional.empty());
    }
}
