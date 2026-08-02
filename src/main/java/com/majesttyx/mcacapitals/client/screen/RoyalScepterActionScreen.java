package com.majesttyx.mcacapitals.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class RoyalScepterActionScreen extends CapitalNoBlurScreen {

    private final UUID targetId;
    private final String targetName;

    public RoyalScepterActionScreen(
            UUID targetId,
            String targetName
    ) {
        super(Component.literal("Royal Scepter"));
        this.targetId = targetId;
        this.targetName =
                targetName == null
                        || targetName.isBlank()
                        ? "Unnamed"
                        : targetName;
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = this.width / 2;
        int buttonWidth = 240;
        int buttonHeight = 20;
        int left = centerX - buttonWidth / 2;
        int top = Math.max(
                42,
                this.height / 2 - 104
        );

        addButton(
                left,
                top,
                buttonWidth,
                buttonHeight,
                "Name Heir Apparent",
                "royalscepter heir " + targetId
        );

        addButton(
                left,
                top + 24,
                buttonWidth,
                buttonHeight,
                "Name Hand of the Crown",
                "royalscepter hand " + targetId
        );

        addButton(
                left,
                top + 48,
                buttonWidth,
                buttonHeight,
                "Name Grand Maester",
                "royalscepter grandmaester " + targetId
        );

        addButton(
                left,
                top + 72,
                buttonWidth,
                buttonHeight,
                "Appoint Lord Commander",
                "royalscepter commander " + targetId
        );

        addButton(
                left,
                top + 96,
                buttonWidth,
                buttonHeight,
                "Appoint to the Royal Guard",
                "royalscepter royalguard " + targetId
        );

        addButton(
                left,
                top + 120,
                buttonWidth,
                buttonHeight,
                "Appoint Ambassador",
                "royalscepteroffice ambassador " + targetId
        );

        addButton(
                left,
                top + 144,
                buttonWidth,
                buttonHeight,
                "Appoint Master of Laws",
                "royalscepteroffice masteroflaws " + targetId
        );

        addButton(
                left,
                top + 168,
                buttonWidth,
                buttonHeight,
                "Bestow Dukedom",
                "royalscepter duke " + targetId
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Cancel"),
                                button -> onClose()
                        )
                        .bounds(
                                left,
                                top + 200,
                                buttonWidth,
                                buttonHeight
                        )
                        .build()
        );
    }

    private void addButton(
            int left,
            int top,
            int width,
            int height,
            String label,
            String command
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(label),
                                button -> runCommand(command)
                        )
                        .bounds(
                                left,
                                top,
                                width,
                                height
                        )
                        .build()
        );
    }

    private void runCommand(String command) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null
                && minecraft.player.connection != null) {
            minecraft.player.connection
                    .sendCommand(command);
        }

        onClose();
    }

    private String getTrimmedTargetName() {
        return this.font.plainSubstrByWidth(
                this.targetName,
                220
        );
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int centerX = this.width / 2;
        int titleY = Math.max(
                8,
                this.height / 2 - 142
        );

        guiGraphics.drawCenteredString(
                this.font,
                "Royal Scepter",
                centerX,
                titleY,
                0xFFFFFF
        );

        guiGraphics.drawCenteredString(
                this.font,
                "Choose appointment for:",
                centerX,
                titleY + 14,
                0xCCCCCC
        );

        guiGraphics.drawCenteredString(
                this.font,
                getTrimmedTargetName(),
                centerX,
                titleY + 26,
                0xCCCCCC
        );

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
