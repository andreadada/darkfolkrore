package com.darkfolklore.core.society.village;

import com.darkfolklore.core.society.organization.OrganizationType;

public final class VillageSocietyState {
    private int publicAwareness;
    private int vampireInfluence;
    private int hunterInfluence;
    private int werewolfInfluence;
    private int witchInfluence;
    private int fear;
    private int suspicion;
    private int politicalImportance;

    public int publicAwareness() { return publicAwareness; }
    public int vampireInfluence() { return vampireInfluence; }
    public int hunterInfluence() { return hunterInfluence; }
    public int werewolfInfluence() { return werewolfInfluence; }
    public int witchInfluence() { return witchInfluence; }
    public int fear() { return fear; }
    public int suspicion() { return suspicion; }
    public int politicalImportance() { return politicalImportance; }

    public void recordIncident(int witnesses, boolean confirmed, int severity) {
        int boundedSeverity = Math.max(1, Math.min(10, severity));
        suspicion = clamp(suspicion + boundedSeverity + witnesses);
        fear = clamp(fear + boundedSeverity * 2);
        if (confirmed) publicAwareness = clamp(publicAwareness + Math.max(1, witnesses * 2));
    }

    public void adjustPublicAwareness(int delta) { publicAwareness = clamp(publicAwareness + delta); }
    public void adjustFear(int delta) { fear = clamp(fear + delta); }
    public void adjustSuspicion(int delta) { suspicion = clamp(suspicion + delta); }
    public void adjustPoliticalImportance(int delta) { politicalImportance = clamp(politicalImportance + delta); }

    public void adjustInfluence(OrganizationType organizationType, int delta) {
        switch (organizationType) {
            case VAMPIRE_COVEN -> vampireInfluence = clamp(vampireInfluence + delta);
            case HUNTER_SOCIETY -> hunterInfluence = clamp(hunterInfluence + delta);
            case WEREWOLF_PACK -> werewolfInfluence = clamp(werewolfInfluence + delta);
            case WITCH_COVEN -> witchInfluence = clamp(witchInfluence + delta);
        }
    }

    public void setValues(int publicAwareness, int vampireInfluence, int hunterInfluence,
                          int werewolfInfluence, int witchInfluence, int fear, int suspicion) {
        this.publicAwareness = clamp(publicAwareness);
        this.vampireInfluence = clamp(vampireInfluence);
        this.hunterInfluence = clamp(hunterInfluence);
        this.werewolfInfluence = clamp(werewolfInfluence);
        this.witchInfluence = clamp(witchInfluence);
        this.fear = clamp(fear);
        this.suspicion = clamp(suspicion);
    }

    public void setPoliticalImportance(int politicalImportance) {
        this.politicalImportance = clamp(politicalImportance);
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
