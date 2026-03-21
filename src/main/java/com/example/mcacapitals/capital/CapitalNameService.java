package com.example.mcacapitals.capital;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CapitalNameService {

    private static final String[] KNOWN_TITLES = new String[] {
            "Queen Dowager",
            "Prince Consort",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Princess",
            "Prince",
            "Duchess",
            "Duke",
            "Lady",
            "Lord",
            "Dame",
            "Sir",
            "Queen",
            "King"
    };

    private CapitalNameService() {
    }

    public static void refreshCapitalNames(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        Set<UUID> allRelevant = new HashSet<>(residents);

        if (capital.getSovereign() != null) {
            allRelevant.add(capital.getSovereign());
        }
        if (capital.getConsort() != null) {
            allRelevant.add(capital.getConsort());
        }
        if (capital.getDowager() != null) {
            allRelevant.add(capital.getDowager());
        }
        if (capital.getHeir() != null) {
            allRelevant.add(capital.getHeir());
        }

        allRelevant.addAll(capital.getRoyalChildren());
        allRelevant.addAll(capital.getDukes());
        allRelevant.addAll(capital.getLords());
        allRelevant.addAll(capital.getKnights());

        for (UUID entityId : allRelevant) {
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof VillagerEntityMCA villager)) {
                continue;
            }

            String currentName = villager.getCustomName() != null
                    ? villager.getCustomName().getString()
                    : villager.getName().getString();

            String baseName = stripKnownTitles(currentName);
            String title = CapitalTitleResolver.getDisplayTitle(level, capital, entityId);

            String finalName = (title == null || title.isBlank() || "Commoner".equals(title) || "None".equals(title))
                    ? baseName
                    : title + " " + baseName;

            villager.setCustomName(Component.literal(finalName));
            villager.setCustomNameVisible(true);
        }

        for (UUID residentId : residents) {
            if (allRelevant.contains(residentId)) {
                continue;
            }

            Entity entity = level.getEntity(residentId);
            if (!(entity instanceof VillagerEntityMCA villager)) {
                continue;
            }

            String currentName = villager.getCustomName() != null
                    ? villager.getCustomName().getString()
                    : villager.getName().getString();

            String baseName = stripKnownTitles(currentName);
            villager.setCustomName(Component.literal(baseName));
            villager.setCustomNameVisible(true);
        }
    }

    private static String stripKnownTitles(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();
        boolean changed = true;

        while (changed) {
            changed = false;
            for (String title : KNOWN_TITLES) {
                String prefix = title + " ";
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }

        return result.isBlank() ? "Unnamed" : result;
    }
}