package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalCampaignService;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Residency;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Residency.class, remap = false)
public abstract class ResidencyCampaignMixin {

    @Shadow
    @Final
    private VillagerEntityMCA entity;

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$skipCampaignResidencyTick(
            CallbackInfo ci
    ) {
        if (!(entity.level()
                instanceof ServerLevel level)) {
            return;
        }

        if (CapitalCampaignService
                .isCampaignAttacker(
                        level,
                        entity.getUUID()
                )) {
            ci.cancel();
        }
    }
}