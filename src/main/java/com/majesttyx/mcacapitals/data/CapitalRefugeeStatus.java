package com.majesttyx.mcacapitals.data;

import java.util.Locale;

public enum CapitalRefugeeStatus {
    EXILED,
    ASYLUM_GRANTED;

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CapitalRefugeeStatus fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return EXILED;
        }
        for (CapitalRefugeeStatus status : values()) {
            if (status.getSerializedName().equals(value)) {
                return status;
            }
        }

        return EXILED;
    }
}
