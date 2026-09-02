package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.conczin.mca.entity.VillagerEntityMCA;
import fabric.net.conczin.mca.entity.VillagerLike;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CapitalNameService {

    private static final String TITLED_NAME_KEY =
            "mcacapitals.dynamic.name.titled";
    private static final String KINGSGUARD_NAME_KEY =
            "mcacapitals.dynamic.name.royal_guard.kingsguard";
    private static final String QUEENSGUARD_NAME_KEY =
            "mcacapitals.dynamic.name.royal_guard.queensguard";

    private CapitalNameService() {
    }

    public static void refreshCapitalNames(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null || residents == null) {
            return;
        }

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

            repairLegacyCapitalsName(entity);
            VillagerIdentitySyncService.syncToNearbyPlayers(level, entity);
        }
    }

    public static String resolveDisplayName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || entityId == null) {
            return "";
        }

        Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, entityId);
        if (entity != null) {
            String baseName = resolveBaseName(entity);
            if (isUsableBaseName(baseName)) {
                return baseName.trim();
            }
        }

        if (capital != null && capital.getVillageId() != null) {
            ServerLevel capitalLevel = CapitalManager.resolveCapitalLevel(level, capital);
            String savedName = MCAIntegrationBridge
                    .getVillageResidentNames(capitalLevel, capital.getVillageId())
                    .get(entityId);
            if (isUsableBaseName(savedName)) {
                return savedName.trim();
            }
        }

        return entityId.toString();
    }

    public static Component resolveDisplayNameComponent(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || entityId == null) {
            return Component.translatable("mcacapitals.system.common.unknown");
        }

        Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, entityId);
        if (entity != null) {
            String baseName = resolveBaseName(entity);
            if (isUsableBaseName(baseName)) {
                return Component.literal(baseName.trim());
            }
        }

        if (capital != null && capital.getVillageId() != null) {
            ServerLevel capitalLevel = CapitalManager.resolveCapitalLevel(level, capital);
            String savedName = MCAIntegrationBridge
                    .getVillageResidentNames(capitalLevel, capital.getVillageId())
                    .get(entityId);
            if (isUsableBaseName(savedName)) {
                return Component.literal(savedName.trim());
            }
        }

        return Component.literal(entityId.toString());
    }

    /**
     * Repairs only names that Capitals itself previously stored as one of its
     * structured translatable title wrappers. Arbitrary/custom MCA names are
     * deliberately left untouched.
     */
    public static boolean repairLegacyCapitalsName(Entity entity) {
        if (!(entity instanceof VillagerEntityMCA villager)) {
            return false;
        }

        Component customName = villager.getCustomName();
        String recoveredName = recoverBaseNameFromCapitalsWrapper(customName);
        if (!isUsableBaseName(recoveredName)) {
            return false;
        }

        String baseName = recoveredName.trim();
        String canonicalName = villager.getTrackedValue(VillagerLike.VILLAGER_NAME);
        if (!baseName.equals(canonicalName)) {
            villager.setName(baseName);
        }

        // Replace only a known legacy Capitals wrapper with the recovered base
        // name. VillagerEntityMCA#setCustomName(non-null) also writes MCA's
        // canonical citizen name, but at this point that value is deliberately
        // the same literal base name. This keeps vanilla/MCA getName() readers
        // correct while Capitals titles remain presentation-only Components.
        villager.setCustomName(Component.literal(baseName));
        return true;
    }

    private static String resolveBaseName(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof VillagerEntityMCA villager) {
            repairLegacyCapitalsName(villager);

            String canonicalName = villager.getTrackedValue(VillagerLike.VILLAGER_NAME);
            if (isUsableBaseName(canonicalName)) {
                return canonicalName.trim();
            }
        }

        Component customName = entity.getCustomName();
        String recoveredName = recoverBaseNameFromCapitalsWrapper(customName);
        if (isUsableBaseName(recoveredName)) {
            return recoveredName.trim();
        }

        if (customName != null) {
            String currentName = customName.getString();
            if (isUsableBaseName(currentName)) {
                return currentName.trim();
            }
        }

        String currentName = entity.getName().getString();
        return isUsableBaseName(currentName)
                ? currentName.trim()
                : null;
    }

    private static String recoverBaseNameFromCapitalsWrapper(Component component) {
        if (component == null
                || !(component.getContents() instanceof TranslatableContents contents)
                || !isCapitalsNameWrapper(contents.getKey())) {
            return null;
        }

        Object[] args = contents.getArgs();
        if (args == null || args.length < 2) {
            return null;
        }

        return extractBaseNameArgument(args[1]);
    }

    private static String extractBaseNameArgument(Object argument) {
        if (argument instanceof Component component) {
            String nestedName = recoverBaseNameFromCapitalsWrapper(component);
            if (isUsableBaseName(nestedName)) {
                return nestedName.trim();
            }

            String value = component.getString();
            return isUsableBaseName(value)
                    ? value.trim()
                    : null;
        }

        if (argument == null) {
            return null;
        }

        String value = String.valueOf(argument);
        return isUsableBaseName(value)
                ? value.trim()
                : null;
    }

    private static boolean isCapitalsNameWrapper(String key) {
        return TITLED_NAME_KEY.equals(key)
                || KINGSGUARD_NAME_KEY.equals(key)
                || QUEENSGUARD_NAME_KEY.equals(key);
    }

    private static boolean isUsableBaseName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        String value = name.trim();
        return !TITLED_NAME_KEY.equals(value)
                && !KINGSGUARD_NAME_KEY.equals(value)
                && !QUEENSGUARD_NAME_KEY.equals(value);
    }

    static String normalizeBaseName(String name) {
        if (!isUsableBaseName(name)) {
            return "";
        }

        return name.trim();
    }
}
