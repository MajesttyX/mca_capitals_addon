package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CapitalNameService {

    private static final String[] KNOWN_TITLES = new String[] {
            "Deposed Queen",
            "Deposed King",
            "Late Queen",
            "Late King",
            "Regent",
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
            "Ambassador",
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

        UUID ambassador = CapitalAmbassadorService.getAmbassador(level, capital);
        if (ambassador != null) {
            allRelevant.add(ambassador);
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

            String baseName = resolveBaseNameFromEntity(entity);
            Component finalName = Component.literal(baseName);
            Component currentComponent = entity.getCustomName();

            if (currentComponent == null || !currentComponent.equals(finalName)) {
                entity.setCustomName(finalName);
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

        return resolveBaseNameFromEntity(entity);
    }

    public static Component resolveDisplayNameComponent(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || entityId == null) {
            return Component.translatable("mcacapitals.system.common.unknown");
        }

        Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, entityId);
        if (entity == null) {
            if (capital != null && capital.getVillageId() != null) {
                String savedName = MCAIntegrationBridge.getVillageResidentNames(level, capital.getVillageId()).get(entityId);
                if (savedName != null && !savedName.isBlank()) {
                    return Component.literal(normalizeBaseName(savedName));
                }
            }
            return Component.literal(entityId.toString());
        }

        String baseName = resolveBaseNameFromEntity(entity);
        if (baseName.isBlank()) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }

        return Component.literal(baseName);
    }

    private static String resolveBaseNameFromEntity(Entity entity) {
        if (entity == null) {
            return "Unnamed";
        }

        Component customName = entity.getCustomName();
        String recovered = recoverBaseNameFromLegacyTranslatedName(customName);
        if (recovered != null && !recovered.isBlank()) {
            return normalizeBaseName(recovered);
        }

        String currentName = customName != null
                ? customName.getString()
                : entity.getName().getString();

        return normalizeBaseName(currentName);
    }

    private static String recoverBaseNameFromLegacyTranslatedName(Component component) {
        if (component == null
                || !(component.getContents() instanceof TranslatableContents contents)) {
            return null;
        }

        String key = contents.getKey();
        if (!"mcacapitals.dynamic.name.titled".equals(key)
                && !"mcacapitals.dynamic.name.royal_guard.kingsguard".equals(key)
                && !"mcacapitals.dynamic.name.royal_guard.queensguard".equals(key)) {
            return null;
        }

        Object[] args = contents.getArgs();
        if (args == null || args.length < 2) {
            return null;
        }

        Object baseNameArg = args[1];
        if (baseNameArg instanceof Component baseNameComponent) {
            return baseNameComponent.getString();
        }

        return baseNameArg == null
                ? null
                : String.valueOf(baseNameArg);
    }

    static String normalizeBaseName(String name) {
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
