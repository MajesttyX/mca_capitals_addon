package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalRoyalBetrothalService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalRoyalEscortCommands {

    private CapitalRoyalEscortCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalroyalescort")
                        .then(
                                Commands.literal("review")
                                        .then(
                                                Commands.argument(
                                                                "ambassadorId",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(context -> review(
                                                                context.getSource(),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "ambassadorId"
                                                                )
                                                        ))
                                        )
                        )
        );
    }

    private static int review(
            CommandSourceStack source,
            String rawAmbassadorId
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(
                    Component.translatable("mcacapitals.system.capital_royal_escort_commands.only_a_player_may_review_royal_escort_requests")
            );

            return 0;
        }

        UUID ambassadorId;

        try {
            ambassadorId = UUID.fromString(
                    rawAmbassadorId
            );
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(
                    Component.translatable("mcacapitals.system.capital_royal_escort_commands.the_ambassador_id_is_invalid")
            );

            return 0;
        }

        return CapitalRoyalBetrothalService
                .openEscortRequests(
                        player,
                        ambassadorId
                );
    }
}