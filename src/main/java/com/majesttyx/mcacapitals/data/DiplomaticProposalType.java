package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;

import java.util.Locale;

public enum DiplomaticProposalType {
    NON_AGGRESSION_PACT(
            "Non-Aggression Pact",
            CapitalDiplomaticState.NON_AGGRESSION_PACT,
            30,
            10
    ),
    ALLIANCE(
            "Alliance",
            CapitalDiplomaticState.ALLIANCE,
            100,
            25
    ),
    TRUCE(
            "Truce",
            CapitalDiplomaticState.TRUCE,
            -249,
            10
    ),
    TRADE_AGREEMENT(
            "Trade Agreement",
            null,
            30,
            10
    ),
    ROYAL_BETROTHAL(
            "Royal Betrothal",
            null,
            60,
            10
    );

    private final String displayName;
    private final CapitalDiplomaticState resultingState;
    private final int minimumRelationship;
    private final int acceptanceBonus;

    DiplomaticProposalType(
            String displayName,
            CapitalDiplomaticState resultingState,
            int minimumRelationship,
            int acceptanceBonus
    ) {
        this.displayName = displayName;
        this.resultingState = resultingState;
        this.minimumRelationship = minimumRelationship;
        this.acceptanceBonus = acceptanceBonus;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CapitalDiplomaticState getResultingState() {
        return resultingState;
    }

    public boolean changesDiplomaticState() {
        return resultingState != null;
    }

    public int getMinimumRelationship() {
        return minimumRelationship;
    }

    public int getAcceptanceBonus() {
        return acceptanceBonus;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DiplomaticProposalType fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (DiplomaticProposalType type : values()) {
            if (type.getSerializedName().equals(value)) {
                return type;
            }
        }

        return null;
    }
}