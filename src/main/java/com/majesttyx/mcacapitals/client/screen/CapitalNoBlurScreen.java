package com.majesttyx.mcacapitals.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class CapitalNoBlurScreen extends Screen {

    protected CapitalNoBlurScreen(Component title) {
        super(title);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}
