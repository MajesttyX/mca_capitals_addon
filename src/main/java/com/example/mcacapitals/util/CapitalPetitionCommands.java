package com.example.mcacapitals.util;

import com.example.mcacapitals.dialogue.CapitalPetitionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalPetitionCommands {

    private CapitalPetitionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalpetition")
                        .then(Commands.literal("betrothal")
                                .then(Commands.argument("capitalId", StringArgumentType.word())
                                        .then(Commands.argument("villagerId", StringArgumentType.word())
                                                .executes(ctx -> handleBetrothal(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "capitalId"),
                                                        StringArgumentType.getString(ctx, "villagerId")
                                                )))))
                        .then(Commands.literal("recommend_betrothal")
                                .then(Commands.argument("capitalId", StringArgumentType.word())
                                        .then(Commands.argument("firstVillagerId", StringArgumentType.word())
                                                .then(Commands.argument("secondVillagerId", StringArgumentType.word())
                                                        .executes(ctx -> handleRecommendedBetrothal(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "capitalId"),
                                                                StringArgumentType.getString(ctx, "firstVillagerId"),
                                                                StringArgumentType.getString(ctx, "secondVillagerId")
                                                        ))))))
        );
    }

    private static int handleBetrothal(CommandSourceStack source, String rawCapitalId, String rawVillagerId) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Only a player may present a betrothal petition."));
            return 0;
        }

        UUID capitalId;
        UUID villagerId;
        try {
            capitalId = UUID.fromString(rawCapitalId);
            villagerId = UUID.fromString(rawVillagerId);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("That betrothal petition carried an invalid seal."));
            return 0;
        }

        return CapitalPetitionService.handleBetrothalSelection(player, capitalId, villagerId) ? 1 : 0;
    }

    private static int handleRecommendedBetrothal(
            CommandSourceStack source,
            String rawCapitalId,
            String rawFirstVillagerId,
            String rawSecondVillagerId
    ) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Only a player may present a betrothal recommendation."));
            return 0;
        }

        UUID capitalId;
        UUID firstVillagerId;
        UUID secondVillagerId;
        try {
            capitalId = UUID.fromString(rawCapitalId);
            firstVillagerId = UUID.fromString(rawFirstVillagerId);
            secondVillagerId = UUID.fromString(rawSecondVillagerId);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("That betrothal recommendation carried an invalid seal."));
            return 0;
        }

        return CapitalPetitionService.handleRecommendedBetrothalSelection(
                player,
                capitalId,
                firstVillagerId,
                secondVillagerId
        ) ? 1 : 0;
    }
}