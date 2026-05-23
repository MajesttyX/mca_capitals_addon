package com.majesttyx.mcacapitals.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class MCAVillageBridge {

    private MCAVillageBridge() {
    }

    static Set<Integer> getVillageIdsAtOrAbovePopulation(ServerLevel level, int requiredPopulation) {
        if (level == null) {
            return Collections.emptySet();
        }

        Set<Integer> result = new HashSet<>();
        for (Object village : getAllVillages(level)) {
            if (isVillage(village) && getVillagePopulation(village) >= requiredPopulation) {
                Integer id = getVillageId(village);
                if (id != null) {
                    result.add(id);
                }
            }
        }

        return result;
    }

    static Set<Integer> getAllVillageIds(ServerLevel level) {
        if (level == null) {
            return Collections.emptySet();
        }

        Set<Integer> result = new HashSet<>();
        for (Object village : getAllVillages(level)) {
            Integer id = getVillageId(village);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    static Integer getVillageIdForResident(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return null;
        }

        for (Object village : getAllVillages(level)) {
            if (getVillageResidents(village).contains(entityId)) {
                return getVillageId(village);
            }
        }

        return null;
    }

    static boolean hasVillage(ServerLevel level, int villageId) {
        if (level == null) {
            return false;
        }

        return getVillageObject(level, villageId) != null;
    }

    static boolean isVillage(ServerLevel level, int villageId) {
        Object village = getVillageObject(level, villageId);
        return village != null && isVillage(village);
    }

    static int getVillagePopulation(ServerLevel level, int villageId) {
        Object village = getVillageObject(level, villageId);
        return village == null ? 0 : getVillagePopulation(village);
    }

    static String getVillageName(ServerLevel level, Integer villageId) {
        Object village = villageId == null ? null : getVillageObject(level, villageId);
        if (village == null) {
            return "Unknown Village";
        }

        String name = MCAReflectionHelper.invokeString(village, "getName");
        return name == null || name.isBlank() ? "Unknown Village" : name;
    }

    static BlockPos getVillageCenter(ServerLevel level, Integer villageId) {
        Object village = villageId == null ? null : getVillageObject(level, villageId);
        if (village == null) {
            return BlockPos.ZERO;
        }

        Object center = MCAReflectionHelper.invoke(village, "getCenter");
        if (center instanceof Vec3i vec) {
            return new BlockPos(vec.getX(), vec.getY(), vec.getZ());
        }

        return BlockPos.ZERO;
    }

    static Set<UUID> getVillageResidents(ServerLevel level, int villageId) {
        Object village = getVillageObject(level, villageId);
        return village == null ? Collections.emptySet() : getVillageResidents(village);
    }

    static Map<UUID, String> getVillageResidentNames(ServerLevel level, int villageId) {
        Object village = getVillageObject(level, villageId);
        if (village == null) {
            return Collections.emptyMap();
        }

        Map<UUID, String> loadedResidents = getLoadedVillageResidentNames(level, village);
        if (!loadedResidents.isEmpty()) {
            return loadedResidents;
        }

        Map<UUID, String> savedNames = getSavedVillageResidentNames(village);
        if (!savedNames.isEmpty()) {
            return savedNames;
        }

        Map<UUID, String> fallback = new LinkedHashMap<>();
        for (UUID residentId : getVillageResidents(village)) {
            if (residentId != null) {
                fallback.put(residentId, residentId.toString());
            }
        }
        return fallback;
    }

    private static Object getVillageObject(ServerLevel level, int villageId) {
        for (Object village : getAllVillages(level)) {
            Integer id = getVillageId(village);
            if (id != null && id == villageId) {
                return village;
            }
        }
        return null;
    }

    private static Iterable<?> getAllVillages(ServerLevel level) {
        if (level == null) {
            return Collections.emptyList();
        }

        for (String className : MCAReflectionHelper.MCA_VILLAGE_MANAGER_CLASSES) {
            try {
                Class<?> managerClass = Class.forName(className);
                Object manager = MCAReflectionHelper.invokeStatic(
                        managerClass,
                        "get",
                        new Class<?>[] {ServerLevel.class},
                        level
                );
                if (manager instanceof Iterable<?> iterable) {
                    return iterable;
                }
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "getAllVillages:" + className,
                        "Failed to query MCA VillageManager class {} ({})",
                        className,
                        t.toString()
                );
            }
        }

        MCAReflectionHelper.warnOnce(
                "getAllVillages:noneResolved",
                "Could not resolve any MCA VillageManager for current level"
        );
        return Collections.emptyList();
    }

    private static Integer getVillageId(Object village) {
        Object value = MCAReflectionHelper.invoke(village, "getId");
        return value instanceof Integer i ? i : null;
    }

    private static boolean isVillage(Object village) {
        Object value = MCAReflectionHelper.invoke(village, "isVillage");
        return value instanceof Boolean b && b;
    }

    private static int getVillagePopulation(Object village) {
        Object value = MCAReflectionHelper.invoke(village, "getPopulation");
        return value instanceof Integer i ? i : 0;
    }

    private static Set<UUID> getVillageResidents(Object village) {
        Object value = MCAReflectionHelper.invoke(village, "getResidentsUUIDs");
        return MCAReflectionHelper.extractUuidSet(value);
    }

    private static Map<UUID, String> getLoadedVillageResidentNames(ServerLevel level, Object village) {
        if (level == null || village == null) {
            return Collections.emptyMap();
        }

        Map<UUID, String> result = new LinkedHashMap<>();
        Object residents = MCAReflectionHelper.invoke(
                village,
                "getResidents",
                new Class<?>[] {ServerLevel.class},
                level
        );

        if (residents instanceof Iterable<?> iterable) {
            for (Object value : iterable) {
                if (!(value instanceof Entity entity)) {
                    continue;
                }
                if (!MCAEntityBridge.isAliveMCAVillagerEntity(entity)) {
                    continue;
                }

                result.put(entity.getUUID(), entity.getName().getString());
            }
        }

        return result;
    }

    private static Map<UUID, String> getSavedVillageResidentNames(Object village) {
        if (village == null) {
            return Collections.emptyMap();
        }

        Object value = MCAReflectionHelper.invoke(village, "getResidentNames");
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }

        Map<UUID, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof UUID uuid && entry.getValue() instanceof String name) {
                if (name != null && !name.isBlank()) {
                    result.put(uuid, name);
                }
            }
        }

        return result;
    }
}