package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorUrgentMatterService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalAmbassadorUrgentMatterCommands {

    private CapitalAmbassadorUrgentMatterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalurgent")
                        .then(Commands.literal("continue")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .executes(context -> withAmbassador(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "ambassadorId"),
                                                CapitalAmbassadorUrgentMatterService::continueConversation
                                        ))))
                        .then(Commands.literal("open")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .executes(context -> withAmbassador(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "ambassadorId"),
                                                CapitalAmbassadorUrgentMatterService::open
                                        ))))
                        .then(Commands.literal("proposals")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .executes(context -> withAmbassador(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "ambassadorId"),
                                                CapitalAmbassadorUrgentMatterService::openProposals
                                        ))))
                        .then(Commands.literal("proposal")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("proposalId", StringArgumentType.word())
                                                .executes(context -> withTwoIds(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "ambassadorId"),
                                                        StringArgumentType.getString(context, "proposalId"),
                                                        CapitalAmbassadorUrgentMatterService::openProposal
                                                )))))
                        .then(Commands.literal("packages")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .executes(context -> withAmbassador(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "ambassadorId"),
                                                CapitalAmbassadorUrgentMatterService::openPackages
                                        ))))
                        .then(Commands.literal("package")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("shipmentId", StringArgumentType.word())
                                                .executes(context -> withTwoIds(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "ambassadorId"),
                                                        StringArgumentType.getString(context, "shipmentId"),
                                                        CapitalAmbassadorUrgentMatterService::openPackage
                                                )))))
        );
    }

    private static int withAmbassador(
            CommandSourceStack source,
            String rawAmbassadorId,
            AmbassadorAction action
    ) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        UUID ambassadorId = parse(source, rawAmbassadorId, "Ambassador");
        return ambassadorId == null ? 0 : action.run(player, ambassadorId);
    }

    private static int withTwoIds(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawMatterId,
            MatterAction action
    ) {
        ServerPlayer player = player(source);
        if (player == null) {
            return 0;
        }
        UUID ambassadorId = parse(source, rawAmbassadorId, "Ambassador");
        UUID matterId = parse(source, rawMatterId, "diplomatic matter");
        return ambassadorId == null || matterId == null
                ? 0
                : action.run(player, ambassadorId, matterId);
    }

    private static UUID parse(
            CommandSourceStack source,
            String raw,
            String label
    ) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(Component.literal("The " + label + " ID is invalid."));
            return null;
        }
    }

    private static ServerPlayer player(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal(
                    "Only a player may review Ambassador matters."
            ));
            return null;
        }
    }

    @FunctionalInterface
    private interface AmbassadorAction {
        int run(ServerPlayer player, UUID ambassadorId);
    }

    @FunctionalInterface
    private interface MatterAction {
        int run(ServerPlayer player, UUID ambassadorId, UUID matterId);
    }
}
