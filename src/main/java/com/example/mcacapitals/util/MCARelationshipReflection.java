package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class MCARelationshipReflection {

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

    private MCARelationshipReflection() {
    }

    static Object getPlayerSaveData(ServerPlayer player) {
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

    static int getBouquetHeartsRequirement() {
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

    static boolean booleanCall(Object target, String methodName, Object... args) {
        Object value = invokeCompatible(target, methodName, args);
        return value instanceof Boolean b && b;
    }

    static Object invokeStaticCompatible(Class<?> type, String methodName, Object... args) {
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

    static Object invokeCompatible(Object target, String methodName, Object... args) {
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
}