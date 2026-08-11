package com.darkfolklore.core.investigation;

public record Hypothesis(
        String concept,
        int score,
        int matchedEvidence,
        int observedEvidence,
        float confidence
) {}
