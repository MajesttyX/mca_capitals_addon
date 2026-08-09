package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.client.screen.AmbassadorCommunicationScreen;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class AmbassadorCommunicationClient {

    private static Screen conversationScreen;

    private AmbassadorCommunicationClient() {
    }

    public static void open(OpenAmbassadorCommunicationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || packet == null) {
            return;
        }

        Screen current = minecraft.screen;
        if (!(current instanceof AmbassadorCommunicationScreen)) {
            conversationScreen = current;
        }

        Screen parent = conversationScreen;
        minecraft.tell(() -> minecraft.setScreen(
                new AmbassadorCommunicationScreen(packet, parent)
        ));
    }

    public static void finishConversationOverlay() {
        conversationScreen = null;
    }
}
