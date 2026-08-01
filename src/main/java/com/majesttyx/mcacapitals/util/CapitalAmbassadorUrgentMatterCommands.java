package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorUrgentMatterService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticResolutionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class CapitalAmbassadorUrgentMatterCommands {

    private CapitalAmbassadorUrgentMatterCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalurgent")
                        .then(
                                Commands.literal("proposal")
                                        .then(
                                                Commands.argument(
                                                                "ambassadorId",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "proposalId",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "decision",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(context -> resolveProposal(
                                                                                                context.getSource(),
                                                                                                StringArgumentType.getString(context, "ambassadorId"),
                                                                                                StringArgumentType.getString(context, "proposalId"),
                                                                                                StringArgumentType.getString(context, "decision")
                                                                                        ))
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("shipment")
                                        .then(
                                                Commands.argument(
                                                                "ambassadorId",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "shipmentId",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "decision",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(context -> resolveShipment(
                                                                                                context.getSource(),
                                                                                                StringArgumentType.getString(context, "ambassadorId"),
                                                                                                StringArgumentType.getString(context, "shipmentId"),
                                                                                                StringArgumentType.getString(context, "decision")
                                                                                        ))
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int resolveProposal(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawProposalId,
            String decision
    ) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, rawAmbassadorId, "The Ambassador ID is invalid.");
        UUID proposalId = parseUuid(source, rawProposalId, "The proposal ID is invalid.");

        if (player == null || ambassadorId == null || proposalId == null) {
            return 0;
        }

        int result;
        if ("accept".equalsIgnoreCase(decision)) {
            result = CapitalDiplomaticAgreementService.accept(player, proposalId);
        } else if ("reject".equalsIgnoreCase(decision)) {
            result = CapitalDiplomaticAgreementService.reject(player, proposalId);
        } else {
            source.sendFailure(Component.literal("Choose accept or reject."));
            return 0;
        }

        reopen(player, ambassadorId);
        return result;
    }

    private static int resolveShipment(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawShipmentId,
            String decision
    ) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, rawAmbassadorId, "The Ambassador ID is invalid.");
        UUID shipmentId = parseUuid(source, rawShipmentId, "The package ID is invalid.");

        if (player == null || ambassadorId == null || shipmentId == null) {
            return 0;
        }

        int result;
        if ("accept".equalsIgnoreCase(decision)) {
            result = CapitalDiplomaticResolutionService.acceptPlayerShipment(player, shipmentId);
        } else if ("return".equalsIgnoreCase(decision)) {
            result = CapitalDiplomaticResolutionService.returnPlayerShipment(player, shipmentId);
        } else {
            source.sendFailure(Component.literal("Choose accept or return."));
            return 0;
        }

        reopen(player, ambassadorId);
        return result;
    }

    private static void reopen(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        Entity ambassador = player.serverLevel().getEntity(ambassadorId);

        if (ambassador == null) {
            return;
        }

        if (!CapitalAmbassadorUrgentMatterService.openIfPresent(player, ambassador)) {
            CapitalDiplomaticAgreementService.openCapitalListDirect(player, ambassador);
        }
    }

    private static UUID parseUuid(
            CommandSourceStack source,
            String rawValue,
            String failureMessage
    ) {
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(Component.literal(failureMessage));
            return null;
        }
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("Only a player may answer diplomatic matters."));
            return null;
        }
    }
}