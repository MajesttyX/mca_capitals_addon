package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalCampaignCommandRegistration {

    private CapitalCampaignCommandRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        CapitalCampaignCommands.register(
                event.getDispatcher()
        );
    }
}