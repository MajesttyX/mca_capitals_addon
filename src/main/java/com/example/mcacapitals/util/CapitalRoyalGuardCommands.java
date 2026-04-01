package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.data.CapitalDataAccess;
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
                                        ctx.getSource().sendFailure(Component.literal("Only a player can use this."));
                                        return 0;
                                    }

                                    CapitalRecord capital = CapitalManager.getCapitalBySovereign(player.getUUID());

                                    if (capital == null) {
                                        ctx.getSource().sendFailure(Component.literal("You are not the sovereign of a capital."));
                                        return 0;
                                    }

                                    Set<UUID> residents = CapitalResidentScanner.scanResidents(player.serverLevel(), capital.getCapitalId());
                                    List<UUID> candidates = CapitalRoyalGuardService.getValidCandidates(player.serverLevel(), capital, residents);
                                    if (candidates.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("No valid royal guard candidates were found."));
                                        return 0;
                                    }

                                    for (UUID candidate : candidates) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "- " + CapitalRoyalGuardService.buildRoyalGuardDisplayName(player.serverLevel(), capital, candidate)
                                                        + " [" + candidate + "] profession="
                                                        + MCAIntegrationBridge.describeProfession(player.serverLevel(), candidate)
                                        ), false);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("appoint")
                                .then(Commands.argument("villagerId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                ctx.getSource().sendFailure(Component.literal("Only a player can use this."));
                                                return 0;
                                            }

                                            CapitalRecord capital = CapitalManager.getCapitalBySovereign(player.getUUID());

                                            if (capital == null) {
                                                ctx.getSource().sendFailure(Component.literal("You are not the sovereign of a capital."));
                                                return 0;
                                            }

                                            UUID villagerId;
                                            try {
                                                villagerId = UUID.fromString(StringArgumentType.getString(ctx, "villagerId"));
                                            } catch (IllegalArgumentException ex) {
                                                ctx.getSource().sendFailure(Component.literal("Invalid UUID."));
                                                return 0;
                                            }

                                            Set<UUID> residents = CapitalResidentScanner.scanResidents(player.serverLevel(), capital.getCapitalId());
                                            if (!residents.contains(villagerId)) {
                                                ctx.getSource().sendFailure(Component.literal("That villager is not a resident of your capital."));
                                                return 0;
                                            }

                                            if (!CapitalRoyalGuardService.appointRoyalGuard(player.serverLevel(), capital, villagerId)) {
                                                ctx.getSource().sendFailure(Component.literal("That villager is not eligible to be named to the royal guard."));
                                                return 0;
                                            }

                                            CapitalDataAccess.markDirty(player.serverLevel());
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    CapitalRoyalGuardService.buildRoyalGuardDisplayName(player.serverLevel(), capital, villagerId)
                                                            + " has been appointed to the royal guard."
                                            ), false);
                                            return 1;
                                        })))
        );
    }
}