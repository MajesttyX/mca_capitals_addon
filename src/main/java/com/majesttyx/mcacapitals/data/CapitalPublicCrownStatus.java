package com.majesttyx.mcacapitals.data;

import java.util.Locale;

public enum CapitalPublicCrownStatus {
    RECOGNIZED_FRIEND("Recognized Friend of the Crown"),
    DISCOVERED_ENEMY("Discovered Enemy of the Crown"),
    RESTORED_TO_PEACE("Restored to the Crown's Peace");

    private final String displayName;

    CapitalPublicCrownStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CapitalPublicCrownStatus fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (CapitalPublicCrownStatus status : values()) {
            if (status.getSerializedName().equals(value)) {
                return status;
            }
        }

        return null;
    }
}