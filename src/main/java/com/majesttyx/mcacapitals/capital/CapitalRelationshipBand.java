package com.majesttyx.mcacapitals.capital;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum CapitalRelationshipBand {
    EXCELLENT,
    FRIENDLY,
    CORDIAL,
    NEUTRAL,
    STRAINED,
    HOSTILE,
    BITTER_ENEMIES;

    public Component getDisplayComponent() {
        return Component.translatable(
                "mcacapitals.diplomacy.relationship_band."
                        + name().toLowerCase(Locale.ROOT)
        );
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
