package com.majesttyx.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class MCAEntityBridge {

    private MCAEntityBridge() {
    }

    static Entity findLoadedEntityByUuid(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return null;
        }

        for (Entity entity : level.getAllEntities()) {
            if (entityId.equals(entity.getUUID())) {
                return entity;
            }
        }

        return null;
    }

    static Entity getEntityByUuid(ServerLevel level, UUID entityId) {
        return findLoadedEntityByUuid(level, entityId);
    }

    static Entity findLoadedMCAVillagerByUuid(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        return isMCAVillagerEntity(entity) ? entity : null;
    }

    static boolean isLoadedAndAlive(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }

    static boolean isMCAVillager(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        return isMCAVillagerEntity(entity);
    }

    static boolean isMCAVillagerEntity(Object entity) {
        if (entity == null) {
            return false;
        }

        Class<?> villagerClass = MCAReflectionHelper.resolveAnyClass(MCAReflectionHelper.MCA_VILLAGER_CLASSES);
        return villagerClass != null && villagerClass.isInstance(entity);
    }

    static boolean isAliveMCAVillager(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        return isAliveMCAVillagerEntity(entity);
    }

    static boolean isAliveMCAVillagerEntity(Entity entity) {
        return entity != null && isMCAVillagerEntity(entity) && entity.isAlive() && !entity.isRemoved();
    }

    static boolean isFemale(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        Object genetics = MCAReflectionHelper.invoke(entity, "getGenetics");
        if (genetics == null) {
            return false;
        }

        Object gender = MCAReflectionHelper.invoke(genetics, "getGender");
        if (gender == null) {
            return false;
        }

        Object binary = MCAReflectionHelper.invoke(gender, "binary");
        if (binary == null) {
            return false;
        }

        Object dataName = MCAReflectionHelper.invoke(binary, "getDataName");
        return dataName instanceof String s && s.equalsIgnoreCase("female");
    }

    static String getAgeState(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return "UNASSIGNED";
        }

        Object ageState = MCAReflectionHelper.invoke(entity, "getAgeState");
        if (ageState == null) {
            return "UNASSIGNED";
        }

        if (ageState instanceof Enum<?> e) {
            return e.name();
        }

        return String.valueOf(ageState);
    }

    static boolean isTeenOrAdultVillager(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        String ageState = getAgeState(level, entityId);
        return "TEEN".equalsIgnoreCase(ageState) || "ADULT".equalsIgnoreCase(ageState);
    }

    static boolean isMCAGuard(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        String profession = MCAReflectionHelper.getProfessionName(entity);
        return profession.contains(McaProfessionKeys.GUARD) || profession.contains(McaProfessionKeys.ARCHER);
    }

    static boolean isMCAFootGuard(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        String profession = MCAReflectionHelper.getProfessionName(entity);
        return profession.contains(McaProfessionKeys.GUARD) && !profession.contains(McaProfessionKeys.ARCHER);
    }

    static boolean isMasterProfessionVillager(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (isMCAVillagerEntity(entity)) {
            Integer levelValue = MCAReflectionHelper.getProfessionLevel(entity);
            captureResidentState(level, entity);
            if (levelValue != null) {
                return levelValue >= 5;
            }
        }

        if (level == null || entityId == null || MCAFamilyBridge.isFamilyNodeDeceased(level, entityId)) {
            return false;
        }

        return MCAResidentStateSavedData.get(level)
                .getProfessionLevel(entityId)
                .map(value -> value >= 5)
                .orElse(false);
    }

    static boolean isMasterClericVillager(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (isMCAVillagerEntity(entity)) {
            String profession = MCAReflectionHelper.getProfessionName(entity);
            Integer levelValue = MCAReflectionHelper.getProfessionLevel(entity);
            captureResidentState(level, entity);
            if (levelValue != null) {
                return profession.contains(McaProfessionKeys.CLERIC) && levelValue >= 5;
            }
        }

        if (level == null || entityId == null || MCAFamilyBridge.isFamilyNodeDeceased(level, entityId)) {
            return false;
        }

        MCAResidentStateSavedData data = MCAResidentStateSavedData.get(level);
        String profession = data.getProfession(entityId).orElse("");
        int levelValue = data.getProfessionLevel(entityId).orElse(0);
        return profession.contains(McaProfessionKeys.CLERIC) && levelValue >= 5;
    }

    static boolean isAliveAdultOrChildVillager(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        return isMCAVillagerEntity(entity) && entity.isAlive() && !entity.isRemoved();
    }

    static String describeProfession(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (isMCAVillagerEntity(entity)) {
            String profession = MCAReflectionHelper.getProfessionName(entity);
            Integer professionLevel = MCAReflectionHelper.getProfessionLevel(entity);
            captureResidentState(level, entity);
            return profession + "@" + (professionLevel == null ? "unknown" : professionLevel);
        }

        if (level != null && entityId != null) {
            MCAResidentStateSavedData data = MCAResidentStateSavedData.get(level);
            String profession = data.getProfession(entityId).orElse(null);
            Integer professionLevel = data.getProfessionLevel(entityId).orElse(null);
            if (profession != null || professionLevel != null) {
                return (profession == null ? "unknown" : profession)
                        + "@"
                        + (professionLevel == null ? "unknown" : professionLevel);
            }
        }

        return "non_mca";
    }

    static int getHeartsWithPlayer(ServerLevel level, UUID villagerId, UUID playerId) {
        if (level == null || villagerId == null || playerId == null) {
            return 0;
        }

        Entity entity = findLoadedEntityByUuid(level, villagerId);
        if (isMCAVillagerEntity(entity)) {
            Map<UUID, Integer> hearts = readHearts(entity);
            if (hearts != null) {
                captureResidentState(level, entity, hearts);
                return hearts.getOrDefault(playerId, 0);
            }
        }

        if (MCAFamilyBridge.isFamilyNodeDeceased(level, villagerId)) {
            return 0;
        }

        return MCAResidentStateSavedData.get(level)
                .getHearts(villagerId, playerId)
                .orElse(0);
    }

    static void captureLoadedResidentStates(ServerLevel level, Collection<UUID> residentIds) {
        if (level == null || residentIds == null || residentIds.isEmpty()) {
            return;
        }

        for (UUID residentId : residentIds) {
            Entity entity = findLoadedEntityByUuid(level, residentId);
            if (isMCAVillagerEntity(entity)) {
                captureResidentState(level, entity);
            }
        }
    }

    static void captureResidentState(ServerLevel level, Entity entity) {
        if (level == null || !isMCAVillagerEntity(entity)) {
            return;
        }

        Map<UUID, Integer> hearts = readHearts(entity);
        captureResidentState(level, entity, hearts);
    }

    private static void captureResidentState(
            ServerLevel level,
            Entity entity,
            Map<UUID, Integer> hearts
    ) {
        if (level == null || !isMCAVillagerEntity(entity)) {
            return;
        }

        String profession = MCAReflectionHelper.getProfessionName(entity);
        Integer professionLevel = MCAReflectionHelper.getProfessionLevel(entity);
        if (professionLevel == null) {
            return;
        }

        MCAResidentStateSavedData.get(level).update(
                entity.getUUID(),
                profession,
                professionLevel,
                hearts
        );
    }

    private static Map<UUID, Integer> readHearts(Entity entity) {
        if (!isMCAVillagerEntity(entity)) {
            return null;
        }

        Object brain = MCAReflectionHelper.invoke(entity, "getVillagerBrain");
        if (brain == null) {
            return null;
        }

        Object memoriesObj = MCAReflectionHelper.invoke(brain, "getMemories");
        if (!(memoriesObj instanceof Map<?, ?> memories)) {
            return null;
        }

        Map<UUID, Integer> hearts = new HashMap<>();
        for (Map.Entry<?, ?> entry : memories.entrySet()) {
            if (!(entry.getKey() instanceof UUID playerId) || entry.getValue() == null) {
                continue;
            }

            Object heartsValue = MCAReflectionHelper.invoke(entry.getValue(), "getHearts");
            if (heartsValue instanceof Integer value) {
                hearts.put(playerId, value);
            }
        }
        return hearts;
    }

    static List<Entity> getNearbyMCAVillagers(ServerLevel level, AABB area) {
        if (level == null || area == null) {
            return Collections.emptyList();
        }

        List<Entity> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity != null && isAliveMCAVillagerEntity(entity) && entity.getBoundingBox().intersects(area)) {
                result.add(entity);
            }
        }
        return result;
    }

    static void addEffect(Entity entity, MobEffectInstance effect) {
        if (entity instanceof LivingEntity living && effect != null) {
            living.addEffect(effect);
        }
    }

    static boolean moveTo(Entity entity, double x, double y, double z, double speed) {
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        if (entity instanceof Mob mob) {
            return mob.getNavigation().moveTo(x, y, z, speed);
        }

        Object navigation = MCAReflectionHelper.invoke(entity, "getNavigation");
        if (navigation == null) {
            return false;
        }

        Object result = MCAReflectionHelper.invoke(
                navigation,
                "moveTo",
                new Class<?>[] {double.class, double.class, double.class, double.class},
                x, y, z, speed
        );

        return result instanceof Boolean b ? b : result != null;
    }
}