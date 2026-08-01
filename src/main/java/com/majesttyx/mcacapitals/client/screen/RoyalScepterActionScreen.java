package com.majesttyx.mcacapitals.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class RoyalScepterActionScreen extends CapitalNoBlurScreen {

    private final UUID targetId;
    private final String targetName;

    public RoyalScepterActionScreen(UUID targetId, String targetName) {
        super(Component.literal("Royal Scepter"));
        this.targetId = targetId;
        this.targetName = targetName == null || targetName.isBlank() ? "Unnamed" : targetName;
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = this.width / 2;
        int buttonWidth = 240;
        int buttonHeight = 20;
        int left = centerX - buttonWidth / 2;
        int top = this.height / 2 - 60;

        addRenderableWidget(
                Button.builder(
                                Component.literal("Name Heir Apparent"),
                                button -> runCommand("royalscepter heir " + targetId)
                        )
                        .bounds(left, top, buttonWidth, buttonHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Name Hand of the Crown"),
                                button -> runCommand("royalscepter hand " + targetId)
                        )
                        .bounds(left, top + 24, buttonWidth, buttonHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Name Grand Maester"),
                                button -> runCommand("royalscepter grandmaester " + targetId)
                        )
                        .bounds(left, top + 48, buttonWidth, buttonHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Appoint Lord Commander"),
                                button -> runCommand("royalscepter commander " + targetId)
                        )
                        .bounds(left, top + 72, buttonWidth, buttonHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Appoint to the Royal Guard"),
                                button -> runCommand("royalscepter royalguard " + targetId)
                        )
                        .bounds(left, top + 96, buttonWidth, buttonHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Bestow Dukedom"),
                                button -> runCommand("royalscepter duke " + targetId)
                        )
                        .bounds(left, top + 120, buttonWidth, buttonHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Cancel"),
                                button -> onClose()
                        )
                        .bounds(left, top + 152, buttonWidth, buttonHeight)
                        .build()
        );
    }

    private void runCommand(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand(command);
        }
        onClose();
    }

    private String getTrimmedTargetName() {
        return this.font.plainSubstrByWidth(this.targetName, 220);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int titleY = this.height / 2 - 98;

        guiGraphics.drawCenteredString(this.font, "Royal Scepter", centerX, titleY, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Choose appointment for:", centerX, titleY + 14, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, getTrimmedTargetName(), centerX, titleY + 26, 0xCCCCCC);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}