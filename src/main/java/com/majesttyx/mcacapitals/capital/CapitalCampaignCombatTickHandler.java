package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import forge.net.mca.entity.VillagerEntityMCA;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalCampaignCombatTickHandler {

    private CapitalCampaignCombatTickHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(
            LivingEvent.LivingTickEvent event
    ) {
        if (event.getEntity()
                instanceof VillagerEntityMCA villager) {
            CapitalCampaignCombatService
                    .enforceCombatState(villager);
        }
    }
}