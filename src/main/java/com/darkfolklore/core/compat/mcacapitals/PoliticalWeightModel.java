package com.darkfolklore.core.compat.mcacapitals;

/** Pure default weight table; callers may scale or replace these values through Core configuration. */
public final class PoliticalWeightModel {
    private PoliticalWeightModel() {}

    public static PoliticalWeights weights(PoliticalRole role) {
        return switch (role) {
            case HIGH_SOVEREIGN -> new PoliticalWeights(0.30, 0.35, 0.35, 0.30);
            case SOVEREIGN -> new PoliticalWeights(0.28, 0.32, 0.32, 0.28);
            case CONSORT, DOWAGER -> new PoliticalWeights(0.22, 0.24, 0.24, 0.22);
            case HEIR -> new PoliticalWeights(0.20, 0.22, 0.24, 0.20);
            case ROYAL_CHILD, PRINCE_CONSORT, DOWAGER_PRINCE ->
                    new PoliticalWeights(0.16, 0.18, 0.18, 0.16);
            case HAND -> new PoliticalWeights(0.22, 0.30, 0.30, 0.20);
            case GRAND_MAESTER -> new PoliticalWeights(0.20, 0.22, 0.28, 0.18);
            case HERALD -> new PoliticalWeights(0.16, 0.18, 0.12, 0.25);
            case DUKE, DOWAGER_DUKE -> new PoliticalWeights(0.16, 0.20, 0.18, 0.15);
            case MAESTER -> new PoliticalWeights(0.14, 0.16, 0.22, 0.12);
            case COMMANDER -> new PoliticalWeights(0.18, 0.28, 0.30, 0.16);
            case ROYAL_GUARD -> new PoliticalWeights(0.12, 0.22, 0.22, 0.10);
            case LORD -> new PoliticalWeights(0.10, 0.14, 0.12, 0.10);
            case KNIGHT -> new PoliticalWeights(0.06, 0.10, 0.10, 0.05);
            case COMMONER, NONE, UNKNOWN -> PoliticalWeights.NONE;
        };
    }
}
