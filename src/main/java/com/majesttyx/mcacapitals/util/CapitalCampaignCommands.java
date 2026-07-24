package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalCampaignService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalCampaignCommands {

    private CapitalCampaignCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalcampaign")
                        .then(
                                Commands.literal("launch")
                                        .then(
                                                Commands.argument(
                                                                "ambassadorId",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targetCapitalId",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(context -> launch(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "ambassadorId"
                                                                                ),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "targetCapitalId"
                                                                                )
                                                                        ))
                                                        )
                                        )
                        )
        );
    }

    private static int launch(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawTargetCapitalId
    ) {
        ServerPlayer player =
                getPlayer(source);

        UUID ambassadorId = parseUuid(
                source,
                rawAmbassadorId,
                "The Ambassador ID is invalid."
        );

        UUID targetCapitalId = parseUuid(
                source,
                rawTargetCapitalId,
                "The target capital ID is invalid."
        );

        if (player == null
                || ambassadorId == null
                || targetCapitalId == null) {
            return 0;
        }

        return CapitalCampaignService
                .launchCampaign(
                        player,
                        ambassadorId,
                        targetCapitalId
                );
    }

    private static UUID parseUuid(
            CommandSourceStack source,
            String rawValue,
            String failureMessage
    ) {
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(
                    Component.literal(failureMessage)
            );

            return null;
        }
    }

    private static ServerPlayer getPlayer(
            CommandSourceStack source
    ) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(
                    Component.literal(
                            "Only a player sovereign may launch a military campaign."
                    )
            );

            return null;
        }
    }
}