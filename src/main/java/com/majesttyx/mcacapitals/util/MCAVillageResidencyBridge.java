package com.majesttyx.mcacapitals.util;

import forge.net.mca.entity.VillagerEntityMCA;
import forge.net.mca.server.world.data.Village;
import forge.net.mca.server.world.data.VillageManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

final class MCAVillageResidencyBridge {

    private static Field villageParameterField;
    private static Method setTrackedValueMethod;

    private MCAVillageResidencyBridge() {
    }

    static boolean forceVillageResidency(
            ServerLevel level,
            UUID villagerId,
            int villageId
    ) {
        if (level == null || villagerId == null) {
            return false;
        }

        Entity entity = MCAEntityBridge.findLoadedMCAVillagerByUuid(level, villagerId);
        if (!(entity instanceof VillagerEntityMCA villager) || !villager.isAlive()) {
            return false;
        }

        Village village = VillageManager.get(level)
                .getOrEmpty(villageId)
                .orElse(null);
        if (village == null) {
            return false;
        }

        try {
            Object villageParameter = resolveVillageParameter(villager.getResidency().getClass());
            Method setTrackedValue = resolveSetTrackedValue(villager.getClass());
            if (villageParameter == null || setTrackedValue == null) {
                return false;
            }

            villager.getResidency().leaveHome();
            setTrackedValue.invoke(villager, villageParameter, villageId);
            village.updateResident(villager);
            village.markDirty();

            return villager.getResidency()
                    .getHomeVillage()
                    .map(home -> home.getId() == villageId)
                    .orElse(false);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Object resolveVillageParameter(Class<?> residencyClass)
            throws ReflectiveOperationException {
        if (villageParameterField == null) {
            Field field = residencyClass.getDeclaredField("VILLAGE");
            field.setAccessible(true);
            villageParameterField = field;
        }

        return villageParameterField.get(null);
    }

    private static Method resolveSetTrackedValue(Class<?> villagerClass) {
        if (setTrackedValueMethod != null) {
            return setTrackedValueMethod;
        }

        for (Method method : villagerClass.getMethods()) {
            if (method.getName().equals("setTrackedValue")
                    && method.getParameterCount() == 2) {
                method.setAccessible(true);
                setTrackedValueMethod = method;
                return method;
            }
        }

        return null;
    }
}