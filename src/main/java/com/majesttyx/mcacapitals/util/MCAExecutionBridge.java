package com.majesttyx.mcacapitals.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Supplier;

public final class MCAExecutionBridge {
    private static final String[] PROFESSION_CLASS_NAMES = {
            "fabric.net.mca.ProfessionsMCA"
    };

    private MCAExecutionBridge() {
    }

    public static boolean markForExecution(ServerLevel level, UUID villagerId) {
        VillagerProfession outlaw = resolveOutlawProfession();
        if (outlaw == null) {
            return false;
        }
        return setProfession(level, villagerId, outlaw);
    }

    public static boolean clearExecutionMark(ServerLevel level, UUID villagerId) {
        if (!isMarkedForExecution(level, villagerId)) {
            return false;
        }

        return setProfession(level, villagerId, VillagerProfession.NONE);
    }

    public static boolean isMarkedForExecution(ServerLevel level, UUID villagerId) {
        if (level == null || villagerId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, villagerId);
        if (entity == null) {
            return false;
        }

        VillagerProfession current = getProfession(entity);
        VillagerProfession outlaw = resolveOutlawProfession();

        return current != null && outlaw != null && current.equals(outlaw);
    }

    private static boolean setProfession(ServerLevel level, UUID villagerId, VillagerProfession profession) {
        if (level == null || villagerId == null || profession == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, villagerId);
        if (entity == null) {
            return false;
        }

        try {
            Method setProfession = entity.getClass().getMethod("setProfession", VillagerProfession.class);
            setProfession.invoke(entity, profession);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static VillagerProfession getProfession(Entity entity) {
        if (entity == null) {
            return null;
        }

        try {
            Method getProfession = entity.getClass().getMethod("getProfession");
            Object result = getProfession.invoke(entity);
            if (result instanceof VillagerProfession profession) {
                return profession;
            }
        } catch (Throwable ignored) {
        }

        try {
            return entity instanceof net.minecraft.world.entity.npc.Villager villager
                    ? villager.getVillagerData().getProfession()
                    : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static VillagerProfession resolveOutlawProfession() {
        for (String className : PROFESSION_CLASS_NAMES) {
            VillagerProfession profession = resolveOutlawProfession(className);
            if (profession != null) {
                return profession;
            }
        }

        return null;
    }

    private static VillagerProfession resolveOutlawProfession(String className) {
        try {
            Class<?> professionsClass = Class.forName(className);
            Field outlawField = professionsClass.getField("OUTLAW");
            Object raw = outlawField.get(null);
            Object resolved = unwrap(raw);

            if (resolved instanceof VillagerProfession profession) {
                return profession;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object unwrap(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof VillagerProfession) {
            return value;
        }

        if (value instanceof Supplier<?> supplier) {
            try {
                return supplier.get();
            } catch (Throwable ignored) {
            }
        }

        try {
            Method get = value.getClass().getMethod("get");
            return get.invoke(value);
        } catch (Throwable ignored) {
        }

        try {
            Method valueMethod = value.getClass().getMethod("value");
            return valueMethod.invoke(value);
        } catch (Throwable ignored) {
        }

        return null;
    }
}
