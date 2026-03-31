package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

final class MCAPlayerBridge {

    private static final String[] MCA_PLAYER_SAVE_DATA_CLASSES = new String[] {
            "net.mca.server.world.data.PlayerSaveData",
            "forge.net.mca.server.world.data.PlayerSaveData",
            "fabric.net.mca.server.world.data.PlayerSaveData",
            "quilt.net.mca.server.world.data.PlayerSaveData"
    };

    private MCAPlayerBridge() {
    }

    static Optional<Integer> getLastSeenVillageId(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return Optional.empty();
        }

        for (String className : MCA_PLAYER_SAVE_DATA_CLASSES) {
            try {
                Object saveData = getPlayerSaveData(level, player, className);
                if (saveData == null) {
                    continue;
                }

                Object value = MCAReflectionHelper.invoke(saveData, "getLastSeenVillageId");
                if (value instanceof Optional<?> optional) {
                    Object id = optional.orElse(null);
                    if (id instanceof Integer villageId) {
                        return Optional.of(villageId);
                    }
                }
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "MCAPlayerBridge#getLastSeenVillageId:" + className,
                        "Failed to query MCA PlayerSaveData class {} ({})",
                        className,
                        t.toString()
                );
            }
        }

        return Optional.empty();
    }

    static boolean isPlayerInVillage(ServerLevel level, ServerPlayer player, Integer villageId) {
        if (level == null || player == null || villageId == null) {
            return false;
        }

        return getLastSeenVillageId(level, player)
                .map(id -> id.equals(villageId))
                .orElse(false);
    }

    static boolean isPlayerFemale(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return false;
        }

        for (String className : MCA_PLAYER_SAVE_DATA_CLASSES) {
            try {
                Object saveData = getPlayerSaveData(level, player, className);
                if (saveData == null) {
                    continue;
                }

                Object gender = MCAReflectionHelper.invoke(saveData, "getGender");
                if (gender == null) {
                    continue;
                }

                Object dataName = MCAReflectionHelper.invoke(gender, "getDataName");
                String resolved = dataName instanceof String
                        ? (String) dataName
                        : String.valueOf(gender);

                if ("female".equalsIgnoreCase(resolved) || "FEMALE".equalsIgnoreCase(resolved)) {
                    return true;
                }
                if ("male".equalsIgnoreCase(resolved) || "MALE".equalsIgnoreCase(resolved)) {
                    return false;
                }
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "MCAPlayerBridge#isPlayerFemale:" + className,
                        "Failed to query MCA PlayerSaveData gender class {} ({})",
                        className,
                        t.toString()
                );
            }
        }

        return false;
    }

    private static Object getPlayerSaveData(ServerLevel level, ServerPlayer player, String className) throws Exception {
        Class<?> playerSaveDataClass = Class.forName(className);

        Object saveData = MCAReflectionHelper.invokeStatic(
                playerSaveDataClass,
                "get",
                new Class<?>[] {player.getClass()},
                player
        );

        if (saveData == null) {
            saveData = MCAReflectionHelper.invokeStatic(
                    playerSaveDataClass,
                    "get",
                    new Class<?>[] {ServerLevel.class, UUID.class},
                    level,
                    player.getUUID()
            );
        }

        return saveData;
    }
}