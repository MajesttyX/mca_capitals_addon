package com.majesttyx.mcacapitals.data;

import java.util.Locale;

public enum CapitalCampaignPhase {
    MUSTERING,
    ACTIVE,
    RETREATING;

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CapitalCampaignPhase fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return MUSTERING;
        }
        for (CapitalCampaignPhase phase : values()) {
            if (phase.getSerializedName().equals(value)) {
                return phase;
            }
        }

        return MUSTERING;
    }
}
