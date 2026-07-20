package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
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
            "Master of Laws",
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
        if (capital.getMasterOfLaws() != null) {
            allRelevant.add(capital.getMasterOfLaws());
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

    public static String resolveDisplayName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || entityId == null) {
            return "Unknown";
        }

        Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, entityId);
        if (entity == null) {
            if (capital != null && capital.getVillageId() != null) {
                String savedName = MCAIntegrationBridge.getVillageResidentNames(level, capital.getVillageId()).get(entityId);
                if (savedName != null && !savedName.isBlank()) {
                    return normalizeBaseName(savedName);
                }
            }
            return entityId.toString();
        }

        String currentName = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getName().getString();

        return normalizeBaseName(currentName);
    }

    private static String buildDisplayName(ServerLevel level, UUID entityId, String baseName) {
        String title = CapitalTitleResolver.getDisplayTitleForEntity(level, entityId);

        if (title == null || title.isBlank() || "Commoner".equals(title) || "None".equals(title)) {
            return baseName;
        }

        CapitalRecord royalGuardCapital = findRoyalGuardCapital(entityId);
        if (royalGuardCapital != null && ("Sir".equals(title) || "Dame".equals(title))) {
            return CapitalRoyalGuardService.buildRoyalGuardDisplayName(level, royalGuardCapital, entityId);
        }

        return title + " " + baseName;
    }

    private static CapitalRecord findRoyalGuardCapital(UUID entityId) {
        if (entityId == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.isRoyalGuard(entityId)) {
                return capital;
            }
        }

        return null;
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