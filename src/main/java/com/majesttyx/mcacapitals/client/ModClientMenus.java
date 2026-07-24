package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.screen.DiplomaticPackageScreen;
import com.majesttyx.mcacapitals.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(
        modid = MCACapitals.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ModClientMenus {

    private ModClientMenus() {
    }

    @SubscribeEvent
    public static void registerScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                ModMenus.DIPLOMATIC_PACKAGE.get(),
                DiplomaticPackageScreen::new
        );
    }
}