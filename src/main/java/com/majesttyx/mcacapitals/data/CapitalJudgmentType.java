package com.majesttyx.mcacapitals.data;

import java.util.Locale;

public enum CapitalJudgmentType {
    IMPRISONMENT("Imprisonment"),
    EXECUTION("Marked for Execution"),
    PARDON("Royal Pardon"),
    EXILE("Exile");

    private final String displayName;

    CapitalJudgmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CapitalJudgmentType fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (CapitalJudgmentType type : values()) {
            if (type.getSerializedName().equals(value)) {
                return type;
            }
        }

        return null;
    }
}
