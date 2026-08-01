package com.majesttyx.mcacapitals.data;

import java.util.Locale;

public enum DiplomaticShipmentStatus {
    DISPATCHED,
    AWAITING_PLAYER_RESPONSE,
    ACCEPTED_RESPONSE_IN_TRANSIT,
    RETURNED_IN_TRANSIT,
    ACCEPTED,
    RETURNED;

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DiplomaticShipmentStatus fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return DISPATCHED;
        }

        for (DiplomaticShipmentStatus status : values()) {
            if (status.getSerializedName().equals(value)) {
                return status;
            }
        }

        return DISPATCHED;
    }
}