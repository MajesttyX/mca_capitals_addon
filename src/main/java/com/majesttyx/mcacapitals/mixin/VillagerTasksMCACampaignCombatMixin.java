package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalCampaignCombatService;
import fabric.net.mca.entity.VillagerEntityMCA;
import fabric.net.mca.entity.ai.brain.VillagerTasksMCA;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = VillagerTasksMCA.class,
        remap = false
)
public abstract class VillagerTasksMCACampaignCombatMixin {

    @Inject(
            method = "guardTooHurt(Lfabric/net/mca/entity/VillagerEntityMCA;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$keepCampaignGuardFighting(
            VillagerEntityMCA villager,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (villager.level() instanceof ServerLevel level
                && CapitalCampaignCombatService.isActiveCombatant(
                level,
                villager.getUUID()
        )) {
            cir.setReturnValue(false);
        }
    }
}
