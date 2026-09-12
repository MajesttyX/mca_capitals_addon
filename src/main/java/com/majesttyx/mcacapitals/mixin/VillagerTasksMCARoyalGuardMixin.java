package com.majesttyx.mcacapitals.mixin;

import com.google.common.collect.ImmutableList;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import forge.net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = VillagerTasksMCA.class, remap = false)
public abstract class VillagerTasksMCARoyalGuardMixin {

    @Inject(method = "initializeTasks", at = @At("TAIL"), remap = false)
    private static void mcacapitals$overrideRoyalGuardWorkPackage(
            VillagerEntityMCA villager,
            Brain<VillagerEntityMCA> inputBrain,
            CallbackInfoReturnable<Brain<VillagerEntityMCA>> cir
    ) {
        if (villager == null || !CapitalManager.isRoyalGuard(villager.getUUID())) {
            return;
        }

        Brain<VillagerEntityMCA> brain = cir.getReturnValue();
        if (brain == null) {
            brain = inputBrain;
        }
        if (brain == null) {
            return;
        }

        ImmutableList<?> stayingPackage = VillagerTasksMCA.getStayingPackage();
        if (stayingPackage == null) {
            return;
        }

        replaceActivityPackage(brain, Activity.WORK, stayingPackage);
        replaceActivityPackage(brain, Activity.RAID, stayingPackage);
        brain.updateActivityFromSchedule(
                villager.level().getDayTime(),
                villager.level().getGameTime()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void replaceActivityPackage(
            Brain<VillagerEntityMCA> brain,
            Activity activity,
            ImmutableList<?> taskList
    ) {
        BrainActivityAccessor accessor = (BrainActivityAccessor) (Object) brain;
        Map<?, ?> priorities = accessor.mcacapitals$getAvailableBehaviorsByPriority();

        for (Object nested : priorities.values()) {
            if (nested instanceof Map<?, ?> activityMap) {
                ((Map) activityMap).remove(activity);
            }
        }

        accessor.mcacapitals$getActivityRequirements().remove(activity);
        accessor.mcacapitals$getActivityMemoriesToEraseWhenStopped().remove(activity);
        brain.addActivity(activity, (ImmutableList) taskList);
    }
}