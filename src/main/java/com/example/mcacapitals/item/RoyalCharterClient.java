package com.example.mcacapitals.item;

import com.example.mcacapitals.client.screen.RoyalCharterDecisionScreen;
import net.minecraft.client.Minecraft;

public class RoyalCharterClient {

    private RoyalCharterClient() {
    }

    public static void openDecisionScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(new RoyalCharterDecisionScreen());
    }
}