package com.darkfolklore.core.society.village;

/** Pure policy converting persisted society pressure into a comprehensible world response. */
public final class VillageResponseRules {
    private VillageResponseRules() {}

    public static Snapshot assess(VillageSocietyState state) {
        int hunterReadiness = clamp((int) Math.round(state.hunterInfluence() * 0.45D
                + state.publicAwareness() * 0.30D + state.suspicion() * 0.15D + state.politicalImportance() * 0.10D));
        int supernaturalPressure = Math.max(state.vampireInfluence(), Math.max(state.werewolfInfluence(), state.witchInfluence()));
        int crisis = clamp((int) Math.round(state.fear() * 0.35D + state.suspicion() * 0.30D
                + supernaturalPressure * 0.35D));

        VillageResponseTier tier;
        if (state.vampireInfluence() >= 70 && state.vampireInfluence() >= state.hunterInfluence() + 15) {
            tier = VillageResponseTier.COMPROMISED;
        } else if (state.hunterInfluence() >= 80 || hunterReadiness >= 75 || crisis >= 85) {
            tier = VillageResponseTier.LOCKDOWN;
        } else if (state.hunterInfluence() >= 55 || hunterReadiness >= 55 || crisis >= 65) {
            tier = VillageResponseTier.MOBILIZED;
        } else if (state.hunterInfluence() >= 35 || hunterReadiness >= 35 || crisis >= 45) {
            tier = VillageResponseTier.ALERT;
        } else if (state.hunterInfluence() >= 15 || hunterReadiness >= 20 || crisis >= 20) {
            tier = VillageResponseTier.UNEASY;
        } else {
            tier = VillageResponseTier.CALM;
        }
        return new Snapshot(tier, hunterReadiness, supernaturalPressure, crisis, message(tier));
    }

    private static String message(VillageResponseTier tier) {
        return switch (tier) {
            case CALM -> "The settlement appears unaware of any supernatural threat.";
            case UNEASY -> "Residents are uneasy; rumors and small precautions are spreading.";
            case ALERT -> "The settlement is alert; witnesses are talking and hunters are paying attention.";
            case MOBILIZED -> "The settlement is mobilized; hunter influence and defensive behavior are visible.";
            case LOCKDOWN -> "The settlement is in supernatural lockdown; public fear and countermeasures are high.";
            case COMPROMISED -> "The settlement appears compromised; vampire influence is overwhelming local resistance.";
        };
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }

    public record Snapshot(VillageResponseTier tier, int hunterReadiness, int supernaturalPressure,
                           int crisis, String message) {}
}
