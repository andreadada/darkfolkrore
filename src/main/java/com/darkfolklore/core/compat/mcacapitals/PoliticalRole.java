package com.darkfolklore.core.compat.mcacapitals;

/** Semantic roles mapped only from titles emitted by MCA Capitals 1.1.0. */
public enum PoliticalRole {
    HIGH_SOVEREIGN,
    SOVEREIGN,
    CONSORT,
    DOWAGER,
    HEIR,
    ROYAL_CHILD,
    PRINCE_CONSORT,
    DOWAGER_PRINCE,
    HAND,
    GRAND_MAESTER,
    HERALD,
    DUKE,
    DOWAGER_DUKE,
    MAESTER,
    COMMANDER,
    ROYAL_GUARD,
    LORD,
    KNIGHT,
    COMMONER,
    NONE,
    UNKNOWN;

    /**
     * Maps the exact, non-localized strings returned by CapitalTitleResolver in the audited 1.1.0 JAR.
     * Sir and Dame need the record's independent royal-guard bit because those titles are also used for knights.
     */
    public static PoliticalRole fromExactTitle(String title, boolean royalGuard) {
        if (title == null) return UNKNOWN;
        return switch (title) {
            case "High Queen", "High King" -> HIGH_SOVEREIGN;
            case "Queen", "King" -> SOVEREIGN;
            case "Queen Consort", "King Consort" -> CONSORT;
            case "Dowager Queen", "Dowager King" -> DOWAGER;
            case "Heir Apparent", "Crown Princess", "Crown Prince" -> HEIR;
            case "Princess", "Prince" -> ROYAL_CHILD;
            case "Princess Consort", "Prince Consort" -> PRINCE_CONSORT;
            case "Dowager Princess", "Dowager Prince" -> DOWAGER_PRINCE;
            case "Hand of the Queen", "Hand of the King" -> HAND;
            case "Grand Maester" -> GRAND_MAESTER;
            case "Court Herald" -> HERALD;
            case "Duchess", "Duke" -> DUKE;
            case "Dowager Duchess", "Dowager Duke" -> DOWAGER_DUKE;
            case "Maester" -> MAESTER;
            case "Lord Commander" -> COMMANDER;
            case "Dame", "Sir" -> royalGuard ? ROYAL_GUARD : KNIGHT;
            case "Lady", "Lord" -> LORD;
            case "Commoner" -> COMMONER;
            case "None" -> NONE;
            default -> UNKNOWN;
        };
    }
}
