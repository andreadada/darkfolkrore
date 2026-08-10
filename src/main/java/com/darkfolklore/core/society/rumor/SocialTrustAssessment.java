package com.darkfolklore.core.society.rumor;

import java.util.List;

public record SocialTrustAssessment(float trust, float transmissionMultiplier,
                                    List<TrustContribution> contributions) {
    public SocialTrustAssessment {
        trust = Math.max(0.0F, Math.min(1.0F, trust));
        transmissionMultiplier = Math.max(0.1F, Math.min(2.0F, transmissionMultiplier));
        contributions = List.copyOf(contributions);
    }
}
