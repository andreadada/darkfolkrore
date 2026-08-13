package com.darkfolklore.core.investigation;

import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.social.EvidenceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Pure ranking logic: hypotheses are derived from evidence, never from the hidden contract target. */
public final class HypothesisEngine {
    private HypothesisEngine() {}

    public static List<Hypothesis> rank(Set<EvidenceType> observed) {
        return rank(observed, FolkloreDataManager.INSTANCE.investigationProfiles());
    }

    public static List<Hypothesis> rank(Set<EvidenceType> observed, Collection<InvestigationProfile> profiles) {
        if (observed == null || observed.isEmpty() || profiles == null || profiles.isEmpty()) return List.of();
        List<Hypothesis> result = new ArrayList<>();
        for (InvestigationProfile profile : profiles) {
            int score = 0;
            int matched = 0;
            int totalWeight = 0;
            int matchedWeight = 0;
            for (EvidenceType type : observed) {
                int weight = weight(type);
                totalWeight += weight;
                if (profile.signatures().contains(type)) {
                    matched++;
                    matchedWeight += weight;
                    score += weight * 3;
                } else {
                    score -= weight;
                }
            }
            if (matched == 0) continue;
            float confidence = totalWeight == 0 ? 0.0F : matchedWeight / (float) totalWeight;
            result.add(new Hypothesis(profile.concept(), score, matched, observed.size(), confidence));
        }
        result.sort(Comparator.comparingInt(Hypothesis::score).reversed()
                .thenComparing(Comparator.comparingDouble(Hypothesis::confidence).reversed())
                .thenComparing(Hypothesis::concept));
        return List.copyOf(result);
    }

    private static int weight(EvidenceType type) {
        return switch (type) {
            case HERBAL_REACTION, GARLIC_REACTION, WOLFSBANE_REACTION, SPIRIT_ECHO, SOUL_ECHO, OCCULT_SIGNATURE,
                    GLAMOUR_TRACE, CURSE_TRACE, BINDING_TRACE -> 3;
            case BITE_MARK, FOOTPRINT, BONE, MAGICAL_RESIDUE, BLOOD_RESONANCE, SCORCH, SCENT, HAIR, TRACK -> 2;
            case DIRECT_WITNESS, BODY, BLOOD, TESTIMONY -> 1;
        };
    }
}
