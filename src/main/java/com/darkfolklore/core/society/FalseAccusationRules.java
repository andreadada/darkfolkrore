package com.darkfolklore.core.society;

import com.darkfolklore.core.knowledge.social.SocialKnowledgeRecord;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;

/** Keeps false accusations rare, story-driven, and separate from factual supernatural state. */
public final class FalseAccusationRules {
    private FalseAccusationRules() {}

    public static boolean eligible(boolean featureEnabled, boolean subjectActuallyHasSecret,
                                   boolean hostileRelationship, boolean storyAuthorized,
                                   SocialKnowledgeRecord belief) {
        if (!featureEnabled || subjectActuallyHasSecret || belief == null) return false;
        if (!hostileRelationship && !storyAuthorized) return false;
        return belief.state().strength() <= SocialKnowledgeState.SUSPECTED.strength()
                && belief.confidence() >= 0.15F && belief.confidence() <= 0.55F;
    }
}
