package com.majesttyx.mcacapitals.data;

import java.util.Locale;
public enum CapitalWarCause {
    TREATY_BROKEN("Treaty Broken", true),
    PREVIOUS_AGGRESSION("Previous Aggression", true),
    ALLY_ATTACKED("Ally Attacked", true),
    ASYLUM_DISPUTE("Asylum Dispute", true),
    SERIOUS_ASYLUM_DISPUTE("Serious Asylum Dispute", true),
    FOREIGN_STORAGE_RAID("Foreign Storage Raided", true),
    REFUSED_REPARATIONS("Refused Reparations", true),
    HOSTILE_RELATIONS("Entrenched Hostility", true),
    HARMED_CROWN_OFFICIAL("Crown Official Harmed", true),
    UNJUST("No Just Cause", false);
    private final String displayName;
    private final boolean justified;

    CapitalWarCause(String displayName, boolean justified) {
        this.displayName = displayName;
        this.justified = justified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isJustified() {
        return justified;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
    public static CapitalWarCause fromSerializedName(String value) {
        if (value != null) {
            for (CapitalWarCause cause : values()) {
                if (cause.getSerializedName().equalsIgnoreCase(value)
                        || cause.name().equalsIgnoreCase(value)) {
                    return cause;
                }
            }
        }

        return UNJUST;
    }
}
