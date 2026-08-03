package com.majesttyx.mcacapitals.mixin;

import com.google.common.collect.ImmutableList;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Pseudo
@Mixin(targets = "net.conczin.mca.entity.ai.brain.VillagerTasksMCA", remap = false)
public class VillagerTasksMCARoyalGuardMixin {

    @Inject(
            method = "initializeTasks",
            at = @At("TAIL"),
            remap = false
    )
    private static void mcacapitals$overrideRoyalGuardWorkPackage(
            @Coerce Object villagerObj,
            @Coerce Object brainObj,
            CallbackInfoReturnable<Object> cir
    ) {
        if (!(villagerObj instanceof Entity entity)) {
            return;
        }

        UUID villagerId = entity.getUUID();
        if (!isRoyalGuard(villagerId)) {
            return;
        }

        Object brain = cir.getReturnValue();
        if (brain == null) {
            brain = brainObj;
        }

        if (brain == null) {
            return;
        }

        Object stayingPackage = resolveStayingPackage();
        if (!(stayingPackage instanceof ImmutableList<?> taskList)) {
            return;
        }

        replaceActivityPackage(brain, Activity.WORK, taskList);
        replaceActivityPackage(brain, Activity.RAID, taskList);
        refreshActivities(brain, entity);
    }

    private static boolean isRoyalGuard(UUID villagerId) {
        if (villagerId == null) {
            return false;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.isRoyalGuard(villagerId)) {
                return true;
            }
        }

        return false;
    }

    private static Object resolveStayingPackage() {
        try {
            Class<?> tasksClass = Class.forName("net.conczin.mca.entity.ai.brain.VillagerTasksMCA");
            Method method = tasksClass.getMethod("getStayingPackage");
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void replaceActivityPackage(Object brain, Activity activity, ImmutableList<?> taskList) {
        if (brain == null || activity == null || taskList == null) {
            return;
        }

        removeExistingActivityPackage(brain, activity);
        addActivityPackage(brain, activity, taskList);
    }

    private static void removeExistingActivityPackage(Object brain, Activity activity) {
        removeActivityFromAvailableBehaviors(brain, activity);
        removeActivityFromMapField(brain, "activityRequirements", activity);
        removeActivityFromMapField(brain, "activityMemoriesToEraseWhenStopped", activity);
    }

    private static void removeActivityFromAvailableBehaviors(Object brain, Activity activity) {
        try {
            Field field = findField(brain.getClass(), "availableBehaviorsByPriority");
            if (field == null) {
                return;
            }

            field.setAccessible(true);
            Object value = field.get(brain);
            if (!(value instanceof Map<?, ?> outerMap)) {
                return;
            }

            for (Object inner : outerMap.values()) {
                if (inner instanceof Map<?, ?> rawMap) {
                    ((Map<?, ?>) rawMap).remove(activity);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void removeActivityFromMapField(Object brain, String fieldName, Activity activity) {
        try {
            Field field = findField(brain.getClass(), fieldName);
            if (field == null) {
                return;
            }

            field.setAccessible(true);
            Object value = field.get(brain);
            if (value instanceof Map<?, ?> rawMap) {
                ((Map<?, ?>) rawMap).remove(activity);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addActivityPackage(Object brain, Activity activity, ImmutableList<?> taskList) {
        try {
            Method method = brain.getClass().getMethod("addActivity", Activity.class, ImmutableList.class);
            method.invoke(brain, activity, taskList);
        } catch (Throwable ignored) {
        }
    }

    private static void refreshActivities(Object brain, Entity entity) {
        if (brain == null || entity == null) {
            return;
        }

        try {
            Method method = brain.getClass().getMethod("updateActivityFromSchedule", long.class, long.class);
            method.invoke(brain, entity.level().getDayTime(), entity.level().getGameTime());
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}