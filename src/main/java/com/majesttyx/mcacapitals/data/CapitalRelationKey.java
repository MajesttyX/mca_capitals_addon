package com.majesttyx.mcacapitals.data;

import java.util.UUID;

public record CapitalRelationKey(UUID first, UUID second) {

    public CapitalRelationKey {
        if (first == null || second == null) {
            throw new IllegalArgumentException(
                    "Capital relationship IDs cannot be null."
            );
        }

        if (first.equals(second)) {
            throw new IllegalArgumentException(
                    "A capital cannot have a relationship with itself."
            );
        }

        if (first.toString().compareTo(second.toString()) > 0) {
            UUID originalFirst = first;
            first = second;
            second = originalFirst;
        }
    }

    public static CapitalRelationKey of(
            UUID first,
            UUID second
    ) {
        return new CapitalRelationKey(first, second);
    }
}