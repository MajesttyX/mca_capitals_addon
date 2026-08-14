package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticGiftService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalDiplomaticGiftCommands {

    private CapitalDiplomaticGiftCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalgift")
                        .then(
                                Commands.literal("send")
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
                                                                        .executes(
                                                                                context -> send(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "ambassadorId"
                                                                                        ),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "targetCapitalId"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int send(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawTargetCapitalId
    ) {
        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(
                    Component.translatable("mcacapitals.system.capital_diplomatic_gift_commands.only_a_player_may_send_a_diplomatic_package")
            );

            return 0;
        }

        UUID ambassadorId;
        UUID targetCapitalId;

        try {
            ambassadorId =
                    UUID.fromString(
                            rawAmbassadorId
                    );

            targetCapitalId =
                    UUID.fromString(
                            rawTargetCapitalId
                    );
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(
                    Component.translatable("mcacapitals.system.capital_diplomatic_gift_commands.the_diplomatic_destination_is_invalid")
            );

            return 0;
        }

        return CapitalDiplomaticGiftService.dispatch(
                player,
                ambassadorId,
                targetCapitalId
        );
    }
}