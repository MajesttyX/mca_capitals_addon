package com.majesttyx.mcacapitals.data;

import java.util.UUID;

public record CapitalRouteKey(
        UUID sourceCapitalId,
        UUID targetCapitalId
) {

    public CapitalRouteKey {
        if (sourceCapitalId == null
                || targetCapitalId == null) {
            throw new IllegalArgumentException(
                    "Capital route IDs cannot be null."
            );
        }

        if (sourceCapitalId.equals(targetCapitalId)) {
            throw new IllegalArgumentException(
                    "A capital cannot send diplomacy to itself."
            );
        }
    }
}