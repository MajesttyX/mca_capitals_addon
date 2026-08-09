package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class CapitalDiplomaticProposalResponseCommands {

    private CapitalDiplomaticProposalResponseCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalsacceptproposal")
                        .executes(context -> showPending(context.getSource(), true))
                        .then(Commands.argument("proposalId", StringArgumentType.word())
                                .executes(context -> respond(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "proposalId"),
                                        true
                                )))
        );
        dispatcher.register(
                Commands.literal("capitalsrejectproposal")
                        .executes(context -> showPending(context.getSource(), false))
                        .then(Commands.argument("proposalId", StringArgumentType.word())
                                .executes(context -> respond(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "proposalId"),
                                        false
                                )))
        );
    }

    private static int showPending(CommandSourceStack source, boolean accepting) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        List<DiplomaticProposal> proposals =
                CapitalDiplomaticAgreementService.getPendingForPlayer(
                        player.serverLevel(),
                        player.getUUID()
                );
        if (proposals.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "You have no diplomatic proposals awaiting a response."
            ));
            return 0;
        }
        player.sendSystemMessage(Component.literal(
                "Speak to your Ambassador to review the urgent proposal, or use /"
                        + (accepting ? "capitalsacceptproposal" : "capitalsrejectproposal")
                        + " <proposalId>."
        ));
        for (DiplomaticProposal proposal : proposals) {
            player.sendSystemMessage(Component.literal(
                    proposal.getProposalId() + " — "
                            + proposal.getType().getDisplayName()
            ));
        }
        return proposals.size();
    }

    private static int respond(
            CommandSourceStack source,
            String rawProposalId,
            boolean accept
    ) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        UUID proposalId;
        try {
            proposalId = UUID.fromString(rawProposalId);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(Component.literal("The diplomatic proposal ID is invalid."));
            return 0;
        }
        return accept
                ? CapitalDiplomaticAgreementService.accept(player, proposalId)
                : CapitalDiplomaticAgreementService.reject(player, proposalId);
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal(
                    "Only a player may answer a diplomatic proposal."
            ));
            return null;
        }
    }
}
