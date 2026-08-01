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
        if (score >= 75) {
            return EXCELLENT;
        }

        if (score >= 40) {
            return FRIENDLY;
        }

        if (score >= 10) {
            return CORDIAL;
        }

        if (score >= -9) {
            return NEUTRAL;
        }

        if (score >= -39) {
            return STRAINED;
        }

        if (score >= -74) {
            return HOSTILE;
        }

        return BITTER_ENEMIES;
    }
}