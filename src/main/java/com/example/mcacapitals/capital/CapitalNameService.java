package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CapitalNameService {

    private static final String[] KNOWN_TITLES = new String[] {
            "High Queen",
            "High King",
            "Dowager Queen",
            "Dowager King",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Crown Princess",
            "Crown Prince",
            "Dowager Princess",
            "Dowager Prince",
            "Princess Consort",
            "Prince Consort",
            "Hand of the Queen",
            "Hand of the King",
            "Grand Maester",
            "Maester",
            "Court Herald",
            "Princess",
            "Prince",
            "Lord Commander",
            "Dowager Duchess",
            "Dowager Duke",
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
        if (capital.getCommander() != null) {
            allRelevant.add(capital.getCommander());
        }
        if (capital.getHand() != null) {
            allRelevant.add(capital.getHand());
        }
        if (capital.getHerald() != null) {
            allRelevant.add(capital.getHerald());
        }
        if (capital.getGrandMaester() != null) {
            allRelevant.add(capital.getGrandMaester());
        }

        allRelevant.addAll(capital.getRoyalChildren());
        allRelevant.addAll(capital.getPrinceConsortSources().keySet());
        allRelevant.addAll(capital.getDowagerPrinceSources().keySet());
        allRelevant.addAll(capital.getDukes());
        allRelevant.addAll(capital.getMarriageDukeSources().keySet());
        allRelevant.addAll(capital.getDowagerDukeSources().keySet());
        allRelevant.addAll(capital.getLords());
        allRelevant.addAll(capital.getKnights());
        allRelevant.addAll(capital.getRoyalGuards());

        for (UUID entityId : allRelevant) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || !MCAIntegrationBridge.isMCAVillager(level, entityId)) {
                continue;
            }

            String currentName = entity.getCustomName() != null
                    ? entity.getCustomName().getString()
                    : entity.getName().getString();

            String baseName = normalizeBaseName(currentName);
            String finalName = buildDisplayName(level, entityId, baseName);

            if (!currentName.equals(finalName)) {
                entity.setCustomName(Component.literal(finalName));
                entity.setCustomNameVisible(true);
            }
        }
    }

    private static String buildDisplayName(ServerLevel level, UUID entityId, String baseName) {
        CapitalRecord displayCapital = CapitalTitleResolver.findCapitalForEntity(level, entityId);
        String title = CapitalTitleResolver.getDisplayTitleForEntity(level, entityId);

        if (displayCapital != null && displayCapital.isRoyalGuard(entityId)) {
            if ("Sir".equals(title) || "Dame".equals(title)) {
                String honorific = "Dame".equals(title) ? "Dame" : "Sir";
                String guardType = displayCapital.isSovereignFemale() ? "Queensguard" : "Kingsguard";
                return honorific + " " + baseName + " of the " + guardType;
            }
        }

        if (title == null || title.isBlank() || "Commoner".equals(title) || "None".equals(title)) {
            return baseName;
        }

        return title + " " + baseName;
    }

    private static String normalizeBaseName(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();

        if (result.endsWith(" of the Kingsguard")) {
            result = result.substring(0, result.length() - " of the Kingsguard".length()).trim();
        }
        if (result.endsWith(" of the Queensguard")) {
            result = result.substring(0, result.length() - " of the Queensguard".length()).trim();
        }

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