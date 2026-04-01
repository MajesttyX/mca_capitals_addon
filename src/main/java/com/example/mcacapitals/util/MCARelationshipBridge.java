package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class MCARelationshipBridge {

    private static final String[] MCA_PLAYER_SAVE_DATA_CLASSES = new String[] {
            "net.mca.server.world.data.PlayerSaveData",
            "forge.net.mca.server.world.data.PlayerSaveData",
            "fabric.net.mca.server.world.data.PlayerSaveData",
            "quilt.net.mca.server.world.data.PlayerSaveData"
    };

    private static final String[] MCA_CONFIG_CLASSES = new String[] {
            "net.mca.Config",
            "forge.net.mca.Config",
            "fabric.net.mca.Config",
            "quilt.net.mca.Config"
    };

    private MCARelationshipBridge() {
    }

    public static BetrothalResult promise(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return BetrothalResult.failure("That betrothal could not be arranged.");
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return BetrothalResult.failure("Only an MCA noble may be chosen for betrothal.");
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(player.serverLevel(), villagerEntity.getUUID())) {
            return BetrothalResult.failure("Only a teen or adult noble may be chosen for betrothal.");
        }

        Object playerData = getPlayerSaveData(player);
        if (playerData == null) {
            return BetrothalResult.failure("The realm could not consult MCA's courtship records.");
        }

        Object relationships = MCAReflectionHelper.invoke(villagerEntity, "getRelationships");
        if (relationships == null) {
            return BetrothalResult.failure("That villager's relationship state could not be read.");
        }

        if (booleanCall(relationships, "isMarriedTo", player)) {
            return BetrothalResult.failure("That noble is already married to you.");
        }

        if (booleanCall(relationships, "isMarried")) {
            return BetrothalResult.failure("That noble is already married.");
        }

        if (booleanCall(relationships, "isEngagedWith", player)) {
            return BetrothalResult.failure("That noble is already engaged to you.");
        }

        if (booleanCall(relationships, "isPromisedTo", player)) {
            return BetrothalResult.failure("That noble is already betrothed to you.");
        }

        if (booleanCall(relationships, "isEngaged")) {
            return BetrothalResult.failure("That noble is already engaged.");
        }

        if (booleanCall(relationships, "isPromised")) {
            return BetrothalResult.failure("That noble is already promised elsewhere.");
        }

        if (booleanCall(playerData, "isMarried")) {
            return BetrothalResult.failure("You are already married.");
        }

        int heartsRequired = getBouquetHeartsRequirement();
        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(player.serverLevel(), villagerEntity.getUUID(), player.getUUID());
        if (hearts < heartsRequired) {
            return BetrothalResult.failure("That noble does not yet return your affection strongly enough.");
        }

        Object attracted = invokeCompatible(villagerEntity, "canBeAttractedTo", playerData);
        if (attracted instanceof Boolean b && !b) {
            return BetrothalResult.failure("That noble would not accept such a match.");
        }

        invokeCompatible(playerData, "promise", villagerEntity);
        invokeCompatible(relationships, "promise", player);

        Object brain = MCAReflectionHelper.invoke(villagerEntity, "getVillagerBrain");
        if (brain != null) {
            invokeCompatible(brain, "modifyMoodValue", 5);
        }

        return BetrothalResult.ok();
    }

    public static BetrothalResult promiseVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return BetrothalResult.failure("That betrothal recommendation could not be arranged.");
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return BetrothalResult.failure("Only MCA villagers may be joined by recommendation.");
        }

        if (firstVillager.getUUID().equals(secondVillager.getUUID())) {
            return BetrothalResult.failure("A villager cannot be recommended for betrothal to themself.");
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");

        if (firstRelationships == null || secondRelationships == null) {
            return BetrothalResult.failure("One of the villagers' relationship records could not be read.");
        }

        if (booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                || booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return BetrothalResult.failure("Those villagers are already married to one another.");
        }

        if (booleanCall(firstRelationships, "isMarried") || booleanCall(secondRelationships, "isMarried")) {
            return BetrothalResult.failure("One of those villagers is already married.");
        }

        if (booleanCall(firstRelationships, "isEngagedWith", secondVillager)
                || booleanCall(secondRelationships, "isEngagedWith", firstVillager)) {
            return BetrothalResult.failure("Those villagers are already engaged to one another.");
        }

        if (booleanCall(firstRelationships, "isPromisedTo", secondVillager)
                || booleanCall(secondRelationships, "isPromisedTo", firstVillager)) {
            return BetrothalResult.failure("Those villagers are already betrothed to one another.");
        }

        if (booleanCall(firstRelationships, "isEngaged") || booleanCall(secondRelationships, "isEngaged")) {
            return BetrothalResult.failure("One of those villagers is already engaged.");
        }

        if (booleanCall(firstRelationships, "isPromised") || booleanCall(secondRelationships, "isPromised")) {
            return BetrothalResult.failure("One of those villagers is already promised elsewhere.");
        }

        Object firstAttracted = invokeCompatible(firstVillager, "canBeAttractedTo", secondVillager);
        if (firstAttracted instanceof Boolean b && !b) {
            return BetrothalResult.failure("The first villager would not accept such a match.");
        }

        Object secondAttracted = invokeCompatible(secondVillager, "canBeAttractedTo", firstVillager);
        if (secondAttracted instanceof Boolean b && !b) {
            return BetrothalResult.failure("The second villager would not accept such a match.");
        }

        invokeCompatible(firstRelationships, "promise", secondVillager);
        invokeCompatible(secondRelationships, "promise", firstVillager);

        Object firstBrain = MCAReflectionHelper.invoke(firstVillager, "getVillagerBrain");
        if (firstBrain != null) {
            invokeCompatible(firstBrain, "modifyMoodValue", 5);
        }

        Object secondBrain = MCAReflectionHelper.invoke(secondVillager, "getVillagerBrain");
        if (secondBrain != null) {
            invokeCompatible(secondBrain, "modifyMoodValue", 5);
        }

        return BetrothalResult.ok();
    }

    public static boolean areVillagersBetrothedToEachOther(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return false;
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");
        if (firstRelationships == null || secondRelationships == null) {
            return false;
        }

        if (booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                || booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return false;
        }

        boolean promised =
                (booleanCall(firstRelationships, "isPromisedTo", secondVillager)
                        && booleanCall(secondRelationships, "isPromisedTo", firstVillager))
                        || (booleanCall(firstRelationships, "isEngagedWith", secondVillager)
                        && booleanCall(secondRelationships, "isEngagedWith", firstVillager));

        return promised;
    }

    public static BetrothalResult marryVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return BetrothalResult.failure("That marriage could not be arranged.");
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return BetrothalResult.failure("Only MCA villagers may be married by this bridge.");
        }

        if (!areVillagersBetrothedToEachOther(firstVillager, secondVillager)) {
            return BetrothalResult.failure("Those villagers are not currently betrothed to one another.");
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");
        if (firstRelationships == null || secondRelationships == null) {
            return BetrothalResult.failure("One of the villagers' relationship records could not be read.");
        }

        if (booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                && booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return BetrothalResult.ok();
        }

        tryRelationshipStep(firstRelationships, firstVillager, secondVillager, "engage");
        tryRelationshipStep(secondRelationships, secondVillager, firstVillager, "engage");

        if (booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                && booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return BetrothalResult.ok();
        }

        tryRelationshipStep(firstRelationships, firstVillager, secondVillager, "marry");
        tryRelationshipStep(secondRelationships, secondVillager, firstVillager, "marry");

        if (booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                && booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return BetrothalResult.ok();
        }

        return BetrothalResult.failure("Those villagers could not yet be advanced from betrothal to marriage.");
    }

    public static boolean isActuallyMarried(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return false;
        }

        Object playerData = getPlayerSaveData(player);
        if (playerData == null) {
            return false;
        }

        Object relationships = MCAReflectionHelper.invoke(villagerEntity, "getRelationships");
        if (relationships == null) {
            return false;
        }

        if (!booleanCall(playerData, "isMarried")) {
            return false;
        }

        return booleanCall(relationships, "isMarriedTo", player);
    }

    public static boolean isActuallyMarriedToPlayer(ServerPlayer player, UUID villagerId) {
        if (player == null || villagerId == null) {
            return false;
        }

        Entity villager = MCAIntegrationBridge.getEntityByUuid(player.serverLevel(), villagerId);
        return isActuallyMarried(player, villager);
    }

    private static void tryRelationshipStep(Object relationships, Entity self, Entity other, String methodName) {
        if (relationships == null || self == null || other == null || methodName == null) {
            return;
        }

        invokeCompatible(relationships, methodName, other);
        invokeCompatible(relationships, methodName, other.getUUID());
        invokeCompatible(self, methodName, other);
        invokeCompatible(self, methodName, other.getUUID());
    }

    private static Object getPlayerSaveData(ServerPlayer player) {
        for (String className : MCA_PLAYER_SAVE_DATA_CLASSES) {
            try {
                Class<?> type = Class.forName(className);
                Object value = invokeStaticCompatible(type, "get", player);
                if (value != null) {
                    return value;
                }
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "MCARelationshipBridge#getPlayerSaveData:" + className,
                        "Failed to resolve MCA PlayerSaveData {} ({})",
                        className,
                        t.toString()
                );
            }
        }
        return null;
    }

    private static int getBouquetHeartsRequirement() {
        for (String className : MCA_CONFIG_CLASSES) {
            try {
                Class<?> type = Class.forName(className);
                Object config = invokeStaticCompatible(type, "getInstance");
                if (config == null) {
                    continue;
                }

                try {
                    Field field = config.getClass().getField("bouquetHeartsRequirement");
                    Object value = field.get(config);
                    if (value instanceof Integer i) {
                        return i;
                    }
                } catch (Throwable ignored) {
                }

                Object getterValue = invokeCompatible(config, "getBouquetHeartsRequirement");
                if (getterValue instanceof Integer i) {
                    return i;
                }
            } catch (Throwable t) {
                MCAReflectionHelper.warnOnce(
                        "MCARelationshipBridge#getBouquetHeartsRequirement:" + className,
                        "Failed to resolve MCA bouquet hearts requirement from {} ({})",
                        className,
                        t.toString()
                );
            }
        }

        return 10;
    }

    private static boolean booleanCall(Object target, String methodName, Object... args) {
        Object value = invokeCompatible(target, methodName, args);
        return value instanceof Boolean b && b;
    }

    private static Object invokeStaticCompatible(Class<?> type, String methodName, Object... args) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (method.getParameterCount() != args.length) {
                continue;
            }
            if (!parametersMatch(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                return method.invoke(null, args);
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object invokeCompatible(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (method.getParameterCount() != args.length) {
                continue;
            }
            if (!parametersMatch(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                return method.invoke(target, args);
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }

            Class<?> parameterType = wrap(parameterTypes[i]);
            Class<?> argType = wrap(arg.getClass());

            if (!parameterType.isAssignableFrom(argType)) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }

    public record BetrothalResult(boolean success, String message) {
        public static BetrothalResult ok() {
            return new BetrothalResult(true, "");
        }

        public static BetrothalResult failure(String message) {
            return new BetrothalResult(false, message);
        }
    }
}