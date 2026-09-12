package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MCACapitals.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CapitalClientLifecycleHandler {

    private CapitalClientLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CapitalClientRuntimeStateReset.clearSessionState();
    }
}
