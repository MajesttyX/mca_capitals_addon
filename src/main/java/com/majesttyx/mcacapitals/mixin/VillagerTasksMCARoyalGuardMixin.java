package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.ai.RoyalGuardFollowGoal;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "net.mca.entity.ai.brain.VillagerTasksMCA", remap = false)
public abstract class VillagerTasksMCARoyalGuardMixin {

    @Shadow(remap = false)
    @Final
    private PathfinderMob entity;

    @Redirect(
            method = "initCombatTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V"
            )
    )
    private void mcacapitals$replaceRoyalGuardCombatFollowGoal(
            net.minecraft.world.entity.ai.goal.GoalSelector selector,
            int priority,
            Goal goal
    ) {
        if (entity == null) {
            selector.addGoal(priority, goal);
            return;
        }

        CapitalRecord capital = CapitalManager.getCapitalForResident(entity.getUUID());
        if (capital == null || !capital.isRoyalGuard(entity.getUUID())) {
            selector.addGoal(priority, goal);
            return;
        }

        if (!isFollowGoal(goal)) {
            selector.addGoal(priority, goal);
            return;
        }

        selector.addGoal(priority, new RoyalGuardFollowGoal(entity, capital.getCapitalId()));
    }

    private static boolean isFollowGoal(Goal goal) {
        if (goal == null) {
            return false;
        }

        String name = goal.getClass().getName().toLowerCase();
        return name.contains("follow") || name.contains("guard");
    }

    public static void refreshCombatTasks(Object tasks) {
        if (tasks == null) {
            return;
        }

        try {
            Class<?> tasksClass = Class.forName("net.mca.entity.ai.brain.VillagerTasksMCA");

            if (!tasksClass.isInstance(tasks)) {
                return;
            }

            Method method = tasksClass.getDeclaredMethod("initCombatTasks");
            method.setAccessible(true);
            method.invoke(tasks);
        } catch (Throwable ignored) {
        }
    }

    public static Object getTasks(Object villager) {
        if (villager == null) {
            return null;
        }

        Class<?> current = villager.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField("tasks");
                field.setAccessible(true);
                return field.get(villager);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }
}