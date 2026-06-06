package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.client.screen.PlayerHouseSetupScreen;
import com.majesttyx.mcacapitals.network.OpenPlayerHouseSetupPacket;
import net.minecraft.client.Minecraft;

public class PlayerHouseSetupClient {

    private PlayerHouseSetupClient() {
    }

    public static void open(OpenPlayerHouseSetupPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(new PlayerHouseSetupScreen(packet.capitalId(), packet.villageName()));
    }
}