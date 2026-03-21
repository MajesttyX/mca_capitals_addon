package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
                                    ServerLevel level = source.getLevel();

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
                                                        + " heirMode=" + describeHeirMode(level, capital)
                                                        + " royalChildren=" + capital.getRoyalChildren().size()
                                                        + " disinherited=" + capital.getDisinheritedRoyalChildren().size()
                                                        + " legitimized=" + capital.getLegitimizedRoyalChildren().size()
                                                        + " dukes=" + capital.getDukes().size()
                                                        + " lords=" + capital.getLords().size()
                                                        + " knights=" + capital.getKnights().size()
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Royal Succession Order: " + formatUuidList(capital.getRoyalSuccessionOrder())
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Disinherited Royals: " + formatUuidSet(capital.getDisinheritedRoyalChildren())
                                        ), false);

                                        source.sendSuccess(() -> Component.literal(
                                                "Legitimized Royals: " + formatUuidSet(capital.getLegitimizedRoyalChildren())
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

    private static String formatUuidList(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        return ids.toString();
    }

    private static String formatUuidSet(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        return ids.toString();
    }

    private static String describeHeirMode(ServerLevel level, CapitalRecord capital) {
        UUID heir = capital.getHeir();
        if (heir == null) {
            return "none";
        }

        UUID expectedDynasticHeir = firstDynasticHeir(level, capital);
        if (expectedDynasticHeir == null) {
            return "manual";
        }

        if (heir.equals(expectedDynasticHeir)) {
            return "dynastic";
        }

        return "manual";
    }

    private static UUID firstDynasticHeir(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return null;
        }

        Set<UUID> residents = com.example.mcacapitals.capital.CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(sovereign) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.getRoyalChildren().contains(childId)) {
                continue;
            }
            if (residents.contains(childId) && MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(sovereign) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.getRoyalChildren().contains(childId)) {
                continue;
            }
            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        return null;
    }
}