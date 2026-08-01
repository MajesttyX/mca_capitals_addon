package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticCorrespondenceService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class CapitalDiplomaticProposalResponseCommands {

    private CapitalDiplomaticProposalResponseCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalsacceptproposal")
                        .executes(context -> respondWithoutId(
                                context.getSource(),
                                true
                        ))
                        .then(
                                Commands.argument(
                                                "proposalId",
                                                StringArgumentType.word()
                                        )
                                        .executes(context -> respondWithId(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "proposalId"
                                                ),
                                                true
                                        ))
                        )
        );

        dispatcher.register(
                Commands.literal("capitalsrejectproposal")
                        .executes(context -> respondWithoutId(
                                context.getSource(),
                                false
                        ))
                        .then(
                                Commands.argument(
                                                "proposalId",
                                                StringArgumentType.word()
                                        )
                                        .executes(context -> respondWithId(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context,
                                                        "proposalId"
                                                ),
                                                false
                                        ))
                        )
        );
    }

    private static int respondWithoutId(
            CommandSourceStack source,
            boolean accept
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

        List<DiplomaticProposal> pending =
                CapitalDiplomaticAgreementService.getPendingForPlayer(
                        player.serverLevel(),
                        player.getUUID()
                );

        if (pending.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "You have no diplomatic proposals awaiting a response."
                    )
            );

            return 0;
        }

        if (pending.size() == 1) {
            DiplomaticProposal proposal =
                    pending.get(0);

            return resolve(
                    player,
                    proposal.getProposalId(),
                    accept
            );
        }

        showPendingList(
                player,
                pending,
                accept
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
            source.sendFailure(
                    Component.literal(
                            "That diplomatic proposal ID is invalid."
                    )
            );

            return 0;
        }

        return resolve(
                player,
                proposalId,
                accept
        );
    }

    private static int resolve(
            ServerPlayer player,
            UUID proposalId,
            boolean accept
    ) {
        return accept
                ? CapitalDiplomaticAgreementService.accept(
                player,
                proposalId
        )
                : CapitalDiplomaticAgreementService.reject(
                player,
                proposalId
        );
    }

    private static void showPendingList(
            ServerPlayer player,
            List<DiplomaticProposal> pending,
            boolean accept
    ) {
        ServerLevel level = player.serverLevel();

        player.sendSystemMessage(
                Component.literal(
                        accept
                                ? "Choose the diplomatic proposal to accept:"
                                : "Choose the diplomatic proposal to reject:"
                )
        );

        for (DiplomaticProposal proposal : pending) {
            CapitalRecord sourceCapital =
                    CapitalManager.getCapital(
                            proposal.getSourceCapitalId()
                    );

            String sourceName =
                    CapitalDiplomaticCorrespondenceService.getCapitalName(
                            level,
                            sourceCapital
                    );

            String command =
                    accept
                            ? "/capitalsacceptproposal "
                            + proposal.getProposalId()
                            : "/capitalsrejectproposal "
                            + proposal.getProposalId();

            String label = accept
                    ? "[Accept] "
                    : "[Reject] ";

            ChatFormatting color = accept
                    ? ChatFormatting.DARK_GREEN
                    : ChatFormatting.RED;

            MutableComponent line =
                    Component.literal(label)
                            .setStyle(
                                    Style.EMPTY
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
                                                            Component.literal(
                                                                    proposal.getType()
                                                                            .getDisplayName()
                                                            )
                                                    )
                                            )
                            )
                            .append(
                                    Component.literal(
                                            sourceName
                                                    + " — "
                                                    + proposal.getType()
                                                    .getDisplayName()
                                    ).withStyle(
                                            ChatFormatting.GOLD
                                    )
                            );

            player.sendSystemMessage(line);
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
                            "Only a player may answer diplomatic proposals."
                    )
            );

            return null;
        }
    }
}