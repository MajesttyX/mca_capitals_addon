package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.capital.CapitalForeignRelationsMenuService;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalDiplomacyCommands {

    private CapitalDiplomacyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaldiplomacy")
                        .then(Commands.literal("targets")
                                .then(uuidArgument("ambassadorId")
                                        .executes(context -> withOneUuid(
                                                context.getSource(),
                                                context,
                                                "ambassadorId",
                                                CapitalForeignRelationsMenuService::openTargets
                                        ))))
                        .then(Commands.literal("options")
                                .then(uuidArgument("ambassadorId")
                                        .then(uuidArgument("targetCapitalId")
                                                .executes(context -> withTwoUuids(
                                                        context.getSource(),
                                                        context,
                                                        "ambassadorId",
                                                        "targetCapitalId",
                                                        CapitalForeignRelationsMenuService::openOptions
                                                )))))
                        .then(Commands.literal("propose")
                                .then(uuidArgument("ambassadorId")
                                        .then(uuidArgument("targetCapitalId")
                                                .then(Commands.argument("proposalType", StringArgumentType.word())
                                                        .executes(context -> propose(
                                                                context.getSource(),
                                                                uuid(context, "ambassadorId"),
                                                                uuid(context, "targetCapitalId"),
                                                                StringArgumentType.getString(context, "proposalType")
                                                        ))))))
                        .then(Commands.literal("end_trade")
                                .then(uuidArgument("ambassadorId")
                                        .then(uuidArgument("targetCapitalId")
                                                .executes(context -> withTwoUuids(
                                                        context.getSource(),
                                                        context,
                                                        "ambassadorId",
                                                        "targetCapitalId",
                                                        CapitalDiplomaticAgreementService::endTradeAgreement
                                                )))))
                        .then(Commands.literal("betrothal_source")
                                .then(uuidArgument("ambassadorId")
                                        .then(uuidArgument("targetCapitalId")
                                                .executes(context -> withTwoUuids(
                                                        context.getSource(),
                                                        context,
                                                        "ambassadorId",
                                                        "targetCapitalId",
                                                        CapitalDiplomaticAgreementService::openBetrothalSourceSelection
                                                )))))
                        .then(Commands.literal("betrothal_target")
                                .then(uuidArgument("ambassadorId")
                                        .then(uuidArgument("targetCapitalId")
                                                .then(uuidArgument("sourceRoyalId")
                                                        .executes(context -> withThreeUuids(
                                                                context.getSource(),
                                                                context,
                                                                "ambassadorId",
                                                                "targetCapitalId",
                                                                "sourceRoyalId",
                                                                CapitalDiplomaticAgreementService::openBetrothalTargetSelection
                                                        ))))))
                        .then(Commands.literal("betrothal_settlement")
                                .then(uuidArgument("ambassadorId")
                                        .then(uuidArgument("targetCapitalId")
                                                .then(uuidArgument("sourceRoyalId")
                                                        .then(uuidArgument("targetRoyalId")
                                                                .executes(context -> withFourUuids(
                                                                        context.getSource(),
                                                                        context,
                                                                        "ambassadorId",
                                                                        "targetCapitalId",
                                                                        "sourceRoyalId",
                                                                        "targetRoyalId",
                                                                        CapitalDiplomaticAgreementService::openBetrothalSettlementSelection
                                                                )))))))
                        .then(Commands.literal("betrothal_send")
                                .then(uuidArgument("ambassadorId")
                                        .then(uuidArgument("targetCapitalId")
                                                .then(uuidArgument("sourceRoyalId")
                                                        .then(uuidArgument("targetRoyalId")
                                                                .then(uuidArgument("destinationCapitalId")
                                                                        .executes(context -> withFiveUuids(
                                                                                context.getSource(),
                                                                                context,
                                                                                "ambassadorId",
                                                                                "targetCapitalId",
                                                                                "sourceRoyalId",
                                                                                "targetRoyalId",
                                                                                "destinationCapitalId",
                                                                                CapitalDiplomaticAgreementService::proposeSelectedBetrothal
                                                                        ))))))))
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String>
    uuidArgument(String name) {
        return Commands.argument(name, StringArgumentType.word());
    }

    private static int propose(
            CommandSourceStack source,
            UUID ambassadorId,
            UUID targetCapitalId,
            String rawType
    ) {
        ServerPlayer player = player(source);
        if (player == null || ambassadorId == null || targetCapitalId == null) {
            return 0;
        }
        DiplomaticProposalType type = DiplomaticProposalType.fromSerializedName(rawType);
        if (type == null) {
            source.sendFailure(Component.literal("That diplomatic proposal type is invalid."));
            return 0;
        }
        return CapitalDiplomaticAgreementService.propose(
                player,
                ambassadorId,
                targetCapitalId,
                type
        );
    }

    private static int withOneUuid(
            CommandSourceStack source,
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            String first,
            OneUuidAction action
    ) {
        ServerPlayer player = player(source);
        UUID firstId = uuid(context, first);
        return player == null || firstId == null ? 0 : action.run(player, firstId);
    }

    private static int withTwoUuids(
            CommandSourceStack source,
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            String first,
            String second,
            TwoUuidAction action
    ) {
        ServerPlayer player = player(source);
        UUID firstId = uuid(context, first);
        UUID secondId = uuid(context, second);
        return player == null || firstId == null || secondId == null
                ? 0
                : action.run(player, firstId, secondId);
    }

    private static int withThreeUuids(
            CommandSourceStack source,
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            String first,
            String second,
            String third,
            ThreeUuidAction action
    ) {
        ServerPlayer player = player(source);
        UUID firstId = uuid(context, first);
        UUID secondId = uuid(context, second);
        UUID thirdId = uuid(context, third);
        return player == null || firstId == null || secondId == null || thirdId == null
                ? 0
                : action.run(player, firstId, secondId, thirdId);
    }

    private static int withFourUuids(
            CommandSourceStack source,
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            String first,
            String second,
            String third,
            String fourth,
            FourUuidAction action
    ) {
        ServerPlayer player = player(source);
        UUID firstId = uuid(context, first);
        UUID secondId = uuid(context, second);
        UUID thirdId = uuid(context, third);
        UUID fourthId = uuid(context, fourth);
        return player == null || firstId == null || secondId == null || thirdId == null || fourthId == null
                ? 0
                : action.run(player, firstId, secondId, thirdId, fourthId);
    }

    private static int withFiveUuids(
            CommandSourceStack source,
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            String first,
            String second,
            String third,
            String fourth,
            String fifth,
            FiveUuidAction action
    ) {
        ServerPlayer player = player(source);
        UUID firstId = uuid(context, first);
        UUID secondId = uuid(context, second);
        UUID thirdId = uuid(context, third);
        UUID fourthId = uuid(context, fourth);
        UUID fifthId = uuid(context, fifth);
        return player == null || firstId == null || secondId == null || thirdId == null
                || fourthId == null || fifthId == null
                ? 0
                : action.run(player, firstId, secondId, thirdId, fourthId, fifthId);
    }

    private static UUID uuid(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            String name
    ) {
        try {
            return UUID.fromString(StringArgumentType.getString(context, name));
        } catch (IllegalArgumentException ignored) {
            context.getSource().sendFailure(Component.literal("A diplomatic identifier is invalid."));
            return null;
        }
    }

    private static ServerPlayer player(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("Only a player may use diplomatic court actions."));
            return null;
        }
    }

    @FunctionalInterface
    private interface OneUuidAction {
        int run(ServerPlayer player, UUID first);
    }

    @FunctionalInterface
    private interface TwoUuidAction {
        int run(ServerPlayer player, UUID first, UUID second);
    }

    @FunctionalInterface
    private interface ThreeUuidAction {
        int run(ServerPlayer player, UUID first, UUID second, UUID third);
    }

    @FunctionalInterface
    private interface FourUuidAction {
        int run(ServerPlayer player, UUID first, UUID second, UUID third, UUID fourth);
    }

    @FunctionalInterface
    private interface FiveUuidAction {
        int run(ServerPlayer player, UUID first, UUID second, UUID third, UUID fourth, UUID fifth);
    }
}
