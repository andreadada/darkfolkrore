package com.darkfolklore.core.society.rumor;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.mca.*;
import com.darkfolklore.core.compat.mcacapitals.PoliticalContext;
import com.darkfolklore.core.compat.mcacapitals.PoliticalWeightModel;
import com.darkfolklore.core.compat.mcacapitals.PoliticalWeights;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeKey;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One bounded, explainable trust query for the two actors participating in a rumor delivery. */
public final class SocialTrustResolver {
    private SocialTrustResolver() {}

    public static SocialTrustAssessment evaluate(FolkloreSavedData data, ServerLevel level,
                                                 LivingEntity observer, LivingEntity source,
                                                 UUID subject, SecretType secret) {
        float trust = 0.5F;
        float transmission = 1.0F;
        List<TrustContribution> reasons = new ArrayList<>();
        reasons.add(new TrustContribution("baseline unfamiliar-source trust", 0.5F));

        Set<UUID> shared = new HashSet<>(data.organizationsForMember(observer.getUUID()));
        shared.retainAll(data.organizationsForMember(source.getUUID()));
        if (!shared.isEmpty()) {
            trust += 0.20F;
            reasons.add(new TrustContribution("same Dark Folklore organization", 0.20F));
        }

        SocialKnowledgeState prior = data.social(new SocialKnowledgeKey(observer.getUUID(), subject, secret))
                .map(value -> value.state()).orElse(SocialKnowledgeState.UNKNOWN);
        if (prior == SocialKnowledgeState.SUSPECTED) {
            trust += 0.10F;
            reasons.add(new TrustContribution("observer already suspects the claim", 0.10F));
        } else if (prior.strength() >= SocialKnowledgeState.CONFIRMED.strength()) {
            trust += 0.20F;
            reasons.add(new TrustContribution("observer already has credible knowledge", 0.20F));
        }

        CompatibilityManager compatibility = CompatibilityManager.INSTANCE;
        if (FolkloreConfig.RELATIONSHIP_TRUST.get()) {
            McaSocialContext context = compatibility.mcaSocial().relationship(observer, source);
            McaTrustResult relationship = McaTrustModel.evaluate(context,
                    FolkloreDataManager.INSTANCE.socialTrustSettings());
            trust += (float) relationship.modifier();
            relationship.contributions().forEach(value -> reasons.add(
                    new TrustContribution(value.reason(), (float) value.amount())));
            if (FolkloreConfig.PERSONALITY_MODIFIERS.get()) {
                McaPersonalityInfluence personality = context.sourcePersonality()
                        .map(McaPersonalityInfluence::fromVerifiedName)
                        .orElseGet(() -> new McaPersonalityInfluence(false, 0, 0, 0));
                if (personality.recognized() && personality.rumorTransmissionModifier() != 0) {
                    transmission += (float) personality.rumorTransmissionModifier();
                    reasons.add(new TrustContribution("verified MCA source personality transmission",
                            (float) personality.rumorTransmissionModifier()));
                }
            }
        }

        if (FolkloreConfig.MCA_CAPITALS.get()) {
            PoliticalContext context = compatibility.mcaCapitals().politicalContext(level, source.getUUID());
            PoliticalWeights weights = FolkloreDataManager.INSTANCE.politicalWeights(context.role());
            float configuredScale = Math.max(0.0F, FolkloreConfig.POLITICAL_RUMOR_WEIGHT.get().floatValue() - 1.0F);
            float political = (float) weights.credibility() * configuredScale;
            if (political > 0.0F) {
                trust += political;
                reasons.add(new TrustContribution("verified MCA Capitals source role " + context.role(), political));
            }
        }
        return new SocialTrustAssessment(trust, transmission, reasons);
    }
}
