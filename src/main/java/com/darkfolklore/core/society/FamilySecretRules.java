package com.darkfolklore.core.society;

import com.darkfolklore.core.compat.mca.McaRelationshipCategory;

import java.util.Optional;

/** Pure, deliberately small reaction table based only on relationships MCA actually exposes. */
public final class FamilySecretRules {
    private FamilySecretRules() {}

    public static Optional<FamilySecretReaction> choose(McaRelationshipCategory relationship,
                                                        String verifiedObserverPersonality,
                                                        boolean observerIsFactualHunter) {
        boolean family = switch (relationship) {
            case SPOUSE, SOURCE_IS_PARENT, SOURCE_IS_CHILD, SIBLING -> true;
            default -> false;
        };
        if (!family) return Optional.empty();
        if (observerIsFactualHunter) return Optional.of(FamilySecretReaction.REPORT_TO_HUNTERS);
        if ("ANXIOUS".equals(verifiedObserverPersonality)) {
            return Optional.of(FamilySecretReaction.FEARFUL_WITHDRAWAL);
        }
        if (relationship == McaRelationshipCategory.SIBLING) {
            return Optional.of(FamilySecretReaction.CONFRONT_RELATIVE);
        }
        return Optional.of(FamilySecretReaction.PROTECT_SECRET);
    }

    public static boolean suppressesRetelling(FamilySecretReaction reaction) {
        return reaction == FamilySecretReaction.PROTECT_SECRET
                || reaction == FamilySecretReaction.CONFRONT_RELATIVE;
    }

    public static boolean suppressesOrganizationReport(FamilySecretReaction reaction) {
        return reaction == FamilySecretReaction.PROTECT_SECRET;
    }
}
