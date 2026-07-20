package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.client.screen.SealedPurseCaseSelectionScreen;
import com.majesttyx.mcacapitals.network.OpenSealedPurseCaseSelectionPacket;
import net.minecraft.client.Minecraft;

public final class SealedPurseCaseSelectionClient {

    private SealedPurseCaseSelectionClient() {
    }

    public static void open(OpenSealedPurseCaseSelectionPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(null);
        minecraft.tell(() -> minecraft.setScreen(new SealedPurseCaseSelectionScreen(
                packet.capitalId(),
                packet.villageName(),
                packet.cases()
        )));
    }
}