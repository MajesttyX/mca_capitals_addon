package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.client.BlueprintAuthorityClientCache;
import com.majesttyx.mcacapitals.client.VillagerIdentityClientCache;
import com.majesttyx.mcacapitals.client.screen.DiplomaticPackageScreen;
import com.majesttyx.mcacapitals.menu.ModMenus;
import com.majesttyx.mcacapitals.network.ModNetwork;
import net.minecraft.client.gui.screens.MenuScreens;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class MCACapitalsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(
                ModMenus.DIPLOMATIC_PACKAGE,
                DiplomaticPackageScreen::new
        );
        ModNetwork.registerClientReceivers();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VillagerIdentityClientCache.clearAll();
            BlueprintAuthorityClientCache.clear();
        });
    }
}