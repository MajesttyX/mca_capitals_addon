package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum DiplomaticProposalType {
    NON_AGGRESSION_PACT(
            CapitalDiplomaticState.NON_AGGRESSION_PACT,
            30,
            10
    ),
    ALLIANCE(
            CapitalDiplomaticState.ALLIANCE,
            100,
            25
    ),
    TRUCE(
            CapitalDiplomaticState.TRUCE,
            -249,
            10
    ),
    TRADE_AGREEMENT(
            null,
            30,
            10
    ),
    ROYAL_BETROTHAL(
            null,
            60,
            10
    );

    private final CapitalDiplomaticState resultingState;
    private final int minimumRelationship;
    private final int acceptanceBonus;

    DiplomaticProposalType(
            CapitalDiplomaticState resultingState,
            int minimumRelationship,
            int acceptanceBonus
    ) {
        this.resultingState = resultingState;
        this.minimumRelationship = minimumRelationship;
        this.acceptanceBonus = acceptanceBonus;
    }

    public Component getDisplayComponent() {
        return Component.translatable(
                "mcacapitals.diplomacy.proposal." + getSerializedName()
        );
    }

    public Component getIndefiniteComponent() {
        return Component.translatable(
                "mcacapitals.diplomacy.proposal.indefinite." + getSerializedName()
        );
    }

    public Component getCapitalizedIndefiniteComponent() {
        return Component.translatable(
                "mcacapitals.diplomacy.proposal.indefinite_capitalized." + getSerializedName()
        );
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
