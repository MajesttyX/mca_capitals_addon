package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
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

    public static void refreshCapitalNames(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null || capital == null || residents == null) {
            return;
        }

        Set<UUID> allRelevant = new HashSet<>(residents);

        addIfPresent(allRelevant, capital.getSovereign());
        addIfPresent(allRelevant, capital.getConsort());
        addIfPresent(allRelevant, capital.getDowager());
        addIfPresent(allRelevant, capital.getHeir());
        addIfPresent(allRelevant, capital.getCommander());
        addIfPresent(allRelevant, capital.getHand());
        addIfPresent(allRelevant, capital.getHerald());
        addIfPresent(allRelevant, capital.getGrandMaester());
        addIfPresent(allRelevant, capital.getMasterOfLaws());

        UUID ambassador = CapitalAmbassadorService.getAmbassador(level, capital);
        addIfPresent(allRelevant, ambassador);

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
            if (entity == null
                    || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
                continue;
            }

            String baseName = resolveBaseName(entity);
            if (!isUsableBaseName(baseName) && capital.getVillageId() != null) {
                String savedName = MCAIntegrationBridge
                        .getVillageResidentNames(level, capital.getVillageId())
                        .get(entityId);
                if (isUsableBaseName(savedName)) {
                    baseName = savedName.trim();
                }
            }

            if (!isUsableBaseName(baseName)) {
                continue;
            }

            Component plainName = Component.literal(baseName.trim());
            Component currentCustomName = entity.getCustomName();

            if (!plainName.equals(currentCustomName)) {
                /*
                 * MCA Reborn mirrors setCustomName(...).getString()
                 * into its canonical tracked/family-tree name. Therefore a
                 * translated title wrapper must never be stored here on the
                 * server. Titles and offices are synced/rendered separately.
                 */
                entity.setCustomName(plainName);
                entity.setCustomNameVisible(true);
            }
        }
    }

    public static String resolveDisplayName(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
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
            String savedName = MCAIntegrationBridge
                    .getVillageResidentNames(level, capital.getVillageId())
                    .get(entityId);
            if (isUsableBaseName(savedName)) {
                return savedName.trim();
            }
        }

        return entityId.toString();
    }

    public static Component resolveDisplayNameComponent(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (level == null || entityId == null) {
            return Component.translatable("mcacapitals.system.common.unknown");
        }

        String baseName = resolveDisplayName(level, capital, entityId);
        if (baseName.isBlank()) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }

        return Component.literal(baseName);
    }

    /**
     * Repairs only legacy name wrappers previously written by Capitals.
     * Normal MCA/custom villager names are left untouched.
     */
    public static boolean repairLegacyCapitalsName(Entity entity) {
        if (entity == null) {
            return false;
        }

        Component customName = entity.getCustomName();
        String recoveredName = recoverBaseNameFromCapitalsWrapper(customName);
        if (!isUsableBaseName(recoveredName)) {
            return false;
        }

        String baseName = recoveredName.trim();
        Component plainName = Component.literal(baseName);
        if (!plainName.equals(customName)) {
            // MCA mirrors a server-side custom-name update into its canonical
            // citizen name, so writing the literal base name also repairs the
            // old Capitals title wrapper without hard-coding a title.
            entity.setCustomName(plainName);
            entity.setCustomNameVisible(true);
        }
        return true;
    }

    static String normalizeBaseName(String name) {
        if (!isUsableBaseName(name)) {
            return "";
        }
        return name.trim();
    }

    private static void addIfPresent(Set<UUID> target, UUID value) {
        if (value != null) {
            target.add(value);
        }
    }

    private static String resolveBaseName(Entity entity) {
        if (entity == null) {
            return null;
        }

        Component customName = entity.getCustomName();
        String recoveredName = recoverBaseNameFromCapitalsWrapper(customName);
        if (isUsableBaseName(recoveredName)) {
            String baseName = recoveredName.trim();
            Component plainName = Component.literal(baseName);

            if (!plainName.equals(customName)) {
                entity.setCustomName(plainName);
                entity.setCustomNameVisible(true);
            }

            return baseName;
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
}
