package com.majesttyx.mcacapitals.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.UUID;

public class SuccessionDecreeConfirmScreen extends Screen {

    private final UUID capitalId;
    private final String capitalName;
    private final UUID targetId;
    private final String targetName;

    public SuccessionDecreeConfirmScreen(UUID capitalId, String capitalName, UUID targetId, String targetName) {
        super(Component.literal("Succession Decree"));
        this.capitalId = capitalId;
        this.capitalName = capitalName == null || capitalName.isBlank() ? "Unknown Capital" : capitalName;
        this.targetId = targetId;
        this.targetName = targetName == null || targetName.isBlank() ? "Unnamed" : targetName;
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = this.width / 2;
        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonY = this.height / 2 + 44;

        addRenderableWidget(
                Button.builder(Component.literal("Yes"), button -> confirm())
                        .bounds(centerX - 88, buttonY, buttonWidth, buttonHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("No"), button -> onClose())
                        .bounds(centerX + 8, buttonY, buttonWidth, buttonHeight)
                        .build()
        );
    }

    private void confirm() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand("successiondecree confirm " + capitalId + " " + targetId);
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int top = this.height / 2 - 58;

        guiGraphics.drawCenteredString(this.font, "Succession Decree", centerX, top, 0xFFFFFF);

        drawWrappedCentered(
                guiGraphics,
                Component.literal("Do you wish to transfer the crown of " + capitalName + " to " + targetName + "?"),
                centerX,
                top + 28,
                260,
                0xFFFFFF
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawWrappedCentered(GuiGraphics guiGraphics, Component text, int centerX, int startY, int maxWidth, int color) {
        Font font = this.font;
        java.util.List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int y = startY;

        for (FormattedCharSequence line : lines) {
            int width = font.width(line);
            guiGraphics.drawString(font, line, centerX - width / 2, y, color, false);
            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}