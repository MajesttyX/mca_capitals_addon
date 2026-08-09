package com.majesttyx.mcacapitals.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.UUID;

final class MCAPlayerBridge {

    private static final String[] MCA_PLAYER_SAVE_DATA_CLASSES = new String[] {
            "fabric.net.mca.server.world.data.PlayerSaveData"
    };

    private static final String[] MCA_GENDER_CLASSES = new String[] {
            "fabric.net.mca.entity.ai.relationship.Gender"
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

    static String getDialogueName(ServerPlayer player) {
        if (player == null) {
            return "";
        }

        String messengerName = resolveMessengerName(player);
        if (messengerName != null && !messengerName.isBlank()) {
            return messengerName.trim();
        }

        for (String className : MCA_PLAYER_SAVE_DATA_CLASSES) {
            try {
                Object saveData = getPlayerSaveData(player.serverLevel(), player, className);
                if (saveData == null) {
                    continue;
                }
                Object familyEntry = MCAReflectionHelper.invoke(saveData, "getFamilyEntry");
                Object resolvedName = familyEntry == null ? null : MCAReflectionHelper.invoke(familyEntry, "getName");
                if (resolvedName instanceof String name && !name.isBlank()) {
                    return name.trim();
                }
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "MCAPlayerBridge#getDialogueName:familyTree:" + className,
                        "Failed to resolve MCA dialogue player name from the family tree using {} ({})",
                        className,
                        t.toString()
                );
            }
        }

        return player.getName().getString();
    }


    private static String resolveMessengerName(ServerPlayer player) {
        String[] classNames = new String[] {
                "fabric.net.mca.entity.ai.Messenger"
        };

        for (String className : classNames) {
            try {
                Class<?> messengerClass = Class.forName(className);
                for (Method method : messengerClass.getMethods()) {
                    if (!"getName".equals(method.getName())
                            || !Modifier.isStatic(method.getModifiers())
                            || method.getParameterCount() != 1
                            || !method.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                        continue;
                    }

                    Object resolved = method.invoke(null, player);
                    if (resolved instanceof String name && !name.isBlank()) {
                        return name.trim();
                    }
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "MCAPlayerBridge#getDialogueName:messenger:" + className,
                        "Failed to resolve MCA dialogue player name through {} ({})",
                        className,
                        t.toString()
                );
            }
        }

        return null;
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

                Object saveDataGender = MCAReflectionHelper.invoke(saveData, "getGender");
                GenderResult saveDataResult = resolveGenderObject(saveDataGender);
                if (saveDataResult.assigned()) {
                    return saveDataResult.isFemale();
                }

                Object entityDataObject = MCAReflectionHelper.invoke(saveData, "getEntityData");
                if (entityDataObject instanceof CompoundTag entityData) {
                    GenderResult trackedResult = resolveGenderFromEntityData(entityData);
                    if (trackedResult.assigned()) {
                        return trackedResult.isFemale();
                    }
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

        GenderResult familyTreeResult = resolveFamilyTreeGender(level, player.getUUID());
        if (familyTreeResult.assigned()) {
            return familyTreeResult.isFemale();
        }

        return false;
    }

    private static GenderResult resolveGenderFromEntityData(CompoundTag entityData) {
        if (entityData == null) {
            return GenderResult.unassigned();
        }

        if (entityData.contains("Gender")) {
            GenderResult result = resolveGenderId(entityData.getInt("Gender"));
            if (result.assigned()) {
                return result;
            }
        }

        if (entityData.contains("gender")) {
            GenderResult result = resolveGenderId(entityData.getInt("gender"));
            if (result.assigned()) {
                return result;
            }
        }

        return GenderResult.unassigned();
    }

    private static GenderResult resolveGenderId(int id) {
        for (String className : MCA_GENDER_CLASSES) {
            try {
                Class<?> genderClass = Class.forName(className);
                Object gender = MCAReflectionHelper.invokeStatic(
                        genderClass,
                        "byId",
                        new Class<?>[] {int.class},
                        id
                );

                GenderResult result = resolveGenderObject(gender);
                if (result.assigned()) {
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        if (id == 1) {
            return GenderResult.female();
        }

        if (id == 0) {
            return GenderResult.male();
        }

        return GenderResult.unassigned();
    }

    private static GenderResult resolveGenderObject(Object gender) {
        if (gender == null) {
            return GenderResult.unassigned();
        }

        Object dataName = MCAReflectionHelper.invoke(gender, "getDataName");
        String resolved = dataName instanceof String
                ? (String) dataName
                : String.valueOf(gender);

        if ("female".equalsIgnoreCase(resolved) || "FEMALE".equalsIgnoreCase(resolved)) {
            return GenderResult.female();
        }

        if ("male".equalsIgnoreCase(resolved) || "MALE".equalsIgnoreCase(resolved)) {
            return GenderResult.male();
        }

        if ("neutral".equalsIgnoreCase(resolved) || "NEUTRAL".equalsIgnoreCase(resolved)) {
            return GenderResult.male();
        }

        return GenderResult.unassigned();
    }

    private static GenderResult resolveFamilyTreeGender(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null) {
            return GenderResult.unassigned();
        }

        for (String className : MCAReflectionHelper.MCA_FAMILY_TREE_CLASSES) {
            try {
                Class<?> familyTreeClass = Class.forName(className);
                Object familyTree = MCAReflectionHelper.invokeStatic(
                        familyTreeClass,
                        "get",
                        new Class<?>[] {ServerLevel.class},
                        level
                );

                if (familyTree == null) {
                    continue;
                }

                Object optional = MCAReflectionHelper.invoke(
                        familyTree,
                        "getOrEmpty",
                        new Class<?>[] {UUID.class},
                        playerId
                );

                if (optional instanceof Optional<?> nodeOptional) {
                    Object node = nodeOptional.orElse(null);
                    if (node == null) {
                        continue;
                    }

                    Object gender = MCAReflectionHelper.invoke(node, "gender");
                    GenderResult result = resolveGenderObject(gender);
                    if (result.assigned()) {
                        return result;
                    }
                }
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "MCAPlayerBridge#resolveFamilyTreeGender:" + className,
                        "Failed to query MCA FamilyTreeNode gender class {} ({})",
                        className,
                        t.toString()
                );
            }
        }

        return GenderResult.unassigned();
    }

    private static Object getPlayerSaveData(ServerLevel level, ServerPlayer player, String className) throws Exception {
        Class<?> playerSaveDataClass = Class.forName(className);

        Object saveData = MCAReflectionHelper.invokeStatic(
                playerSaveDataClass,
                "get",
                new Class<?>[] {ServerPlayer.class},
                player
        );

        if (saveData == null) {
            saveData = MCAReflectionHelper.invokeStatic(
                    playerSaveDataClass,
                    "get",
                    new Class<?>[] {player.getClass()},
                    player
            );
        }

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

    private record GenderResult(boolean assigned, boolean isFemale) {

        static GenderResult female() {
            return new GenderResult(true, true);
        }

        static GenderResult male() {
            return new GenderResult(true, false);
        }

        static GenderResult unassigned() {
            return new GenderResult(false, false);
        }
    }
}