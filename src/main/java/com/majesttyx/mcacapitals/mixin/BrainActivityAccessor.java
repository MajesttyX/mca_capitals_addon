package com.majesttyx.mcacapitals.mixin;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(Brain.class)
public interface BrainActivityAccessor {

    @Accessor("availableBehaviorsByPriority")
    Map<?, ?> mcacapitals$getAvailableBehaviorsByPriority();

    @Accessor("activityRequirements")
    Map<Activity, ?> mcacapitals$getActivityRequirements();

    @Accessor("activityMemoriesToEraseWhenStopped")
    Map<Activity, ?> mcacapitals$getActivityMemoriesToEraseWhenStopped();
}