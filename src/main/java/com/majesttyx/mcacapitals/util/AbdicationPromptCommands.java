package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
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
                                        player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_commands.you_are_not_the_sovereign_of_a_capital"));
                                        return 0;
                                    }

                                    boolean changed = CapitalFoundationService.abdicateSovereign(player.serverLevel(), capital);
                                    if (!changed) {
                                        player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_commands.there_is_no_valid_successor_to_receive_the_throne"));
                                        return 0;
                                    }

                                    player.sendSystemMessage(Component.translatable("mcacapitals.system.abdication_prompt_commands.by_solemn_declaration_you_have_abdicated_the_throne"));
                                    return 1;
                                }))
        );
    }
}