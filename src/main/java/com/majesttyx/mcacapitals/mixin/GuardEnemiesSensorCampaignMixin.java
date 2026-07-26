package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalCampaignService;
import net.conczin.mca.entity.ai.brain.sensor.GuardEnemiesSensor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuardEnemiesSensor.class, remap = false)
public abstract class GuardEnemiesSensorCampaignMixin {

    private static final double CAMPAIGN_ENEMY_RANGE_SQR =
            96.0D * 96.0D;

    @Inject(
            method = "isGuardEnemy",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$recognizeCampaignEnemy(
            LivingEntity candidate,
            LivingEntity guard,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(guard.level()
                instanceof ServerLevel level)) {
            return;
        }

        if (CapitalCampaignService
                .areOpposingCampaignCombatants(
                        level,
                        candidate.getUUID(),
                        guard.getUUID()
                )) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "isWithinGuardEnemyRange",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$widenCampaignEnemyRange(
            LivingEntity candidate,
            LivingEntity guard,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(guard.level()
                instanceof ServerLevel level)) {
            return;
        }

        if (CapitalCampaignService
                .areOpposingCampaignCombatants(
                        level,
                        candidate.getUUID(),
                        guard.getUUID()
                )
                && guard.distanceToSqr(candidate)
                <= CAMPAIGN_ENEMY_RANGE_SQR) {
            cir.setReturnValue(true);
        }
    }
}