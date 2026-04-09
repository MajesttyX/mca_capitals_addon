package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

final class MCAClothingBridge {

    private MCAClothingBridge() {
    }

    static String getClothes(ServerLevel level, UUID entityId) {
        Entity entity = MCAEntityBridge.getEntityByUuid(level, entityId);
        if (!MCAEntityBridge.isMCAVillagerEntity(entity)) {
            return null;
        }

        Object clothes = MCAReflectionHelper.invoke(entity, "getClothes");
        return clothes instanceof String s ? s : null;
    }

    static boolean setClothes(ServerLevel level, UUID entityId, String clothesId) {
        Entity entity = MCAEntityBridge.getEntityByUuid(level, entityId);
        if (!MCAEntityBridge.isMCAVillagerEntity(entity) || clothesId == null || clothesId.isBlank()) {
            return false;
        }

        Object result = MCAReflectionHelper.invoke(
                entity,
                "setClothes",
                new Class<?>[] {String.class},
                clothesId
        );

        return result != null || clothingExists(clothesId);
    }

    static void randomizeClothes(ServerLevel level, UUID entityId) {
        Entity entity = MCAEntityBridge.getEntityByUuid(level, entityId);
        if (!MCAEntityBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        MCAReflectionHelper.invoke(entity, "randomizeClothes");
    }

    static boolean clothingExists(String clothesId) {
        if (clothesId == null || clothesId.isBlank()) {
            return false;
        }

        if (existsInBaseClothingList(clothesId)) {
            return true;
        }

        return existsInCustomClothingManager(clothesId);
    }

    private static boolean existsInBaseClothingList(String clothesId) {
        Class<?> clothingListClass = MCAReflectionHelper.resolveAnyClass(MCAReflectionHelper.MCA_CLOTHING_LIST_CLASSES);
        if (clothingListClass == null) {
            return false;
        }

        Object instance = MCAReflectionHelper.invokeStatic(clothingListClass, "getInstance", new Class<?>[0]);
        if (instance == null) {
            MCAReflectionHelper.warnOnce(
                    "clothingExists:getInstanceNull",
                    "MCA ClothingList#getInstance returned null"
            );
            return false;
        }

        try {
            Field clothingField = instance.getClass().getField("clothing");
            Object clothingMap = clothingField.get(instance);
            if (clothingMap instanceof Map<?, ?> map) {
                return map.containsKey(clothesId);
            }

            if (clothingMap != null) {
                MCAReflectionHelper.warnOnce(
                        "clothingExists:unexpectedFieldType",
                        "MCA ClothingList#clothing returned unexpected type: {}",
                        clothingMap.getClass().getName()
                );
            }
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "clothingExists:fieldAccess",
                    "Failed to inspect MCA ClothingList#clothing ({})",
                    t.toString()
            );
        }

        return false;
    }

    private static boolean existsInCustomClothingManager(String clothesId) {
        String[] classNames = new String[] {
                "net.mca.server.world.data.CustomClothingManager",
                "forge.net.mca.server.world.data.CustomClothingManager",
                "fabric.net.mca.server.world.data.CustomClothingManager",
                "quilt.net.mca.server.world.data.CustomClothingManager"
        };

        Class<?> managerClass = MCAReflectionHelper.resolveAnyClass(classNames);
        if (managerClass == null) {
            return false;
        }

        try {
            Method getClothing = managerClass.getMethod("getClothing");
            Object storage = getClothing.invoke(null);
            if (storage == null) {
                return false;
            }

            Method getEntries = storage.getClass().getMethod("getEntries");
            Object entries = getEntries.invoke(storage);
            if (entries instanceof Map<?, ?> map) {
                return map.containsKey(clothesId);
            }
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "clothingExists:customManagerAccess",
                    "Failed to inspect MCA CustomClothingManager ({})",
                    t.toString()
            );
        }

        return false;
    }
}