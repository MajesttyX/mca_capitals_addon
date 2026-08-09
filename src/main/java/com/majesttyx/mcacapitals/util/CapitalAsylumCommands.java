package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorUrgentMatterService;
import com.majesttyx.mcacapitals.capital.CapitalAsylumScreenService;
import com.majesttyx.mcacapitals.capital.CapitalAsylumService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalAsylumCommands {

    private CapitalAsylumCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalasylum")
                        .then(
                                Commands.literal("review")
                                        .then(
                                                Commands.argument(
                                                                "ambassadorId",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        review(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "ambassadorId"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("grant")
                                        .then(
                                                Commands.argument(
                                                                "ambassadorId",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "refugeeId",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        grant(
                                                                                                context.getSource(),
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "ambassadorId"
                                                                                                ),
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "refugeeId"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int review(
            CommandSourceStack source,
            String rawAmbassadorId
    ) {
        ServerPlayer player =
                getPlayer(
                        source
                );

        UUID ambassadorId =
                parseUuid(
                        source,
                        rawAmbassadorId,
                        "The Ambassador ID is invalid."
                );

        if (player == null
                || ambassadorId == null) {
            return 0;
        }

        return CapitalAsylumScreenService.openRequests(
                player,
                ambassadorId
        );
    }

    private static int grant(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawRefugeeId
    ) {
        ServerPlayer player =
                getPlayer(
                        source
                );

        UUID ambassadorId =
                parseUuid(
                        source,
                        rawAmbassadorId,
                        "The Ambassador ID is invalid."
                );

        UUID refugeeId =
                parseUuid(
                        source,
                        rawRefugeeId,
                        "The refugee ID is invalid."
                );

        if (player == null
                || ambassadorId == null
                || refugeeId == null) {
            return 0;
        }

        int result =
                CapitalAsylumService.grantAsylum(
                        player,
                        ambassadorId,
                        refugeeId
                );

        CapitalAmbassadorUrgentMatterService.continueConversation(
                player,
                ambassadorId
        );

        return result;
    }

    private static UUID parseUuid(
            CommandSourceStack source,
            String rawValue,
            String failureMessage
    ) {
        try {
            return UUID.fromString(
                    rawValue
            );
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(
                    Component.literal(
                            failureMessage
                    )
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
                            "Only a player may review asylum requests."
                    )
            );

            return null;
        }
    }
}