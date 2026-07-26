package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalCampaignCombatTickHandler {

    private CapitalCampaignCombatTickHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {
        if (event.getEntity()
                instanceof VillagerEntityMCA villager) {
            CapitalCampaignCombatService
                    .enforceCombatState(villager);
        }
    }
}