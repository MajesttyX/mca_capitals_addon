package com.example.mcacapitals.util;

import com.example.mcacapitals.MCACapitals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCAIntegrationBridge {

    private static final String[] MCA_VILLAGER_CLASSES = new String[] {
            "net.mca.entity.VillagerEntityMCA",
            "forge.net.mca.entity.VillagerEntityMCA",
            "fabric.net.mca.entity.VillagerEntityMCA",
            "quilt.net.mca.entity.VillagerEntityMCA"
    };

    private static final String[] MCA_FAMILY_TREE_CLASSES = new String[] {
            "net.mca.server.world.data.FamilyTree",
            "forge.net.mca.server.world.data.FamilyTree",
            "fabric.net.mca.server.world.data.FamilyTree",
            "quilt.net.mca.server.world.data.FamilyTree"
    };

    private static final String[] MCA_VILLAGE_MANAGER_CLASSES = new String[] {
            "net.mca.server.world.data.VillageManager",
            "forge.net.mca.server.world.data.VillageManager",
            "fabric.net.mca.server.world.data.VillageManager",
            "quilt.net.mca.server.world.data.VillageManager"
    };

    private static final String[] MCA_CLOTHING_LIST_CLASSES = new String[] {
            "net.mca.resources.ClothingList",
            "forge.net.mca.resources.ClothingList",
            "fabric.net.mca.resources.ClothingList",
            "quilt.net.mca.resources.ClothingList"
    };

    private MCAIntegrationBridge() {
    }

    public static Entity getEntityByUuid(ServerLevel level, UUID entityId) {
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

    public static boolean isMCAVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        return isMCAVillagerEntity(entity);
    }

    public static boolean isMCAVillagerEntity(Object entity) {
        if (entity == null) {
            return false;
        }

        Class<?> villagerClass = resolveAnyClass(MCA_VILLAGER_CLASSES);
        return villagerClass != null && villagerClass.isInstance(entity);
    }

    public static boolean isAliveMCAVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        return isAliveMCAVillagerEntity(entity);
    }

    public static boolean isAliveMCAVillagerEntity(Entity entity) {
        return entity != null && isMCAVillagerEntity(entity) && entity.isAlive() && !entity.isRemoved();
    }

    public static boolean isFemale(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        Object genetics = invoke(entity, "getGenetics");
        if (genetics == null) {
            return false;
        }

        Object gender = invoke(genetics, "getGender");
        if (gender == null) {
            return false;
        }

        Object binary = invoke(gender, "binary");
        if (binary == null) {
            return false;
        }

        Object dataName = invoke(binary, "getDataName");
        return dataName instanceof String s && s.equalsIgnoreCase("female");
    }

    public static String getAgeState(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return "UNASSIGNED";
        }

        Object ageState = invoke(entity, "getAgeState");
        if (ageState == null) {
            return "UNASSIGNED";
        }

        if (ageState instanceof Enum<?> e) {
            return e.name();
        }

        return String.valueOf(ageState);
    }

    public static boolean isTeenOrAdultVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        String ageState = getAgeState(level, entityId);
        return "TEEN".equalsIgnoreCase(ageState) || "ADULT".equalsIgnoreCase(ageState);
    }

    public static boolean hasFamilyNode(ServerLevel level, UUID entityId) {
        return getFamilyNode(level, entityId).isPresent();
    }

    public static UUID getSpouse(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return null;
        }

        UUID partner = asUuid(invoke(nodeOpt.get(), "partner"));
        return isNullUuid(partner) ? null : partner;
    }

    public static Set<UUID> getChildren(ServerLevel level, UUID entityId) {
        Optional<Object> nodeOpt = getFamilyNode(level, entityId);
        if (nodeOpt.isEmpty()) {
            return Collections.emptySet();
        }

        return extractUuidSet(invoke(nodeOpt.get(), "children"));
    }

    public static boolean isChildOf(ServerLevel level, UUID childId, UUID parentId) {
        Optional<Object> childOpt = getFamilyNode(level, childId);
        if (childOpt.isEmpty() || parentId == null) {
            return false;
        }

        Object child = childOpt.get();
        UUID father = asUuid(invoke(child, "father"));
        UUID mother = asUuid(invoke(child, "mother"));

        return parentId.equals(father) || parentId.equals(mother);
    }

    public static boolean isMCAGuard(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        String profession = getProfessionName(entity);
        return profession.contains("guard") || profession.contains("archer");
    }

    public static boolean isMCAFootGuard(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        String profession = getProfessionName(entity);
        return profession.contains("guard") && !profession.contains("archer");
    }

    public static boolean isMasterProfessionVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        Integer levelValue = getProfessionLevel(entity);
        return levelValue != null && levelValue >= 5;
    }

    public static boolean isAliveAdultOrChildVillager(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        return isMCAVillagerEntity(entity) && entity.isAlive() && !entity.isRemoved();
    }

    public static String describeProfession(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return "non_mca";
        }

        String profession = getProfessionName(entity);
        Integer professionLevel = getProfessionLevel(entity);
        return profession + "@" + (professionLevel == null ? "unknown" : professionLevel);
    }

    public static Set<Integer> getVillageIdsAtOrAbovePopulation(ServerLevel level, int requiredPopulation) {
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

    public static Set<Integer> getAllVillageIds(ServerLevel level) {
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

    public static Integer getVillageIdForResident(ServerLevel level, UUID entityId) {
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

    public static boolean hasVillage(ServerLevel level, int villageId) {
        if (level == null) {
            return false;
        }

        return getVillageObject(level, villageId) != null;
    }

    public static boolean isVillage(ServerLevel level, int villageId) {
        Object village = getVillageObject(level, villageId);
        return village != null && isVillage(village);
    }

    public static int getVillagePopulation(ServerLevel level, int villageId) {
        Object village = getVillageObject(level, villageId);
        return village == null ? 0 : getVillagePopulation(village);
    }

    public static String getVillageName(ServerLevel level, Integer villageId) {
        Object village = villageId == null ? null : getVillageObject(level, villageId);
        if (village == null) {
            return "Unknown Village";
        }

        String name = invokeString(village, "getName");
        return name == null || name.isBlank() ? "Unknown Village" : name;
    }

    public static BlockPos getVillageCenter(ServerLevel level, Integer villageId) {
        Object village = villageId == null ? null : getVillageObject(level, villageId);
        if (village == null) {
            return BlockPos.ZERO;
        }

        Object center = invoke(village, "getCenter");
        if (center instanceof Vec3i vec) {
            return new BlockPos(vec.getX(), vec.getY(), vec.getZ());
        }

        return BlockPos.ZERO;
    }

    public static Set<UUID> getVillageResidents(ServerLevel level, int villageId) {
        Object village = getVillageObject(level, villageId);
        return village == null ? Collections.emptySet() : getVillageResidents(village);
    }

    public static int getHeartsWithPlayer(ServerLevel level, UUID villagerId, UUID playerId) {
        Entity entity = getEntityByUuid(level, villagerId);
        if (!isMCAVillagerEntity(entity) || playerId == null) {
            return 0;
        }

        Object brain = invoke(entity, "getVillagerBrain");
        if (brain == null) {
            return 0;
        }

        Object memoriesObj = invoke(brain, "getMemories");
        if (!(memoriesObj instanceof Map<?, ?> memories)) {
            return 0;
        }

        Object memory = memories.get(playerId);
        if (memory == null) {
            return 0;
        }

        Object hearts = invoke(memory, "getHearts");
        return hearts instanceof Integer i ? i : 0;
    }

    public static String getClothes(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return null;
        }

        Object clothes = invoke(entity, "getClothes");
        return clothes instanceof String s ? s : null;
    }

    public static boolean setClothes(ServerLevel level, UUID entityId, String clothesId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity) || clothesId == null || clothesId.isBlank()) {
            return false;
        }

        Object result = invoke(entity, "setClothes", new Class<?>[] {String.class}, clothesId);
        return result != null || clothingExists(clothesId);
    }

    public static void randomizeClothes(ServerLevel level, UUID entityId) {
        Entity entity = getEntityByUuid(level, entityId);
        if (!isMCAVillagerEntity(entity)) {
            return;
        }

        invoke(entity, "randomizeClothes");
    }

    public static boolean clothingExists(String clothesId) {
        if (clothesId == null || clothesId.isBlank()) {
            return false;
        }

        Class<?> clothingListClass = resolveAnyClass(MCA_CLOTHING_LIST_CLASSES);
        if (clothingListClass == null) {
            return false;
        }

        Object instance = invokeStatic(clothingListClass, "getInstance", new Class<?>[0]);
        if (instance == null) {
            return false;
        }

        try {
            Field clothingField = instance.getClass().getField("clothing");
            Object clothingMap = clothingField.get(instance);
            if (clothingMap instanceof Map<?, ?> map) {
                return map.containsKey(clothesId);
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    public static List<Entity> getNearbyMCAVillagers(ServerLevel level, AABB area) {
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

    public static void addEffect(Entity entity, MobEffectInstance effect) {
        if (entity instanceof LivingEntity living && effect != null) {
            living.addEffect(effect);
        }
    }

    public static boolean moveTo(Entity entity, double x, double y, double z, double speed) {
        if (!isMCAVillagerEntity(entity)) {
            return false;
        }

        Object navigation = invoke(entity, "getNavigation");
        if (navigation == null) {
            return false;
        }

        Object result = invoke(
                navigation,
                "moveTo",
                new Class<?>[] {double.class, double.class, double.class, double.class},
                x, y, z, speed
        );

        return result instanceof Boolean b ? b : result != null;
    }

    public static String describeEntity(ServerLevel level, UUID entityId) {
        return "isMCA=" + isMCAVillager(level, entityId)
                + ", hasFamilyNode=" + hasFamilyNode(level, entityId)
                + ", isGuard=" + isMCAGuard(level, entityId)
                + ", isFootGuard=" + isMCAFootGuard(level, entityId)
                + ", isMaster=" + isMasterProfessionVillager(level, entityId)
                + ", isFemale=" + isFemale(level, entityId)
                + ", ageState=" + getAgeState(level, entityId)
                + ", spouse=" + (getSpouse(level, entityId) == null ? "none" : getSpouse(level, entityId))
                + ", childCount=" + getChildren(level, entityId).size()
                + ", profession=" + describeProfession(level, entityId);
    }

    private static Optional<Object> getFamilyNode(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return Optional.empty();
        }

        try {
            Class<?> familyTreeClass = resolveAnyClass(MCA_FAMILY_TREE_CLASSES);
            if (familyTreeClass == null) {
                return Optional.empty();
            }

            Object familyTree = invokeStatic(familyTreeClass, "get", new Class<?>[] {ServerLevel.class}, level);
            if (familyTree == null) {
                return Optional.empty();
            }

            Object optional = invoke(familyTree, "getOrEmpty", new Class<?>[] {UUID.class}, entityId);
            if (optional instanceof Optional<?> opt) {
                return opt.map(node -> node);
            }
        } catch (Throwable ignored) {
        }

        return Optional.empty();
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

        for (String className : MCA_VILLAGE_MANAGER_CLASSES) {
            try {
                Class<?> managerClass = Class.forName(className);
                Object manager = invokeStatic(managerClass, "get", new Class<?>[] {ServerLevel.class}, level);
                if (manager instanceof Iterable<?> iterable) {
                    return iterable;
                }
            } catch (Throwable ignored) {
            }
        }

        MCACapitals.LOGGER.warn("[MCACapitals] Could not resolve MCA VillageManager for {}", level.dimension().location());
        return Collections.emptyList();
    }

    private static Integer getVillageId(Object village) {
        Object value = invoke(village, "getId");
        return value instanceof Integer i ? i : null;
    }

    private static boolean isVillage(Object village) {
        Object value = invoke(village, "isVillage");
        return value instanceof Boolean b && b;
    }

    private static int getVillagePopulation(Object village) {
        Object value = invoke(village, "getPopulation");
        return value instanceof Integer i ? i : 0;
    }

    private static Set<UUID> getVillageResidents(Object village) {
        Object value = invoke(village, "getResidentsUUIDs");
        return extractUuidSet(value);
    }

    private static Set<UUID> extractUuidSet(Object value) {
        if (value instanceof Stream<?> stream) {
            try {
                return stream.filter(UUID.class::isInstance)
                        .map(UUID.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            } finally {
                stream.close();
            }
        }

        if (value instanceof Iterable<?> iterable) {
            Set<UUID> result = new LinkedHashSet<>();
            for (Object obj : iterable) {
                if (obj instanceof UUID uuid && !isNullUuid(uuid)) {
                    result.add(uuid);
                }
            }
            return result;
        }

        if (value instanceof Collection<?> collection) {
            Set<UUID> result = new LinkedHashSet<>();
            for (Object obj : collection) {
                if (obj instanceof UUID uuid && !isNullUuid(uuid)) {
                    result.add(uuid);
                }
            }
            return result;
        }

        return Collections.emptySet();
    }

    private static String getProfessionName(Object villager) {
        Object directProfession = invoke(villager, "getProfession");
        if (directProfession != null) {
            String direct = normalizeProfessionString(directProfession.toString());
            if (!direct.isBlank() && !"unknown".equals(direct)) {
                return direct;
            }
        }

        if (villager instanceof Villager vanillaVillager) {
            try {
                return normalizeProfessionString(vanillaVillager.getVillagerData().getProfession().toString());
            } catch (Throwable ignored) {
            }
        }

        Object villagerData = invoke(villager, "getVillagerData");
        if (villagerData == null) {
            return "unknown";
        }

        Object profession = invoke(villagerData, "getProfession");
        if (profession == null) {
            return "unknown";
        }

        return normalizeProfessionString(profession.toString());
    }

    private static Integer getProfessionLevel(Object villager) {
        if (villager instanceof Villager vanillaVillager) {
            try {
                return vanillaVillager.getVillagerData().getLevel();
            } catch (Throwable ignored) {
            }
        }

        Object villagerData = invoke(villager, "getVillagerData");
        if (villagerData == null) {
            return null;
        }

        Object level = invoke(villagerData, "getLevel");
        return level instanceof Integer i ? i : null;
    }

    private static String normalizeProfessionString(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }

        String result = raw.toLowerCase(Locale.ROOT).trim();

        int slash = result.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < result.length()) {
            result = result.substring(slash + 1);
        }

        int colon = result.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < result.length()) {
            result = result.substring(colon + 1);
        }

        if (result.startsWith("villager_profession.")) {
            result = result.substring("villager_profession.".length());
        }

        if (result.startsWith("profession.")) {
            result = result.substring("profession.".length());
        }

        if (result.startsWith("entity.minecraft.")) {
            result = result.substring("entity.minecraft.".length());
        }

        return result.isBlank() ? "unknown" : result;
    }

    private static Class<?> resolveAnyClass(String[] classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeStatic(Class<?> targetClass, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (targetClass == null) {
            return null;
        }

        try {
            Method method = targetClass.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String invokeString(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value instanceof String s ? s : null;
    }

    private static UUID asUuid(Object value) {
        return value instanceof UUID uuid ? uuid : null;
    }

    private static boolean isNullUuid(UUID uuid) {
        return uuid == null || new UUID(0L, 0L).equals(uuid);
    }
}