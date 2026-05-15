package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.client.screen.RoyalScepterActionScreen;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public class RoyalScepterClient {

    private RoyalScepterClient() {
    }

    public static void openScreen(UUID targetId, String targetName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(new RoyalScepterActionScreen(targetId, targetName));
    }
}