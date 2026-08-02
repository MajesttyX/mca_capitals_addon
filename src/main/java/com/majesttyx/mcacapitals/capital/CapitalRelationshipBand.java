package com.majesttyx.mcacapitals.capital;

public enum CapitalRelationshipBand {
    EXCELLENT("Excellent"),
    FRIENDLY("Friendly"),
    CORDIAL("Cordial"),
    NEUTRAL("Neutral"),
    STRAINED("Strained"),
    HOSTILE("Hostile"),
    BITTER_ENEMIES("Bitter Enemies");

    private final String displayName;

    CapitalRelationshipBand(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CapitalRelationshipBand fromScore(int score) {
        if (score >= 250) {
            return EXCELLENT;
        }

        if (score >= 100) {
            return FRIENDLY;
        }

        if (score >= 30) {
            return CORDIAL;
        }

        if (score >= -29) {
            return NEUTRAL;
        }

        if (score >= -99) {
            return STRAINED;
        }

        if (score >= -249) {
            return HOSTILE;
        }

        return BITTER_ENEMIES;
    }
}
