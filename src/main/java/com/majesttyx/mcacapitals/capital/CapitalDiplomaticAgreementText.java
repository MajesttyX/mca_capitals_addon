package com.majesttyx.mcacapitals.capital;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;

final class CapitalDiplomaticAgreementText {

    private CapitalDiplomaticAgreementText() {
    }

    static Style clickableStyle(
            ChatFormatting color,
            String command,
            String hoverText
    ) {
        return Style.EMPTY
                .withColor(color)
                .withBold(true)
                .withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                command
                        )
                )
                .withHoverEvent(
                        new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal(hoverText)
                        )
                );
    }

    static String stateDisplay(
            CapitalDiplomaticState state
    ) {
        return switch (state) {
            case PEACE -> "Peace";
            case NON_AGGRESSION_PACT ->
                    "Non-Aggression Pact";
            case ALLIANCE -> "Alliance";
            case TRUCE -> "Truce";
            case WAR -> "War";
        };
    }

    static String capitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return CapitalDiplomaticCorrespondenceService
                .getCapitalName(
                        level,
                        capital
                );
    }
}