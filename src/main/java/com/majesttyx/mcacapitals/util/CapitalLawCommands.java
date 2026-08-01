package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.dialogue.CapitalJusticeService;
import com.majesttyx.mcacapitals.item.SealedPurseHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalLawCommands {

    private CapitalLawCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitallaw")
                        .then(Commands.literal("accuse")
                                .then(Commands.argument("capitalId", StringArgumentType.word())
                                        .then(Commands.argument("targetId", StringArgumentType.word())
                                                .executes(ctx -> accuse(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "capitalId"),
                                                        StringArgumentType.getString(ctx, "targetId")
                                                )))))
                        .then(Commands.literal("sealed_purse")
                                .then(Commands.argument("capitalId", StringArgumentType.word())
                                        .then(Commands.argument("targetId", StringArgumentType.word())
                                                .executes(ctx -> sealedPurse(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "capitalId"),
                                                        StringArgumentType.getString(ctx, "targetId")
                                                )))))
        );
    }

    private static int accuse(CommandSourceStack source, String rawCapitalId, String rawTargetId) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Only a player may make an accusation before the Master of Laws."));
            return 0;
        }

        UUID capitalId;
        UUID targetId;
        try {
            capitalId = UUID.fromString(rawCapitalId);
            targetId = UUID.fromString(rawTargetId);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("That accusation carried an invalid seal."));
            return 0;
        }

        return CapitalJusticeService.handleAccusation(player, capitalId, targetId) ? 1 : 0;
    }

    private static int sealedPurse(CommandSourceStack source, String rawCapitalId, String rawTargetId) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Only a player may gift a Sealed Purse."));
            return 0;
        }

        UUID capitalId;
        UUID targetId;
        try {
            capitalId = UUID.fromString(rawCapitalId);
            targetId = UUID.fromString(rawTargetId);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("That Sealed Purse request carried an invalid seal."));
            return 0;
        }

        return SealedPurseHandler.handleSelectedCase(player, capitalId, targetId) ? 1 : 0;
    }
}