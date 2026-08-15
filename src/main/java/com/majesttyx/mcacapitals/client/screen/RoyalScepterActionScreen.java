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
        super(Component.translatable("mcacapitals.system.royal_scepter_action_screen.royal_scepter"));
        this.targetId = targetId;
        this.targetName = targetName == null ? "" : targetName.trim();
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
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.name_heir_apparent"),
                "royalscepter heir " + targetId,
                left,
                top,
                columnWidth
        );

        addActionButton(
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.name_hand_of_the_crown"),
                "royalscepter hand " + targetId,
                right,
                top,
                columnWidth
        );

        addActionButton(
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.name_grand_maester"),
                "royalscepter grandmaester " + targetId,
                left,
                top + rowStep,
                columnWidth
        );

        addActionButton(
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.appoint_lord_commander"),
                "royalscepter commander " + targetId,
                right,
                top + rowStep,
                columnWidth
        );

        addActionButton(
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.appoint_royal_guard"),
                "royalscepter royalguard " + targetId,
                left,
                top + rowStep * 2,
                columnWidth
        );

        addActionButton(
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.bestow_dukedom"),
                "royalscepter duke " + targetId,
                right,
                top + rowStep * 2,
                columnWidth
        );

        addActionButton(
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.appoint_master_of_laws"),
                "royalscepter masteroflaws " + targetId,
                left,
                top + rowStep * 3,
                columnWidth
        );

        addActionButton(
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.appoint_ambassador"),
                "royalscepter ambassador " + targetId,
                right,
                top + rowStep * 3,
                columnWidth
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable("mcacapitals.system.royal_scepter_action_screen.cancel"),
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
            Component label,
            String command,
            int x,
            int y,
            int width
    ) {
        addRenderableWidget(
                Button.builder(
                                label,
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

    private Component getTrimmedTargetName() {
        if (this.targetName.isBlank()) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }

        return Component.literal(this.font.plainSubstrByWidth(
                this.targetName,
                Math.min(
                        360,
                        this.width - 32
                )
        ));
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
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.royal_scepter"),
                centerX,
                titleY,
                0xFFFFFF
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("mcacapitals.system.royal_scepter_action_screen.choose_appointment_for"),
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