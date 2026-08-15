package com.majesttyx.mcacapitals.capital;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

final class CapitalDiplomaticAgreementText {

    private CapitalDiplomaticAgreementText() {
    }

    static Component stateDisplay(CapitalDiplomaticState state) {
        CapitalDiplomaticState resolved = state == null
                ? CapitalDiplomaticState.PEACE
                : state;

        return Component.translatable(
                "mcacapitals.diplomacy.state."
                        + resolved.getSerializedName()
        );
    }

    static String capitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return CapitalDiplomaticCorrespondenceService.getCapitalName(level, capital);
    }

    static Component capitalNameComponent(
            ServerLevel level,
            CapitalRecord capital
    ) {
        String name = capitalName(level, capital);
        return name == null
                || name.isBlank()
                || "Unknown Capital".equals(name)
                ? Component.translatable("mcacapitals.diplomacy.unknown_capital")
                : Component.literal(name);
    }
}
