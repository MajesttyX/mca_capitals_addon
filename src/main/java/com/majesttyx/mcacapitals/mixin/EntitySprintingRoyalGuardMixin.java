package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntitySprintingRoyalGuardMixin {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void mcacapitals$disableRoyalGuardSprinting(boolean sprinting, CallbackInfo ci) {
        if (!sprinting) {
            return;
        }

        Entity self = (Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel)) {
            return;
        }

        UUID entityId = self.getUUID();
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.isRoyalGuard(entityId)) {
                ci.cancel();
                return;
            }
        }
    }
}