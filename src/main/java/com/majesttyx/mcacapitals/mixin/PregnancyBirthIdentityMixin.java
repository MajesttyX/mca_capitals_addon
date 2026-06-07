package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.BirthIdentityService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.mca.entity.ai.Pregnancy", remap = false)
public class PregnancyBirthIdentityMixin {

    @Inject(
            method = "createChild(Lnet/mca/entity/ai/relationship/Gender;Lnet/mca/entity/VillagerEntityMCA;)Lnet/mca/entity/VillagerEntityMCA;",
            at = @At("RETURN"),
            remap = false
    )
    private void mcacapitals$applyBirthIdentity(
            @Coerce Object gender,
            @Coerce Object otherParent,
            CallbackInfoReturnable<Object> cir
    ) {
        if (!(cir.getReturnValue() instanceof Entity child)) {
            return;
        }

        if (!(child.level() instanceof ServerLevel level)) {
            return;
        }

        Entity firstParent = resolveSelfParent();
        Entity secondParent = otherParent instanceof Entity entity ? entity : null;

        BirthIdentityService.applyBirthIdentity(level, child, firstParent, secondParent);
    }

    private Entity resolveSelfParent() {
        try {
            Object target = this;
            Object mother = target.getClass().getDeclaredField("mother").get(target);
            if (mother instanceof Entity entity) {
                return entity;
            }
        } catch (Throwable ignored) {
        }

        try {
            Object target = this;
            Object entity = target.getClass().getDeclaredField("entity").get(target);
            if (entity instanceof Entity villager) {
                return villager;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}