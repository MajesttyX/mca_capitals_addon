package com.majesttyx.mcacapitals.data;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum CapitalPublicCrownStatus {
    RECOGNIZED_FRIEND,
    DISCOVERED_ENEMY,
    RESTORED_TO_PEACE;

    public Component getDisplayComponent() {
        return Component.translatable(
                "mcacapitals.justice.public_status." + getSerializedName()
        );
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
