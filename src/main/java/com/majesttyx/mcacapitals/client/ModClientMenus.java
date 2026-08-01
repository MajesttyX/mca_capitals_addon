package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.screen.DiplomaticPackageScreen;
import com.majesttyx.mcacapitals.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = MCACapitals.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class ModClientMenus {

    private ModClientMenus() {
    }

    @SubscribeEvent
    public static void registerScreens(
            FMLClientSetupEvent event
    ) {
        event.enqueueWork(
                () -> MenuScreens.register(
                        ModMenus.DIPLOMATIC_PACKAGE.get(),
                        DiplomaticPackageScreen::new
                )
        );
    }
}