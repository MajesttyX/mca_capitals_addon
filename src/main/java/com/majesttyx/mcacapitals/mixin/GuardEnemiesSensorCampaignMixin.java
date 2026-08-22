package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalCampaignService;
import fabric.net.conczin.mca.entity.VillagerEntityMCA;
import fabric.net.conczin.mca.entity.ai.brain.sensor.GuardEnemiesSensor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Optional;

@Mixin(value = GuardEnemiesSensor.class, remap = false)
public abstract class GuardEnemiesSensorCampaignMixin {

    private static final double CAMPAIGN_ENEMY_RANGE =
            96.0D;

    private static final double CAMPAIGN_ENEMY_RANGE_SQR =
            CAMPAIGN_ENEMY_RANGE * CAMPAIGN_ENEMY_RANGE;

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
            method = "getNearestHostile",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$findCampaignEnemyBeyondLegacyRange(
            VillagerEntityMCA guard,
            CallbackInfoReturnable<Optional<LivingEntity>> cir
    ) {
        if (cir.getReturnValue().isPresent()
                || !(guard.level()
                instanceof ServerLevel level)) {
            return;
        }

        Optional<LivingEntity> campaignEnemy =
                level.getEntitiesOfClass(
                                LivingEntity.class,
                                guard.getBoundingBox()
                                        .inflate(CAMPAIGN_ENEMY_RANGE),
                                candidate ->
                                        candidate != null
                                                && candidate != guard
                                                && candidate.isAlive()
                                                && !candidate.isRemoved()
                                                && guard.distanceToSqr(candidate)
                                                <= CAMPAIGN_ENEMY_RANGE_SQR
                                                && CapitalCampaignService
                                                .areOpposingCampaignCombatants(
                                                        level,
                                                        candidate.getUUID(),
                                                        guard.getUUID()
                                                )
                        )
                        .stream()
                        .filter(candidate ->
                                guard.getSensing()
                                        .hasLineOfSight(candidate)
                        )
                        .min(
                                Comparator.comparingDouble(
                                        guard::distanceToSqr
                                )
                        );

        if (campaignEnemy.isPresent()) {
            cir.setReturnValue(campaignEnemy);
        }
    }
}