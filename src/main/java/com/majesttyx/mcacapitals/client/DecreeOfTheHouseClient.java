package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.client.screen.DecreeOfTheHouseScreen;
import com.majesttyx.mcacapitals.network.OpenDecreeOfTheHousePacket;
import net.minecraft.client.Minecraft;

public final class DecreeOfTheHouseClient {

    private DecreeOfTheHouseClient() {
    }

    public static void open(OpenDecreeOfTheHousePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        minecraft.setScreen(new DecreeOfTheHouseScreen(packet).withInitialValues(
                packet.firstName(),
                packet.currentSurname(),
                packet.houseWords()
        ));
    }
}