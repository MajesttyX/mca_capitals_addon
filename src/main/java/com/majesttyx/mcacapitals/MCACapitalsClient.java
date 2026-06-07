package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.network.ModNetwork;
import net.fabricmc.api.ClientModInitializer;

public class MCACapitalsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModNetwork.registerClientReceivers();
    }
}