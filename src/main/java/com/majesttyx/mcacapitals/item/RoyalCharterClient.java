package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.client.screen.RoyalCharterDecisionScreen;
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