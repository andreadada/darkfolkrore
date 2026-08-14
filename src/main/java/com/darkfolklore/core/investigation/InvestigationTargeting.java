package com.darkfolklore.core.investigation;

import com.darkfolklore.core.contracts.ContractAssignment;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.society.story.PersistentStory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.UUID;

/** Shared factual target matching for tracking, prepared hunts and contract completion. */
public final class InvestigationTargeting {
    private static final Set<String> ENCOUNTER_STORY_TEMPLATES = Set.of(
            "darkfolklore:missing_traveller",
            "darkfolklore:livestock_panic",
            "darkfolklore:returned_dead",
            "darkfolklore:wild_hunt_omen"
    );

    private InvestigationTargeting() {}

    public static String canonicalConcept(LivingEntity entity) {
        String registry = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return FolkloreDataManager.INSTANCE.canonical().resolve(registry)
                .map(value -> value.concept()).orElseGet(() -> SecretFacts.canonicalConcept(entity));
    }

    public static boolean matches(ContractAssignment assignment, LivingEntity entity,
                                  InvestigationCaseLink link) {
        if (!canonicalConcept(entity).equals(assignment.contract().targetConcept())) return false;
        if (link == null || link.culpritId().isEmpty()) return true;
        return entity.getUUID().equals(link.culpritId().get()) || link.culpritFallbackAllowed();
    }

    /** Identity testimony follows the exact culprit until confirmed fallback is authorized. */
    public static boolean matchesTestimonySubject(UUID subject, InvestigationCaseLink link) {
        if (link == null || link.culpritId().isEmpty() || link.culpritFallbackAllowed()) return true;
        return link.culpritId().get().equals(subject);
    }

    /** Physical evidence stays bound to the exact factual incident until its explicit fallback policy allows otherwise. */
    public static boolean matchesEvidence(String concept, EvidenceRecord evidence,
                                          InvestigationCaseLink link, PersistentStory exactStory) {
        if (!evidence.concept().equals(concept)) return false;
        if (link == null) return true;
        if (link.storyId().isPresent()
                && (exactStory == null || !link.storyId().get().equals(exactStory.story().id()))) return false;

        if (link.storyId().isPresent() && isEncounterStory(exactStory)) {
            return matchesEncounterEvidence(exactStory, evidence, link);
        }

        if (link.culpritId().isPresent() && !link.culpritFallbackAllowed()
                && evidence.subject().filter(link.culpritId().get()::equals).isEmpty()) return false;
        if (link.storyId().isPresent()) return IncidentContinuity.matches(exactStory, evidence);
        return true;
    }

    private static boolean isEncounterStory(PersistentStory story) {
        return story != null && ENCOUNTER_STORY_TEMPLATES.contains(story.story().template());
    }

    /**
     * Encounter evidence has two factual phases: subjectless omens before manifestation and exact-subject evidence
     * afterwards. The legacy +/-20 tick actor matcher is intentionally not used because these stories build over
     * many minutes before the provider entity exists.
     */
    private static boolean matchesEncounterEvidence(PersistentStory story, EvidenceRecord evidence,
                                                     InvestigationCaseLink link) {
        if (!story.story().concept().equals(evidence.concept())) return false;
        if (evidence.createdAt() < story.story().createdAt() || evidence.createdAt() > story.story().expiresAt()) return false;
        if (!evidence.position().dimension().equals(story.location().dimension())) return false;

        if (evidence.subject().isEmpty()) {
            // Omen placement is bounded to +/-12 on both horizontal axes: sqrt(12^2 + 12^2) < 18.
            return evidence.position().distanceSquared(story.location().blockPos()) <= 324.0D;
        }
        if (link.culpritId().isEmpty()) return false;
        return evidence.subject().filter(link.culpritId().get()::equals).isPresent() || link.culpritFallbackAllowed();
    }

    public static PersistentStory exactLinkedStory(FolkloreSavedData data, InvestigationCaseLink link) {
        if (link == null || link.storyId().isEmpty()) return null;
        return data.story(link.storyId().get()).orElse(null);
    }

    /** Legacy concept/village matching is valid only for rows that never had an exact story identity. */
    public static boolean mayUseLegacyStoryFallback(InvestigationCaseLink link) {
        return link == null || link.storyId().isEmpty();
    }
}
