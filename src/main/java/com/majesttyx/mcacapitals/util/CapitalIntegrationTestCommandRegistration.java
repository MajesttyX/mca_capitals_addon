package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalIntegrationTestCommandRegistration {

    private CapitalIntegrationTestCommandRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        CapitalIntegrationTestCommands.register(
                event.getDispatcher()
        );
    }
}