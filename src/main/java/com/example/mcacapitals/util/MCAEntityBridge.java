package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
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

        for (Entity entity : level.getEntities().getAll()) {
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
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        Integer levelValue = MCAReflectionHelper.getProfessionLevel(entity);
        return levelValue != null && levelValue >= 5;
    }

    static boolean isAliveAdultOrChildVillager(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        return isMCAVillagerEntity(entity) && entity.isAlive() && !entity.isRemoved();
    }

    static String describeProfession(ServerLevel level, UUID entityId) {
        Entity entity = findLoadedEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return "non_mca";
        }

        String profession = MCAReflectionHelper.getProfessionName(entity);
        Integer professionLevel = MCAReflectionHelper.getProfessionLevel(entity);
        return profession + "@" + (professionLevel == null ? "unknown" : professionLevel);
    }

    static int getHeartsWithPlayer(ServerLevel level, UUID villagerId, UUID playerId) {
        Entity entity = findLoadedEntityByUuid(level, villagerId);
        if (!isMCAVillagerEntity(entity) || playerId == null) {
            return 0;
        }

        Object brain = MCAReflectionHelper.invoke(entity, "getVillagerBrain");
        if (brain == null) {
            return 0;
        }

        Object memoriesObj = MCAReflectionHelper.invoke(brain, "getMemories");
        if (!(memoriesObj instanceof Map<?, ?> memories)) {
            return 0;
        }

        Object memory = memories.get(playerId);
        if (memory == null) {
            return 0;
        }

        Object hearts = MCAReflectionHelper.invoke(memory, "getHearts");
        return hearts instanceof Integer i ? i : 0;
    }

    static List<Entity> getNearbyMCAVillagers(ServerLevel level, AABB area) {
        if (level == null || area == null) {
            return Collections.emptyList();
        }

        List<Entity> result = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
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