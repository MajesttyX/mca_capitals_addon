package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalRoyalGuardService;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CapitalRoyalGuardCommands {

    private CapitalRoyalGuardCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalguard")
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.only_a_player_can_use_this"));
                                        return 0;
                                    }

                                    CapitalRecord capital = CapitalManager.getCapitalBySovereign(player.getUUID());

                                    if (capital == null) {
                                        ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.you_are_not_the_sovereign_of_a_capital"));
                                        return 0;
                                    }

                                    Set<UUID> residents = CapitalResidentScanner.scanResidents(player.serverLevel(), capital.getCapitalId());
                                    List<UUID> candidates = CapitalRoyalGuardService.getValidCandidates(player.serverLevel(), capital, residents);
                                    if (candidates.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.no_valid_royal_guard_candidates_were_found"));
                                        return 0;
                                    }

                                    for (UUID candidate : candidates) {
                                        ctx.getSource().sendSuccess(
                                                () -> Component.translatable(
                                                        "mcacapitals.system.capital_royal_guard_commands.candidate_line",
                                                        CapitalRoyalGuardService.buildRoyalGuardDisplayNameComponent(
                                                                player.serverLevel(),
                                                                capital,
                                                                candidate
                                                        ),
                                                        candidate.toString(),
                                                        MCAIntegrationBridge.describeProfession(
                                                                player.serverLevel(),
                                                                candidate
                                                        )
                                                ),
                                                false
                                        );
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("appoint")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.only_a_player_can_use_this"));
                                                return 0;
                                            }

                                            CapitalRecord capital = CapitalManager.getCapitalBySovereign(player.getUUID());

                                            if (capital == null) {
                                                ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.you_are_not_the_sovereign_of_a_capital"));
                                                return 0;
                                            }

                                            UUID villagerId;
                                            try {
                                                villagerId = UUID.fromString(StringArgumentType.getString(ctx, "villagerId"));
                                            } catch (IllegalArgumentException ex) {
                                                ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.invalid_uuid"));
                                                return 0;
                                            }

                                            Set<UUID> residents = CapitalResidentScanner.scanResidents(player.serverLevel(), capital.getCapitalId());
                                            if (!residents.contains(villagerId)) {
                                                ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.that_villager_is_not_a_resident_of_your_capital"));
                                                return 0;
                                            }

                                            if (!CapitalRoyalGuardService.appointRoyalGuard(player.serverLevel(), capital, villagerId)) {
                                                ctx.getSource().sendFailure(Component.translatable("mcacapitals.system.capital_royal_guard_commands.that_villager_is_not_eligible_to_be_named_to_the_royal_guard"));
                                                return 0;
                                            }

                                            CapitalDataAccess.markDirty(player.serverLevel());
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable(
                                                            "mcacapitals.system.capital_royal_guard_commands.appointed",
                                                            CapitalRoyalGuardService.buildRoyalGuardDisplayNameComponent(
                                                                    player.serverLevel(),
                                                                    capital,
                                                                    villagerId
                                                            )
                                                    ),
                                                    false
                                            );
                                            return 1;
                                        })))
        );
    }
}