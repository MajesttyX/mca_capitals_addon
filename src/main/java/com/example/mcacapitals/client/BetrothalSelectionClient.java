package com.example.mcacapitals.client;

import com.example.mcacapitals.client.screen.BetrothalSelectionScreen;
import com.example.mcacapitals.network.OpenBetrothalSelectionPacket;
import net.minecraft.client.Minecraft;

public final class BetrothalSelectionClient {

    private BetrothalSelectionClient() {
    }

    public static void open(OpenBetrothalSelectionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(null);
        minecraft.tell(() -> minecraft.setScreen(new BetrothalSelectionScreen(
                packet.capitalId(),
                packet.villageName(),
                packet.playerCandidates(),
                packet.recommendationCandidates()
        )));
    }
}