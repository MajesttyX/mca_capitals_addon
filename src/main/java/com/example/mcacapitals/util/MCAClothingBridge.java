package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
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
}