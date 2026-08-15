package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.client.screen.AccusationSelectionScreen;
import com.majesttyx.mcacapitals.network.OpenAccusationSelectionPacket;
import net.minecraft.client.Minecraft;

public final class AccusationSelectionClient {

    private AccusationSelectionClient() {
    }

    public static void open(OpenAccusationSelectionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(null);
        minecraft.tell(() -> minecraft.setScreen(new AccusationSelectionScreen(
                packet.capitalId(),
                packet.villageName(),
                packet.candidates()
        )));
    }
}