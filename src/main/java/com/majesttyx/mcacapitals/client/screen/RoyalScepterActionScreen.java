package com.majesttyx.mcacapitals.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class RoyalScepterActionScreen extends CapitalNoBlurScreen {

    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int COLUMN_GAP = 8;

    private final UUID targetId;
    private final String targetName;

    public RoyalScepterActionScreen(
            UUID targetId,
            String targetName
    ) {
        super(Component.literal("Royal Scepter"));
        this.targetId = targetId;
        this.targetName =
                targetName == null || targetName.isBlank()
                        ? "Unnamed"
                        : targetName;
    }

    @Override
    protected void init() {
        clearWidgets();

        int totalWidth =
                Math.min(
                        396,
                        Math.max(
                                280,
                                this.width - 24
                        )
                );

        int columnWidth =
                (totalWidth - COLUMN_GAP) / 2;

        int left =
                (this.width - totalWidth) / 2;

        int right =
                left + columnWidth + COLUMN_GAP;

        int top =
                Math.max(
                        54,
                        this.height / 2 - 50
                );

        int rowStep =
                BUTTON_HEIGHT + ROW_GAP;

        addActionButton(
                "Name Heir Apparent",
                "royalscepter heir " + targetId,
                left,
                top,
                columnWidth
        );

        addActionButton(
                "Name Hand of the Crown",
                "royalscepter hand " + targetId,
                right,
                top,
                columnWidth
        );

        addActionButton(
                "Name Grand Maester",
                "royalscepter grandmaester " + targetId,
                left,
                top + rowStep,
                columnWidth
        );

        addActionButton(
                "Appoint Lord Commander",
                "royalscepter commander " + targetId,
                right,
                top + rowStep,
                columnWidth
        );

        addActionButton(
                "Appoint Royal Guard",
                "royalscepter royalguard " + targetId,
                left,
                top + rowStep * 2,
                columnWidth
        );

        addActionButton(
                "Bestow Dukedom",
                "royalscepter duke " + targetId,
                right,
                top + rowStep * 2,
                columnWidth
        );

        addActionButton(
                "Appoint Master of Laws",
                "royalscepter masteroflaws " + targetId,
                left,
                top + rowStep * 3,
                columnWidth
        );

        addActionButton(
                "Appoint Ambassador",
                "royalscepter ambassador " + targetId,
                right,
                top + rowStep * 3,
                columnWidth
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Cancel"),
                                button -> onClose()
                        )
                        .bounds(
                                left,
                                top + rowStep * 4 + 4,
                                totalWidth,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
    }

    private void addActionButton(
            String label,
            String command,
            int x,
            int y,
            int width
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(label),
                                button -> runCommand(command)
                        )
                        .bounds(
                                x,
                                y,
                                width,
                                BUTTON_HEIGHT
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
                Math.min(
                        360,
                        this.width - 32
                )
        );
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int top =
                Math.max(
                        54,
                        this.height / 2 - 50
                );

        int centerX =
                this.width / 2;

        int titleY =
                top - 42;

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