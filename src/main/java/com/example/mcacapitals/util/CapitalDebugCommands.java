package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class CapitalDebugCommands {

    private CapitalDebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitaldebug")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();

                                    if (CapitalManager.getAllCapitals().isEmpty()) {
                                        source.sendSuccess(() -> Component.literal("No capitals found."), false);
                                        return 1;
                                    }

                                    for (CapitalRecord capital : CapitalManager.getAllCapitals().values()) {
                                        source.sendSuccess(() -> Component.literal(
                                                "Capital " + capital.getCapitalId()
                                                        + " villageId=" + capital.getVillageId()
                                                        + " state=" + capital.getState()
                                                        + " sovereign=" + capital.getSovereign()
                                                        + " consort=" + capital.getConsort()
                                                        + " heir=" + capital.getHeir()
                                                        + " royalChildren=" + capital.getRoyalChildren().size()
                                                        + " dukes=" + capital.getDukes().size()
                                                        + " lords=" + capital.getLords().size()
                                                        + " knights=" + capital.getKnights().size()
                                        ), false);
                                    }

                                    return 1;
                                }))

                        .then(Commands.literal("villages")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();

                                    var ids = MCAIntegrationBridge.getAllVillageIds(level);
                                    if (ids.isEmpty()) {
                                        source.sendSuccess(() -> Component.literal("MCA villages: none"), false);
                                        return 1;
                                    }

                                    for (int villageId : ids.stream().sorted().toList()) {
                                        boolean isVillage = MCAIntegrationBridge.isVillage(level, villageId);
                                        int population = MCAIntegrationBridge.getVillagePopulation(level, villageId);
                                        int residents = MCAIntegrationBridge.getVillageResidents(level, villageId).size();

                                        source.sendSuccess(() -> Component.literal(
                                                "MCA villageId=" + villageId
                                                        + " isVillage=" + isVillage
                                                        + " population=" + population
                                                        + " residents=" + residents
                                                        + " hasCapital=" + CapitalManager.hasCapitalForVillageId(villageId)
                                        ), false);
                                    }

                                    return 1;
                                }))
        );
    }
}