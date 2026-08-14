package com.darkfolklore.core.persistence;

import com.darkfolklore.core.investigation.IncidentFact;
import com.darkfolklore.core.investigation.InvestigationCaseLink;
import com.darkfolklore.core.knowledge.observation.CreatureSightingKey;
import com.darkfolklore.core.knowledge.observation.CreatureSightingRecord;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.knowledge.social.KnowledgeSource;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvestigationSavedDataTest {
    @Test
    void conceptSightingRoundTripsWithoutBecomingASocialSecret() {
        InvestigationSavedData original = new InvestigationSavedData();
        UUID observer = UUID.randomUUID();
        UUID entity = UUID.randomUUID();
        CreatureSightingKey key = new CreatureSightingKey(observer, "darkfolklore:wendigo");
        WorldPosition position = new WorldPosition("minecraft:overworld", 12, 70, -4);
        original.mergeSighting(key, new CreatureSightingRecord(SocialKnowledgeState.CONFIRMED, 0.9F,
                KnowledgeSource.DIRECT_WITNESS, 120, Optional.of(entity), Optional.of(position),
                EvidenceType.DIRECT_WITNESS));

        InvestigationSavedData restored = InvestigationSavedData.load(original.save(new CompoundTag(), null), null);
        CreatureSightingRecord value = restored.sighting(observer, "darkfolklore:wendigo").orElseThrow();
        assertEquals(SocialKnowledgeState.CONFIRMED, value.state());
        assertEquals(entity, value.entityId().orElseThrow());
        assertEquals(position, value.location().orElseThrow());
        assertEquals(1, restored.sightingsHeldBy(observer).size());
    }

    @Test
    void incidentAndContractContinuityRoundTripWithFallbackFlags() {
        InvestigationSavedData original = new InvestigationSavedData();
        UUID story = UUID.randomUUID();
        UUID contract = UUID.randomUUID();
        UUID culprit = UUID.randomUUID();
        IncidentFact fact = new IncidentFact(Optional.of(culprit), "eidolon_repraised:wraith", 400);
        original.putIncidentFact(story, fact);
        original.putCaseLink(contract, InvestigationCaseLink.fromStory(story, fact));
        assertTrue(original.allowCulpritFallback(contract));
        assertTrue(original.allowIssuerFallback(contract));

        InvestigationSavedData restored = InvestigationSavedData.load(original.save(new CompoundTag(), null), null);
        assertEquals(culprit, restored.incidentFact(story).orElseThrow().culpritId().orElseThrow());
        InvestigationCaseLink link = restored.caseLink(contract).orElseThrow();
        assertEquals(story, link.storyId().orElseThrow());
        assertEquals(culprit, link.culpritId().orElseThrow());
        assertEquals("eidolon_repraised:wraith", link.observedImplementation());
        assertTrue(link.culpritFallbackAllowed());
        assertTrue(link.issuerFallbackAllowed());
    }

    @Test
    void lateManifestationBindsEveryExistingCaseForExactStory() {
        InvestigationSavedData data = new InvestigationSavedData();
        UUID story = UUID.randomUUID();
        UUID contractA = UUID.randomUUID();
        UUID contractB = UUID.randomUUID();
        UUID otherStoryContract = UUID.randomUUID();
        UUID culprit = UUID.randomUUID();
        data.putCaseLink(contractA, InvestigationCaseLink.fromStory(story, null));
        data.putCaseLink(contractB, InvestigationCaseLink.fromStory(story, null).allowCulpritFallback());
        data.putCaseLink(otherStoryContract, InvestigationCaseLink.fromStory(UUID.randomUUID(), null));

        assertEquals(2, data.bindCulpritForStory(story, culprit, "cnc:wendigo", 500L));
        assertEquals(culprit, data.incidentFact(story).orElseThrow().culpritId().orElseThrow());
        assertEquals(culprit, data.caseLink(contractA).orElseThrow().culpritId().orElseThrow());
        assertEquals(culprit, data.caseLink(contractB).orElseThrow().culpritId().orElseThrow());
        assertFalse(data.caseLink(contractB).orElseThrow().culpritFallbackAllowed());
        assertTrue(data.caseLink(otherStoryContract).orElseThrow().culpritId().isEmpty());
    }

    @Test
    void weakOldSightingsCanBePrunedWithoutTouchingConfirmedRecentOnes() {
        InvestigationSavedData data = new InvestigationSavedData();
        UUID observer = UUID.randomUUID();
        data.mergeSighting(new CreatureSightingKey(observer, "darkfolklore:wendigo"),
                new CreatureSightingRecord(SocialKnowledgeState.RUMOR, 0.05F, KnowledgeSource.RUMOR,
                        0, Optional.empty(), Optional.empty(), EvidenceType.TESTIMONY));
        data.mergeSighting(new CreatureSightingKey(observer, "darkfolklore:sprite"),
                new CreatureSightingRecord(SocialKnowledgeState.CONFIRMED, 0.9F, KnowledgeSource.DIRECT_WITNESS,
                        900, Optional.empty(), Optional.empty(), EvidenceType.DIRECT_WITNESS));

        assertEquals(1, data.pruneSightings(1000, 0.08F, 100));
        assertTrue(data.sighting(observer, "darkfolklore:wendigo").isEmpty());
        assertTrue(data.sighting(observer, "darkfolklore:sprite").isPresent());
    }

    @Test
    void continuityCapRejectsNewAdmissionInsteadOfEvictingAnExistingExactCase() {
        InvestigationSavedData data = new InvestigationSavedData();
        UUID first = new UUID(1L, 0L);
        InvestigationCaseLink link = InvestigationCaseLink.fromStory(UUID.randomUUID(), null);
        for (int i = 0; i < 20_000; i++) {
            assertTrue(data.putCaseLink(new UUID(1L, i), link));
        }

        assertFalse(data.putCaseLink(new UUID(2L, 0L), link));
        assertTrue(data.caseLink(first).isPresent(), "active exact continuity must never be silently evicted");
    }
}
