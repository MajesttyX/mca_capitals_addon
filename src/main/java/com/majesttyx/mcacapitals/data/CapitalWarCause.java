package com.majesttyx.mcacapitals.data;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum CapitalWarCause {
    TREATY_BROKEN(true),
    PREVIOUS_AGGRESSION(true),
    ALLY_ATTACKED(true),
    ASYLUM_DISPUTE(true),
    SERIOUS_ASYLUM_DISPUTE(true),
    FOREIGN_STORAGE_RAID(true),
    REFUSED_REPARATIONS(true),
    HOSTILE_RELATIONS(true),
    HARMED_CROWN_OFFICIAL(true),
    UNJUST(false);

    private final boolean justified;

    CapitalWarCause(boolean justified) {
        this.justified = justified;
    }

    public Component getDisplayComponent() {
        return Component.translatable(
                "mcacapitals.war.cause." + getSerializedName()
        );
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
