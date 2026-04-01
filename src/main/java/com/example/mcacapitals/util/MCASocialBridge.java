package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

final class MCASocialBridge {

    private MCASocialBridge() {
    }

    static boolean adjustHearts(ServerLevel level, UUID villagerId, UUID playerId, int delta) {
        if (level == null || villagerId == null || playerId == null || delta == 0) {
            return false;
        }

        Entity entity = MCAEntityBridge.getEntityByUuid(level, villagerId);
        if (!MCAEntityBridge.isMCAVillagerEntity(entity)) {
            return false;
        }

        try {
            Object brain = MCAReflectionHelper.invoke(entity, "getVillagerBrain");
            if (brain == null) {
                return false;
            }

            Object memoriesObj = MCAReflectionHelper.invoke(brain, "getMemories");
            if (!(memoriesObj instanceof Map<?, ?> memories)) {
                return false;
            }

            Object memory = memories.get(playerId);
            if (memory == null) {
                return false;
            }

            Object heartsObj = MCAReflectionHelper.invoke(memory, "getHearts");
            int currentHearts = heartsObj instanceof Integer i ? i : 0;
            int newHearts = currentHearts + delta;

            if (trySetHeartsByMethod(memory, newHearts)) {
                return true;
            }

            return trySetHeartsByField(memory, newHearts);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean stopInteracting(Entity villagerEntity) {
        if (villagerEntity == null) {
            return false;
        }

        try {
            Object interactions = MCAReflectionHelper.invoke(villagerEntity, "getInteractions");
            if (interactions == null) {
                return false;
            }

            Object result = MCAReflectionHelper.invoke(interactions, "stopInteracting");
            return result == null || Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean trySetHeartsByMethod(Object memory, int newHearts) {
        try {
            Method setter = memory.getClass().getMethod("setHearts", int.class);
            setter.invoke(memory, newHearts);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean trySetHeartsByField(Object memory, int newHearts) {
        try {
            Field heartsField = memory.getClass().getDeclaredField("hearts");
            heartsField.setAccessible(true);
            heartsField.setInt(memory, newHearts);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}