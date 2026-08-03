package com.majesttyx.mcacapitals.capital;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class CapitalCampaignCombatDamageHandler {

    private static final float
            CAMPAIGN_DAMAGE_MULTIPLIER =
            0.3F;

    private CapitalCampaignCombatDamageHandler() {
    }

    public static boolean allowDamage(
            LivingEntity victim,
            DamageSource source,
            float amount
    ) {
        if (victim == null
                || source == null
                || !(victim.level()
                instanceof ServerLevel level)) {
            return true;
        }

        Entity sourceEntity =
                source.getEntity();

        if (!(sourceEntity
                instanceof LivingEntity attacker)) {
            return true;
        }

        if (!CapitalCampaignCombatService
                .areOpposingCombatants(
                        level,
                        attacker.getUUID(),
                        victim.getUUID()
                )) {
            return true;
        }

        return CapitalCampaignCombatService
                .canApplyCampaignDamage(
                        level,
                        attacker,
                        victim
                );
    }

    public static float modifyDamage(
            LivingEntity victim,
            DamageSource source,
            float amount
    ) {
        if (victim == null
                || source == null
                || !(victim.level()
                instanceof ServerLevel level)) {
            return amount;
        }

        Entity sourceEntity =
                source.getEntity();

        if (!(sourceEntity
                instanceof LivingEntity attacker)) {
            return amount;
        }

        if (!CapitalCampaignCombatService
                .areOpposingCombatants(
                        level,
                        attacker.getUUID(),
                        victim.getUUID()
                )
                || !CapitalCampaignCombatService
                .canApplyCampaignDamage(
                        level,
                        attacker,
                        victim
                )) {
            return amount;
        }

        return Math.max(
                0.0F,
                amount * CAMPAIGN_DAMAGE_MULTIPLIER
        );
    }
}
