package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
public abstract class EntitySprintingRoyalGuardMixin {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void mcacapitals$disableRoyalGuardSprinting(boolean sprinting, CallbackInfo ci) {
        if (!sprinting) {
            return;
        }

        Entity self = (Entity) (Object) this;
        if (!(self instanceof VillagerEntityMCA)
                || !(self.level() instanceof ServerLevel)) {
            return;
        }

        if (CapitalManager.isRoyalGuard(self.getUUID())) {
            ci.cancel();
        }
    }
}