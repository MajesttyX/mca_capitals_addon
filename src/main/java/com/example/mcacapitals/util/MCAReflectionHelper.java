package com.example.mcacapitals.util;

import com.example.mcacapitals.MCACapitals;
import net.minecraft.world.entity.npc.Villager;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class MCAReflectionHelper {

    static final String[] MCA_VILLAGER_CLASSES = new String[] {
            "net.mca.entity.VillagerEntityMCA",
            "forge.net.mca.entity.VillagerEntityMCA",
            "fabric.net.mca.entity.VillagerEntityMCA",
            "quilt.net.mca.entity.VillagerEntityMCA"
    };

    static final String[] MCA_FAMILY_TREE_CLASSES = new String[] {
            "net.mca.server.world.data.FamilyTree",
            "forge.net.mca.server.world.data.FamilyTree",
            "fabric.net.mca.server.world.data.FamilyTree",
            "quilt.net.mca.server.world.data.FamilyTree"
    };

    static final String[] MCA_VILLAGE_MANAGER_CLASSES = new String[] {
            "net.mca.server.world.data.VillageManager",
            "forge.net.mca.server.world.data.VillageManager",
            "fabric.net.mca.server.world.data.VillageManager",
            "quilt.net.mca.server.world.data.VillageManager"
    };

    static final String[] MCA_CLOTHING_LIST_CLASSES = new String[] {
            "net.mca.resources.ClothingList",
            "forge.net.mca.resources.ClothingList",
            "fabric.net.mca.resources.ClothingList",
            "quilt.net.mca.resources.ClothingList"
    };

    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    private MCAReflectionHelper() {
    }

    static Class<?> resolveAnyClass(String[] classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (Throwable ignored) {
            }
        }

        warnOnce(
                "resolveAnyClass:" + String.join("|", classNames),
                "Could not resolve any MCA class from candidates: {}",
                String.join(", ", classNames)
        );
        return null;
    }

    static Object invokeStatic(Class<?> targetClass, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (targetClass == null) {
            return null;
        }

        try {
            Method method = targetClass.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (Throwable t) {
            warnOnce(
                    "invokeStatic:" + targetClass.getName() + "#" + methodName,
                    "Static reflection call failed: {}#{} ({})",
                    targetClass.getName(),
                    methodName,
                    t.toString()
            );
            return null;
        }
    }

    static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable t) {
            warnOnce(
                    "invoke:" + target.getClass().getName() + "#" + methodName,
                    "Reflection call failed: {}#{} ({})",
                    target.getClass().getName(),
                    methodName,
                    t.toString()
            );
            return null;
        }
    }

    static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Throwable t) {
            warnOnce(
                    "invoke:" + target.getClass().getName() + "#" + methodName + ":" + parameterSignature(parameterTypes),
                    "Reflection call failed: {}#{} ({})",
                    target.getClass().getName(),
                    methodName,
                    t.toString()
            );
            return null;
        }
    }

    static String invokeString(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value instanceof String s ? s : null;
    }

    static UUID asUuid(Object value) {
        return value instanceof UUID uuid ? uuid : null;
    }

    static boolean isNullUuid(UUID uuid) {
        return uuid == null || new UUID(0L, 0L).equals(uuid);
    }

    static Set<UUID> extractUuidSet(Object value) {
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

    static String getProfessionName(Object villager) {
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
            } catch (Throwable t) {
                warnOnce(
                        "getProfessionName:vanillaVillagerData",
                        "Failed to read vanilla villager profession ({})",
                        t.toString()
                );
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

    static Integer getProfessionLevel(Object villager) {
        if (villager instanceof Villager vanillaVillager) {
            try {
                return vanillaVillager.getVillagerData().getLevel();
            } catch (Throwable t) {
                warnOnce(
                        "getProfessionLevel:vanillaVillagerData",
                        "Failed to read vanilla villager profession level ({})",
                        t.toString()
                );
            }
        }

        Object villagerData = invoke(villager, "getVillagerData");
        if (villagerData == null) {
            return null;
        }

        Object level = invoke(villagerData, "getLevel");
        return level instanceof Integer i ? i : null;
    }

    static String normalizeProfessionString(String raw) {
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

    static void warnOnce(String key, String message, Object... args) {
        if (WARNED_KEYS.add(key)) {
            MCACapitals.LOGGER.warn("[MCACapitals] " + message, args);
        }
    }

    private static String parameterSignature(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "()";
        }

        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameterTypes[i] == null ? "null" : parameterTypes[i].getSimpleName());
        }
        builder.append(')');
        return builder.toString();
    }
}