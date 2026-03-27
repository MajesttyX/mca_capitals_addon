package com.example.mcacapitals.item;

import com.example.mcacapitals.client.screen.RoyalScepterActionScreen;
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