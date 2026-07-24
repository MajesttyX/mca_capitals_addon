package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class CapitalDiplomaticAgreementCommands {

    private CapitalDiplomaticAgreementCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitaldiplomacy")
                        .then(
                                Commands.literal("targets")
                                        .then(
                                                Commands.argument(
                                                                "ambassadorId",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(context -> openTargets(
                                                                context.getSource(),
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "ambassadorId"
                                                                )
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("options")
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
                                                                        .executes(context -> openOptions(
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
                        .then(
                                Commands.literal("propose")
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
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "proposalType",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(context -> propose(
                                                                                                context.getSource(),
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "ambassadorId"
                                                                                                ),
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "targetCapitalId"
                                                                                                ),
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "proposalType"
                                                                                                )
                                                                                        ))
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("endtrade")
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
                                                                        .executes(context -> endTrade(
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
                        .then(
                                Commands.literal("war")
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
                                                                        .executes(context -> declareWar(
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

    private static int openTargets(
            CommandSourceStack source,
            String rawAmbassadorId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

        UUID ambassadorId = parseUuid(
                source,
                rawAmbassadorId,
                "The Ambassador ID is invalid."
        );

        if (ambassadorId == null) {
            return 0;
        }

        Entity ambassador = player.serverLevel().getEntity(
                ambassadorId
        );

        if (ambassador == null) {
            source.sendFailure(
                    Component.literal(
                            "The Ambassador is unavailable."
                    )
            );

            return 0;
        }

        return CapitalDiplomaticAgreementService.openCapitalList(
                player,
                ambassador
        ) ? 1 : 0;
    }

    private static int openOptions(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawTargetCapitalId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

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

        if (ambassadorId == null || targetCapitalId == null) {
            return 0;
        }

        return CapitalDiplomaticAgreementService.openActionList(
                player,
                ambassadorId,
                targetCapitalId
        );
    }

    private static int propose(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawTargetCapitalId,
            String rawProposalType
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

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

        if (ambassadorId == null || targetCapitalId == null) {
            return 0;
        }

        DiplomaticProposalType type =
                DiplomaticProposalType.fromSerializedName(
                        rawProposalType
                );

        if (type == null) {
            source.sendFailure(
                    Component.literal(
                            "That diplomatic proposal type is invalid."
                    )
            );

            return 0;
        }

        return CapitalDiplomaticAgreementService.propose(
                player,
                ambassadorId,
                targetCapitalId,
                type
        );
    }

    private static int endTrade(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawTargetCapitalId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

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

        if (ambassadorId == null || targetCapitalId == null) {
            return 0;
        }

        return CapitalDiplomaticAgreementService.endTradeAgreement(
                player,
                ambassadorId,
                targetCapitalId
        );
    }

    private static int declareWar(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawTargetCapitalId
    ) {
        ServerPlayer player = getPlayer(source);

        if (player == null) {
            return 0;
        }

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

        if (ambassadorId == null || targetCapitalId == null) {
            return 0;
        }

        return CapitalDiplomaticAgreementService.declareWar(
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
                            "Only a player may conduct formal diplomacy."
                    )
            );

            return null;
        }
    }
}