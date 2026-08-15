package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalCrownJusticeService;
import com.majesttyx.mcacapitals.data.CapitalJudgmentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.UUID;

public final class CapitalJusticeCommands {

    private CapitalJusticeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaljustice")
                        .then(Commands.literal("review")
                                .then(Commands.argument("masterOfLawsId", StringArgumentType.word())
                                        .executes(context -> review(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "masterOfLawsId")
                                        ))))
                        .then(Commands.literal("options")
                                .then(Commands.argument("masterOfLawsId", StringArgumentType.word())
                                        .then(Commands.argument("targetId", StringArgumentType.word())
                                                .executes(context -> options(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "masterOfLawsId"),
                                                        StringArgumentType.getString(context, "targetId")
                                                )))))
                        .then(Commands.literal("judge")
                                .then(Commands.argument("masterOfLawsId", StringArgumentType.word())
                                        .then(Commands.argument("targetId", StringArgumentType.word())
                                                .then(Commands.argument("judgment", StringArgumentType.word())
                                                        .executes(context -> judge(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "masterOfLawsId"),
                                                                StringArgumentType.getString(context, "targetId"),
                                                                StringArgumentType.getString(context, "judgment")
                                                        ))))))
                        .then(Commands.literal("recognize")
                                .then(Commands.argument("masterOfLawsId", StringArgumentType.word())
                                        .then(Commands.argument("targetId", StringArgumentType.word())
                                                .executes(context -> recognize(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "masterOfLawsId"),
                                                        StringArgumentType.getString(context, "targetId")
                                                )))))
                        .then(Commands.literal("restore")
                                .then(Commands.argument("masterOfLawsId", StringArgumentType.word())
                                        .then(Commands.argument("targetId", StringArgumentType.word())
                                                .executes(context -> restore(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "masterOfLawsId"),
                                                        StringArgumentType.getString(context, "targetId")
                                                )))))
        );
    }

    private static int review(CommandSourceStack source, String rawMasterOfLawsId) {
        ServerPlayer player = getPlayer(source);
        UUID masterOfLawsId = parseUuid(source, rawMasterOfLawsId, "mcacapitals.system.command_validation.invalid_master_of_laws_id");
        return player == null || masterOfLawsId == null
                ? 0
                : CapitalCrownJusticeService.openReview(player, masterOfLawsId);
    }

    private static int options(CommandSourceStack source, String rawMasterOfLawsId, String rawTargetId) {
        ServerPlayer player = getPlayer(source);
        UUID masterOfLawsId = parseUuid(source, rawMasterOfLawsId, "mcacapitals.system.command_validation.invalid_master_of_laws_id");
        UUID targetId = parseUuid(source, rawTargetId, "mcacapitals.system.command_validation.invalid_prisoner_id");
        return player == null || masterOfLawsId == null || targetId == null
                ? 0
                : CapitalCrownJusticeService.openJudgmentOptions(player, masterOfLawsId, targetId);
    }

    private static int judge(CommandSourceStack source, String rawMasterOfLawsId, String rawTargetId, String rawJudgment) {
        ServerPlayer player = getPlayer(source);
        UUID masterOfLawsId = parseUuid(source, rawMasterOfLawsId, "mcacapitals.system.command_validation.invalid_master_of_laws_id");
        UUID targetId = parseUuid(source, rawTargetId, "mcacapitals.system.command_validation.invalid_prisoner_id");
        CapitalJudgmentType judgment = parseJudgment(source, rawJudgment);
        return player == null || masterOfLawsId == null || targetId == null || judgment == null
                ? 0
                : CapitalCrownJusticeService.decide(player, masterOfLawsId, targetId, judgment);
    }

    private static int recognize(CommandSourceStack source, String rawMasterOfLawsId, String rawTargetId) {
        ServerPlayer player = getPlayer(source);
        UUID masterOfLawsId = parseUuid(source, rawMasterOfLawsId, "mcacapitals.system.command_validation.invalid_master_of_laws_id");
        UUID targetId = parseUuid(source, rawTargetId, "mcacapitals.system.command_validation.invalid_villager_id");
        return player == null || masterOfLawsId == null || targetId == null
                ? 0
                : CapitalCrownJusticeService.recognizeFriend(player, masterOfLawsId, targetId) ? 1 : 0;
    }

    private static int restore(CommandSourceStack source, String rawMasterOfLawsId, String rawTargetId) {
        ServerPlayer player = getPlayer(source);
        UUID masterOfLawsId = parseUuid(source, rawMasterOfLawsId, "mcacapitals.system.command_validation.invalid_master_of_laws_id");
        UUID targetId = parseUuid(source, rawTargetId, "mcacapitals.system.command_validation.invalid_villager_id");
        return player == null || masterOfLawsId == null || targetId == null
                ? 0
                : CapitalCrownJusticeService.restoreToPeace(player, masterOfLawsId, targetId) ? 1 : 0;
    }

    private static CapitalJudgmentType parseJudgment(CommandSourceStack source, String rawJudgment) {
        try {
            return CapitalJudgmentType.valueOf(rawJudgment.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(Component.translatable("mcacapitals.system.capital_justice_commands.the_judgment_must_be_pardon_imprisonment_exile_or_execution"));
            return null;
        }
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
            source.sendFailure(Component.translatable("mcacapitals.system.capital_justice_commands.only_a_player_may_use_crown_justice_decisions"));
            return null;
        }
    }
}