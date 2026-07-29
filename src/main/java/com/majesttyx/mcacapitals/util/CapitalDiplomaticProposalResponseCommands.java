package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalDiplomaticProposalResponseCommands {

    private CapitalDiplomaticProposalResponseCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalsacceptproposal")
                        .executes(context -> directToAmbassador(context.getSource()))
                        .then(
                                Commands.argument("proposalId", StringArgumentType.word())
                                        .executes(context -> respondWithId(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "proposalId"),
                                                true
                                        ))
                        )
        );

        dispatcher.register(
                Commands.literal("capitalsrejectproposal")
                        .executes(context -> directToAmbassador(context.getSource()))
                        .then(
                                Commands.argument("proposalId", StringArgumentType.word())
                                        .executes(context -> respondWithId(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "proposalId"),
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
                Component.literal(
                        "Speak to your Ambassador to review and answer pending diplomatic proposals."
                )
        );
        return 1;
    }

    private static int respondWithId(
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
            source.sendFailure(Component.literal("That diplomatic proposal ID is invalid."));
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
            source.sendFailure(Component.literal("Only a player may answer diplomatic proposals."));
            return null;
        }
    }
}