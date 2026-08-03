package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalCampaignCombatDamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCampaignDamageMixin {

    @ModifyVariable(
            method = "actuallyHurt",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float mcacapitals$scaleCampaignDamage(
            float amount,
            DamageSource source
    ) {
        return CapitalCampaignCombatDamageHandler
                .modifyDamage(
                        (LivingEntity) (Object) this,
                        source,
                        amount
                );
    }
}
