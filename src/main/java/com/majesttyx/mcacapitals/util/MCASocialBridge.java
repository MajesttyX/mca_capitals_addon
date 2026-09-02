package com.majesttyx.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

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
        if (MCAFamilyBridge.isFamilyNodeDeceased(level, villagerId)) {
            return false;
        }

        Entity entity = MCAEntityBridge.getEntityByUuid(level, villagerId);
        if (!MCAEntityBridge.isMCAVillagerEntity(entity)) {
            MCAResidentStateSavedData.get(level).queueHeartDelta(villagerId, playerId, delta);
            return true;
        }

        applyPendingHeartDeltas(level, entity);
        Object memory = resolveMemory(level, entity, playerId);
        if (memory == null) {
            MCAResidentStateSavedData.get(level).queueHeartDelta(villagerId, playerId, delta);
            return true;
        }

        Integer currentHearts = readHearts(memory);
        if (currentHearts == null) {
            MCAResidentStateSavedData.get(level).queueHeartDelta(villagerId, playerId, delta);
            return true;
        }

        boolean updated = setHearts(memory, currentHearts + delta);
        if (updated) {
            MCAEntityBridge.captureResidentState(level, entity);
        }
        return updated;
    }

    static void applyPendingHeartDeltas(ServerLevel level, Entity entity) {
        if (level == null || !MCAEntityBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        MCAResidentStateSavedData data = MCAResidentStateSavedData.get(level);
        Map<UUID, Integer> pending = data.getPendingHeartDeltas(entity.getUUID());
        if (pending.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Integer> entry : pending.entrySet()) {
            UUID playerId = entry.getKey();
            int delta = entry.getValue();
            if (delta == 0) {
                data.acknowledgeHeartDelta(entity.getUUID(), playerId);
                continue;
            }

            Object memory = resolveMemory(level, entity, playerId);
            if (memory == null) {
                continue;
            }

            Integer currentHearts = readHearts(memory);
            if (currentHearts == null || !setHearts(memory, currentHearts + delta)) {
                continue;
            }

            // Acknowledge each applied delta independently so a later failure cannot replay it.
            data.acknowledgeHeartDelta(entity.getUUID(), playerId);
        }
    }

    private static Object resolveMemory(ServerLevel level, Entity entity, UUID playerId) {
        try {
            Object brain = MCAReflectionHelper.invoke(entity, "getVillagerBrain");
            if (brain == null) {
                return null;
            }

            Object memoriesObj = MCAReflectionHelper.invoke(brain, "getMemories");
            if (memoriesObj instanceof Map<?, ?> memories) {
                Object existing = memories.get(playerId);
                if (existing != null) {
                    return existing;
                }
            }

            if (level.getServer() != null) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    Object created = MCAReflectionHelper.invoke(
                            brain,
                            "getMemoriesForPlayer",
                            new Class<?>[] {Player.class},
                            player
                    );
                    if (created != null) {
                        return created;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Integer readHearts(Object memory) {
        Object heartsObj = MCAReflectionHelper.invoke(memory, "getHearts");
        return heartsObj instanceof Integer i ? i : null;
    }

    private static boolean setHearts(Object memory, int newHearts) {
        return trySetHeartsByMethod(memory, newHearts) || trySetHeartsByField(memory, newHearts);
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
