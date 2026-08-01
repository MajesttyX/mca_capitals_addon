package com.majesttyx.mcacapitals.data;

public enum DiplomaticProposalStatus {
    DISPATCHED,
    AWAITING_PLAYER_RESPONSE,
    ACCEPTED_RESPONSE_IN_TRANSIT,
    REJECTED_RESPONSE_IN_TRANSIT;

    public static DiplomaticProposalStatus fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return DISPATCHED;
        }

        for (DiplomaticProposalStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        return DISPATCHED;
    }
}