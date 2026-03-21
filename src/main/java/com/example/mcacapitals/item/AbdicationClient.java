package com.example.mcacapitals.item;

import com.example.mcacapitals.client.screen.AbdicationConfirmScreen;
import net.minecraft.client.Minecraft;

public class AbdicationClient {

    private AbdicationClient() {
    }

    public static void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.setScreen(new AbdicationConfirmScreen());
    }
}