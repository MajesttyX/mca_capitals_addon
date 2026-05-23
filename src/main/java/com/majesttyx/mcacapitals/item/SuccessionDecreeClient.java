package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.client.screen.SuccessionDecreeConfirmScreen;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public class SuccessionDecreeClient {

    private SuccessionDecreeClient() {
    }

    public static void openScreen(UUID capitalId, String capitalName, UUID targetId, String targetName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(new SuccessionDecreeConfirmScreen(capitalId, capitalName, targetId, targetName));
    }
}