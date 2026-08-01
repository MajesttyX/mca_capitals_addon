package com.majesttyx.mcacapitals.capital;

import java.util.Locale;

public enum CapitalDiplomaticState {
    PEACE,
    NON_AGGRESSION_PACT,
    ALLIANCE,
    TRUCE,
    WAR;

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CapitalDiplomaticState fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return PEACE;
        }

        for (CapitalDiplomaticState state : values()) {
            if (state.getSerializedName().equals(value)) {
                return state;
            }
        }

        return PEACE;
    }
}