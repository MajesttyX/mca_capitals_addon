package com.majesttyx.mcacapitals.data;

import java.util.Locale;

public enum CapitalWarGoal {
    PUNITIVE("Punitive War"),
    DEPOSITION("War of Deposition");

    private final String displayName;

    CapitalWarGoal(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
    public static CapitalWarGoal fromSerializedName(String value) {
        if (value != null) {
            for (CapitalWarGoal goal : values()) {
                if (goal.getSerializedName().equalsIgnoreCase(value)
                        || goal.name().equalsIgnoreCase(value)) {
                    return goal;
                }
            }
        }

        return PUNITIVE;
    }
}
