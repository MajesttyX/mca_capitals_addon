package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.client.screen.AmbassadorCommunicationScreen;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import net.minecraft.client.Minecraft;

public final class AmbassadorCommunicationClient {

    private AmbassadorCommunicationClient() {
    }

    public static void open(
            OpenAmbassadorCommunicationPacket packet
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(null);

        minecraft.tell(() -> minecraft.setScreen(
                new AmbassadorCommunicationScreen(packet)
        ));
    }
}