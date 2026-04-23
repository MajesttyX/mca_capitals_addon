package com.example.mcacapitals.mixin;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.UUID;

@Pseudo
@Mixin(targets = "forge.net.mca.entity.ai.brain.VillagerTasksMCA", remap = false)
public class VillagerTasksMCARoyalGuardMixin {

    @Inject(method = "initializeTasks", at = @At("TAIL"), remap = false)
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

        Object minimalLookPackage = resolveMinimalLookPackage();
        if (!(minimalLookPackage instanceof ImmutableList<?> taskList)) {
            return;
        }

        setTaskList(brain, Activity.WORK, taskList);
        setTaskList(brain, Activity.RAID, taskList);
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

    private static Object resolveMinimalLookPackage() {
        try {
            Class<?> tasksClass = Class.forName("forge.net.mca.entity.ai.brain.VillagerTasksMCA");
            Method method = tasksClass.getDeclaredMethod("getMinimalLookBehavior");
            method.setAccessible(true);
            Object pair = method.invoke(null);
            if (pair == null) {
                return null;
            }
            return ImmutableList.of((Pair<?, ?>) pair);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setTaskList(Object brain, Activity activity, ImmutableList<?> taskList) {
        try {
            for (Method method : brain.getClass().getMethods()) {
                if (!method.getName().equals("setTaskList") || method.getParameterCount() != 2) {
                    continue;
                }

                Class<?> first = method.getParameterTypes()[0];
                Class<?> second = method.getParameterTypes()[1];

                if (first.isAssignableFrom(Activity.class) && second.isAssignableFrom(taskList.getClass())) {
                    method.invoke(brain, activity, taskList);
                    return;
                }

                if (first.isAssignableFrom(Activity.class)) {
                    method.invoke(brain, activity, taskList);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void refreshActivities(Object brain, Entity entity) {
        try {
            for (Method method : brain.getClass().getMethods()) {
                if (!method.getName().equals("refreshActivities") || method.getParameterCount() != 2) {
                    continue;
                }
                method.invoke(brain, entity.level().getDayTime(), entity.level().getGameTime());
                return;
            }
        } catch (Throwable ignored) {
        }
    }
}