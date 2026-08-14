package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticResolutionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalDiplomaticResponseCommands {

    private CapitalDiplomaticResponseCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalsaccept")
                        .executes(context -> directToAmbassador(context.getSource()))
                        .then(
                                Commands.argument("shipmentId", StringArgumentType.word())
                                        .executes(context -> respondWithId(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "shipmentId"),
                                                true
                                        ))
                        )
        );

        dispatcher.register(
                Commands.literal("capitalsreturn")
                        .executes(context -> directToAmbassador(context.getSource()))
                        .then(
                                Commands.argument("shipmentId", StringArgumentType.word())
                                        .executes(context -> respondWithId(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "shipmentId"),
                                                false
                                        ))
                        )
        );
    }

    private static int directToAmbassador(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

        player.sendSystemMessage(
                Component.translatable("mcacapitals.system.capital_diplomatic_response_commands.speak_to_your_ambassador_to_inspect_and_answer_pending_diplomatic_pack")
        );
        return 1;
    }

    private static int respondWithId(
            CommandSourceStack source,
            String rawShipmentId,
            boolean accept
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

        UUID shipmentId;

        try {
            shipmentId = UUID.fromString(rawShipmentId);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_diplomatic_response_commands.that_diplomatic_package_id_is_invalid"));
            return 0;
        }

        return accept
                ? CapitalDiplomaticResolutionService.acceptPlayerShipment(player, shipmentId)
                : CapitalDiplomaticResolutionService.returnPlayerShipment(player, shipmentId);
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_diplomatic_response_commands.only_a_player_may_answer_diplomatic_packages"));
            return null;
        }
    }
}