package com.darkfolklore.core.society.story;

import com.darkfolklore.core.knowledge.social.SecretType;

import java.util.Objects;
import java.util.Optional;

/** Validated data-pack definition for a bounded society story trigger. */
public record StoryTemplateDefinition(String id, StoryTrigger trigger, String concept, int weight,
                                      long cooldownTicks, long lifetimeTicks,
                                      Optional<SecretType> requiredSecret, boolean capitalOnly,
                                      boolean contractEligible, boolean enabled) {
    public StoryTemplateDefinition {
        id = Objects.requireNonNull(id, "id").trim();
        trigger = Objects.requireNonNull(trigger, "trigger");
        concept = Objects.requireNonNullElse(concept, "").trim();
        requiredSecret = requiredSecret == null ? Optional.empty() : requiredSecret;
        if (id.isEmpty()) throw new IllegalArgumentException("story template id is required");
        if (weight < 1 || weight > 1000) throw new IllegalArgumentException("story weight must be 1..1000");
        if (cooldownTicks < 0) throw new IllegalArgumentException("story cooldown cannot be negative");
        if (lifetimeTicks < 200) throw new IllegalArgumentException("story lifetime must be at least 200 ticks");
    }

    public String resolvedConcept(String eventConcept) {
        return concept.isBlank() || concept.equals("*") ? eventConcept : concept;
    }
}
