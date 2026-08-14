package com.darkfolklore.core.knowledge.lore;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stable presentation policy for progressive monster knowledge. Field Guide remains the provider UI; this dossier
 * decides what Dark Folklore itself is allowed to reveal at each lore stage.
 */
public record KnowledgeDossier(String concept, LoreProgress progress, Set<KnowledgeFacet> revealed,
                               Set<KnowledgeFacet> hidden) {
    public KnowledgeDossier {
        revealed = Set.copyOf(revealed);
        hidden = Set.copyOf(hidden);
    }

    public static KnowledgeDossier from(String concept, LoreProgress progress) {
        EnumSet<KnowledgeFacet> revealed = EnumSet.noneOf(KnowledgeFacet.class);
        switch (progress.stage()) {
            case UNKNOWN -> { }
            case DISCOVERED -> revealed.add(KnowledgeFacet.EXISTENCE);
            case OBSERVED -> {
                revealed.add(KnowledgeFacet.EXISTENCE);
                revealed.add(KnowledgeFacet.SIGNS);
                revealed.add(KnowledgeFacet.BEHAVIOR);
            }
            case STUDIED -> {
                revealed.add(KnowledgeFacet.EXISTENCE);
                revealed.add(KnowledgeFacet.SIGNS);
                revealed.add(KnowledgeFacet.BEHAVIOR);
                revealed.add(KnowledgeFacet.IDENTITY);
                revealed.add(KnowledgeFacet.FEEDING_HABITS);
                revealed.add(KnowledgeFacet.WEAKNESSES);
                revealed.add(KnowledgeFacet.COUNTERMEASURES);
                revealed.add(KnowledgeFacet.CURE);
            }
            case MASTERED -> revealed.addAll(EnumSet.allOf(KnowledgeFacet.class));
        }
        EnumSet<KnowledgeFacet> hidden = EnumSet.allOf(KnowledgeFacet.class);
        hidden.removeAll(revealed);
        return new KnowledgeDossier(concept, progress, revealed, hidden);
    }

    public boolean reveals(KnowledgeFacet facet) { return revealed.contains(facet); }
}
