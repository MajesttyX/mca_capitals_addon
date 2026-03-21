package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AbdicationPromptCommands {

    private AbdicationPromptCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalabdication")
                        .then(Commands.literal("confirm")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        return 0;
                                    }

                                    CapitalRecord capital = CapitalManager.getCapitalBySovereign(player.getUUID());
                                    if (capital == null) {
                                        player.sendSystemMessage(Component.literal("You are not the sovereign of a capital."));
                                        return 0;
                                    }

                                    boolean changed = CapitalFoundationService.abdicateSovereign(player.serverLevel(), capital);
                                    if (!changed) {
                                        player.sendSystemMessage(Component.literal("There is no valid successor to receive the throne."));
                                        return 0;
                                    }

                                    player.sendSystemMessage(Component.literal("By solemn declaration, you have abdicated the throne."));
                                    return 1;
                                }))
        );
    }
}