package com.example.mcacapitals.client.screen;

import com.example.mcacapitals.MCACapitals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class AbdicationConfirmScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("mcacapitals", "textures/gui/declaration_paper.png");

    private static final int BG_WIDTH = 160;
    private static final int BG_HEIGHT = 160;

    private boolean finalConfirm = false;

    public AbdicationConfirmScreen() {
        super(Component.literal("Declaration of Abdication"));
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        if (!finalConfirm) {
            addRenderableWidget(
                    Button.builder(Component.literal("Yes"), button -> {
                        finalConfirm = true;
                        init();
                    }).bounds(left + 18, top + 118, 54, 20).build()
            );

            addRenderableWidget(
                    Button.builder(Component.literal("No"), button -> onClose())
                            .bounds(left + 88, top + 118, 54, 20)
                            .build()
            );
        } else {
            addRenderableWidget(
                    Button.builder(Component.literal("Yes"), button -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.player != null && minecraft.player.connection != null) {
                            minecraft.player.connection.sendCommand("capitalabdication confirm");
                        }
                        onClose();
                    }).bounds(left + 18, top + 118, 54, 20).build()
            );

            addRenderableWidget(
                    Button.builder(Component.literal("No"), button -> onClose())
                            .bounds(left + 88, top + 118, 54, 20)
                            .build()
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        guiGraphics.blit(BACKGROUND, left, top, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        drawCenteredNoShadow(guiGraphics, "Declaration of Abdication", this.width / 2, top + 18, 0x3E2E1F);

        if (!finalConfirm) {
            drawWrappedCenteredNoShadow(
                    guiGraphics,
                    Component.literal("Do you wish to abdicate the throne?"),
                    this.width / 2,
                    top + 52,
                    108,
                    0x3E2E1F
            );
        } else {
            drawWrappedCenteredNoShadow(
                    guiGraphics,
                    Component.literal("Are you certain? This act cannot be undone."),
                    this.width / 2,
                    top + 48,
                    116,
                    0x3E2E1F
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredNoShadow(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        int width = this.font.width(text);
        guiGraphics.drawString(this.font, text, centerX - width / 2, y, color, false);
    }

    private void drawWrappedCenteredNoShadow(GuiGraphics guiGraphics, Component text, int centerX, int startY, int maxWidth, int color) {
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