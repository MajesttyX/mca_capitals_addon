package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalCampaignCombatDamageHandler {

    private static final float
            CAMPAIGN_DAMAGE_MULTIPLIER =
            0.3F;

    private CapitalCampaignCombatDamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingDamage(
            LivingDamageEvent.Pre event
    ) {
        LivingEntity victim =
                event.getEntity();

        if (!(victim.level()
                instanceof ServerLevel level)) {
            return;
        }

        Entity sourceEntity =
                event.getSource().getEntity();

        if (!(sourceEntity
                instanceof LivingEntity attacker)) {
            return;
        }

        if (!CapitalCampaignCombatService
                .areOpposingCombatants(
                        level,
                        attacker.getUUID(),
                        victim.getUUID()
                )) {
            return;
        }

        if (!CapitalCampaignCombatService
                .canApplyCampaignDamage(
                        level,
                        attacker,
                        victim
                )) {
            event.setNewDamage(0.0F);
            return;
        }

        event.setNewDamage(
                Math.max(
                        0.0F,
                        event.getNewDamage()
                                * CAMPAIGN_DAMAGE_MULTIPLIER
                )
        );
    }
}