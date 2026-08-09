package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.client.screen.DiplomaticPackageScreen;
import com.majesttyx.mcacapitals.menu.ModMenus;
import com.majesttyx.mcacapitals.network.ModNetwork;
import net.minecraft.client.gui.screens.MenuScreens;
import net.fabricmc.api.ClientModInitializer;

public class MCACapitalsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(
                ModMenus.DIPLOMATIC_PACKAGE,
                DiplomaticPackageScreen::new
        );
        ModNetwork.registerClientReceivers();
    }
}