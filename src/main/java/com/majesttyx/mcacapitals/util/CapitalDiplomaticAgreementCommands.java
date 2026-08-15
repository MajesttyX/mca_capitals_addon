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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaldiplomacy")
                        .then(Commands.literal("targets")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .executes(context -> openTargets(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "ambassadorId")
                                        ))))
                        .then(Commands.literal("options")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .executes(context -> openOptions(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "ambassadorId"),
                                                        StringArgumentType.getString(context, "targetCapitalId")
                                                )))))
                        .then(Commands.literal("propose")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .then(Commands.argument("proposalType", StringArgumentType.word())
                                                        .executes(context -> propose(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "ambassadorId"),
                                                                StringArgumentType.getString(context, "targetCapitalId"),
                                                                StringArgumentType.getString(context, "proposalType")
                                                        ))))))
                        .then(Commands.literal("betrothal_source")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .executes(context -> betrothalSource(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "ambassadorId"),
                                                        StringArgumentType.getString(context, "targetCapitalId")
                                                )))))
                        .then(Commands.literal("betrothal_target")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .then(Commands.argument("sourceRoyalId", StringArgumentType.word())
                                                        .executes(context -> betrothalTarget(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "ambassadorId"),
                                                                StringArgumentType.getString(context, "targetCapitalId"),
                                                                StringArgumentType.getString(context, "sourceRoyalId")
                                                        ))))))
                        .then(Commands.literal("betrothal_settlement")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .then(Commands.argument("sourceRoyalId", StringArgumentType.word())
                                                        .then(Commands.argument("targetRoyalId", StringArgumentType.word())
                                                                .executes(context -> betrothalSettlement(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "ambassadorId"),
                                                                        StringArgumentType.getString(context, "targetCapitalId"),
                                                                        StringArgumentType.getString(context, "sourceRoyalId"),
                                                                        StringArgumentType.getString(context, "targetRoyalId")
                                                                )))))))
                        .then(Commands.literal("betrothal_send")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .then(Commands.argument("sourceRoyalId", StringArgumentType.word())
                                                        .then(Commands.argument("targetRoyalId", StringArgumentType.word())
                                                                .then(Commands.argument("destinationCapitalId", StringArgumentType.word())
                                                                        .executes(context -> betrothalSend(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "ambassadorId"),
                                                                                StringArgumentType.getString(context, "targetCapitalId"),
                                                                                StringArgumentType.getString(context, "sourceRoyalId"),
                                                                                StringArgumentType.getString(context, "targetRoyalId"),
                                                                                StringArgumentType.getString(context, "destinationCapitalId")
                                                                        ))))))))
                        .then(Commands.literal("endtrade")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .executes(context -> endTrade(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "ambassadorId"),
                                                        StringArgumentType.getString(context, "targetCapitalId")
                                                )))))
                        .then(Commands.literal("war")
                                .then(Commands.argument("ambassadorId", StringArgumentType.word())
                                        .then(Commands.argument("targetCapitalId", StringArgumentType.word())
                                                .executes(context -> declareWar(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "ambassadorId"),
                                                        StringArgumentType.getString(context, "targetCapitalId")
                                                )))))
        );
    }

    private static int openTargets(CommandSourceStack source, String rawAmbassadorId) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, rawAmbassadorId, "mcacapitals.system.command_validation.invalid_ambassador_id");
        if (player == null || ambassadorId == null) {
            return 0;
        }
        Entity ambassador = player.serverLevel().getEntity(ambassadorId);
        if (ambassador == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_diplomatic_agreement_commands.the_ambassador_is_unavailable"));
            return 0;
        }
        return CapitalDiplomaticAgreementService.openCapitalListDirect(player, ambassador) ? 1 : 0;
    }

    private static int openOptions(CommandSourceStack source, String ambassador, String target) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        return player == null || ambassadorId == null || targetId == null
                ? 0
                : CapitalDiplomaticAgreementService.openActionList(player, ambassadorId, targetId);
    }

    private static int propose(CommandSourceStack source, String ambassador, String target, String rawType) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        DiplomaticProposalType type = DiplomaticProposalType.fromSerializedName(rawType);
        if (player == null || ambassadorId == null || targetId == null) {
            return 0;
        }
        if (type == null) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_diplomatic_agreement_commands.that_diplomatic_proposal_type_is_invalid"));
            return 0;
        }
        return CapitalDiplomaticAgreementService.propose(player, ambassadorId, targetId, type);
    }

    private static int betrothalSource(CommandSourceStack source, String ambassador, String target) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        return player == null || ambassadorId == null || targetId == null
                ? 0
                : CapitalDiplomaticAgreementService.openBetrothalSourceSelection(player, ambassadorId, targetId);
    }

    private static int betrothalTarget(
            CommandSourceStack source,
            String ambassador,
            String target,
            String sourceRoyal
    ) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        UUID sourceRoyalId = parseUuid(source, sourceRoyal, "mcacapitals.system.command_validation.invalid_chosen_royal_id");
        return player == null || ambassadorId == null || targetId == null || sourceRoyalId == null
                ? 0
                : CapitalDiplomaticAgreementService.openBetrothalTargetSelection(
                player,
                ambassadorId,
                targetId,
                sourceRoyalId
        );
    }

    private static int betrothalSettlement(
            CommandSourceStack source,
            String ambassador,
            String target,
            String sourceRoyal,
            String targetRoyal
    ) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        UUID sourceRoyalId = parseUuid(source, sourceRoyal, "mcacapitals.system.command_validation.invalid_chosen_royal_id");
        UUID targetRoyalId = parseUuid(source, targetRoyal, "mcacapitals.system.command_validation.invalid_foreign_royal_id");
        return player == null || ambassadorId == null || targetId == null
                || sourceRoyalId == null || targetRoyalId == null
                ? 0
                : CapitalDiplomaticAgreementService.openBetrothalSettlementSelection(
                player,
                ambassadorId,
                targetId,
                sourceRoyalId,
                targetRoyalId
        );
    }

    private static int betrothalSend(
            CommandSourceStack source,
            String ambassador,
            String target,
            String sourceRoyal,
            String targetRoyal,
            String destination
    ) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        UUID sourceRoyalId = parseUuid(source, sourceRoyal, "mcacapitals.system.command_validation.invalid_chosen_royal_id");
        UUID targetRoyalId = parseUuid(source, targetRoyal, "mcacapitals.system.command_validation.invalid_foreign_royal_id");
        UUID destinationId = parseUuid(source, destination, "mcacapitals.system.command_validation.invalid_settlement_capital_id");
        return player == null || ambassadorId == null || targetId == null
                || sourceRoyalId == null || targetRoyalId == null || destinationId == null
                ? 0
                : CapitalDiplomaticAgreementService.proposeSelectedBetrothal(
                player,
                ambassadorId,
                targetId,
                sourceRoyalId,
                targetRoyalId,
                destinationId
        );
    }

    private static int endTrade(CommandSourceStack source, String ambassador, String target) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        return player == null || ambassadorId == null || targetId == null
                ? 0
                : CapitalDiplomaticAgreementService.endTradeAgreement(player, ambassadorId, targetId);
    }

    private static int declareWar(CommandSourceStack source, String ambassador, String target) {
        ServerPlayer player = getPlayer(source);
        UUID ambassadorId = parseUuid(source, ambassador, "mcacapitals.system.command_validation.invalid_ambassador_id");
        UUID targetId = parseUuid(source, target, "mcacapitals.system.command_validation.invalid_target_capital_id");
        return player == null || ambassadorId == null || targetId == null
                ? 0
                : CapitalDiplomaticAgreementService.declareWar(player, ambassadorId, targetId);
    }

    private static UUID parseUuid(CommandSourceStack source, String rawValue, String failureMessage) {
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(Component.translatable(failureMessage));
            return null;
        }
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_diplomatic_agreement_commands.only_a_player_may_conduct_formal_diplomacy"));
            return null;
        }
    }
}