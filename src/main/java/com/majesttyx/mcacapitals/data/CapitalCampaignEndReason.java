package com.majesttyx.mcacapitals.data;

import java.util.Locale;

public enum CapitalCampaignEndReason {
    NONE,
    DEFENDING_SOVEREIGN_DIED,
    ATTACKING_SOVEREIGN_DIED,
    PEACE_ACCEPTED,
    ATTACKERS_DEFEATED,
    DEFENDERS_SURRENDERED,
    COMMANDER_ORDERED_RETREAT,
    INVALIDATED;

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
    public static CapitalCampaignEndReason fromSerializedName(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        for (CapitalCampaignEndReason reason : values()) {
            if (reason.getSerializedName()
                    .equals(value)) {
                return reason;
            }
        }

        return NONE;
    }
}
