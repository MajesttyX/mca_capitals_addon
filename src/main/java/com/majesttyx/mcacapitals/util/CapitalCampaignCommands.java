package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalCampaignService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarGoal;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalCampaignCommands {

    private CapitalCampaignCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalcampaign")
                        .then(
                                Commands.literal("launch")
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
                                                                                                "warGoal",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(context -> launch(
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
                                                                                                        "warGoal"
                                                                                                )
                                                                                        ))
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("clearcooldown")
                                        .requires(source ->
                                                source.hasPermission(2)
                                        )
                                        .then(
                                                Commands.literal("all")
                                                        .executes(context ->
                                                                clearAllCooldowns(
                                                                        context.getSource()
                                                                )
                                                        )
                                        )
                        )
        );
    }

    private static int launch(
            CommandSourceStack source,
            String rawAmbassadorId,
            String rawTargetCapitalId,
            String rawWarGoal
    ) {
        ServerPlayer player =
                getPlayer(source);

        UUID ambassadorId = parseUuid(
                source,
                rawAmbassadorId,
                "mcacapitals.system.command_validation.invalid_ambassador_id"
        );

        UUID targetCapitalId = parseUuid(
                source,
                rawTargetCapitalId,
                "mcacapitals.system.command_validation.invalid_target_capital_id"
        );

        CapitalWarGoal warGoal = parseWarGoal(
                source,
                rawWarGoal
        );

        if (player == null
                || ambassadorId == null
                || targetCapitalId == null
                || warGoal == null) {
            return 0;
        }

        return CapitalCampaignService
                .launchCampaign(
                        player,
                        ambassadorId,
                        targetCapitalId,
                        warGoal
                );
    }

    private static int clearAllCooldowns(
            CommandSourceStack source
    ) {
        long currentDay =
                CapitalWarDataAccess.currentDay(
                        source.getLevel()
                );

        int cleared = 0;

        for (CapitalRecord capital :
                CapitalManager.getAllCapitalRecords()) {
            if (capital == null
                    || capital.getCapitalId() == null
                    || CapitalWarDataAccess
                    .getCampaignAvailableDay(
                            source.getLevel(),
                            capital.getCapitalId()
                    ) <= currentDay) {
                continue;
            }

            CapitalWarDataAccess.setCampaignRecovery(
                    source.getLevel(),
                    capital.getCapitalId(),
                    0L
            );

            cleared++;
        }

        int result = cleared;

        source.sendSuccess(
                () -> Component.literal(
                        "Cleared campaign recovery cooldowns for "
                                + result
                                + (result == 1
                                ? " capital."
                                : " capitals.")
                ),
                true
        );

        return 1;
    }

    private static CapitalWarGoal parseWarGoal(
            CommandSourceStack source,
            String rawValue
    ) {
        if (rawValue == null) {
            return null;
        }

        for (CapitalWarGoal goal : CapitalWarGoal.values()) {
            if (goal.getSerializedName().equalsIgnoreCase(rawValue)
                    || goal.name().equalsIgnoreCase(rawValue)) {
                return goal;
            }
        }

        source.sendFailure(
                Component.translatable("mcacapitals.system.capital_campaign_commands.the_war_goal_must_be_punitive_or_deposition")
        );
        return null;
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
                    Component.translatable(failureMessage)
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
                    Component.translatable("mcacapitals.system.capital_campaign_commands.only_a_player_sovereign_may_launch_a_military_campaign")
            );

            return null;
        }
    }
}