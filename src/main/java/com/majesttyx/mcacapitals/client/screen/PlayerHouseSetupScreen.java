package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.house.PlayerHouseInheritanceMode;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PlayerHouseSetupScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("mcacapitals", "textures/gui/declaration_paper.png");

    private static final int BG_WIDTH = 200;
    private static final int BG_HEIGHT = 190;

    private final UUID capitalId;
    private final String villageName;

    private EditBox houseNameBox;
    private Button preserveButton;
    private Button followLawButton;
    private Component errorMessage = Component.empty();
    private PlayerHouseInheritanceMode selectedMode = PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE;

    public PlayerHouseSetupScreen(UUID capitalId, String villageName) {
        super(Component.literal("Establish Your House"));
        this.capitalId = capitalId;
        this.villageName = villageName == null || villageName.isBlank() ? "this capital" : villageName;
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        houseNameBox = new EditBox(this.font, left + 32, top + 80, 136, 18, Component.literal("House Name"));
        houseNameBox.setMaxLength(20);
        houseNameBox.setResponder(value -> errorMessage = Component.empty());
        addRenderableWidget(houseNameBox);
        setInitialFocus(houseNameBox);

        preserveButton = Button.builder(Component.literal(""), button -> {
            selectedMode = PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE;
            refreshModeButtons();
        }).bounds(left + 22, top + 116, 156, 20).build();
        addRenderableWidget(preserveButton);

        followLawButton = Button.builder(Component.literal(""), button -> {
            selectedMode = PlayerHouseInheritanceMode.FOLLOW_CAPITAL_LAW;
            refreshModeButtons();
        }).bounds(left + 22, top + 142, 156, 20).build();
        addRenderableWidget(followLawButton);

        addRenderableWidget(
                Button.builder(Component.literal("Continue"), button -> submit())
                        .bounds(left + 54, top + 168, 92, 20)
                        .build()
        );

        refreshModeButtons();
    }

    private void refreshModeButtons() {
        if (preserveButton != null) {
            preserveButton.setMessage(Component.literal(
                    (selectedMode == PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE ? "✓ " : "") + "Preserve My House"
            ));
        }
        if (followLawButton != null) {
            followLawButton.setMessage(Component.literal(
                    (selectedMode == PlayerHouseInheritanceMode.FOLLOW_CAPITAL_LAW ? "✓ " : "") + "Follow Capital Law"
            ));
        }
    }

    private void submit() {
        String houseName = PlayerHouseService.normalizeHouseName(houseNameBox == null ? "" : houseNameBox.getValue());
        if (!PlayerHouseService.isValidHouseName(houseName)) {
            errorMessage = Component.literal("Use 2-20 letters, spaces, hyphens, or apostrophes.");
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null || capitalId == null) {
            onClose();
            return;
        }

        minecraft.player.connection.sendCommand("capitalhouse set_and_open " + capitalId + " " + selectedMode.name() + " " + houseName);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x55000000);

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        guiGraphics.blit(BACKGROUND, left, top, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        drawCenteredNoShadow(guiGraphics, "Establish Your House", this.width / 2, top + 14, 0x3E2E1F);

        drawWrappedCenteredNoShadow(
                guiGraphics,
                Component.literal("The courts of " + villageName + " will know you and your descendants by a House Name."),
                this.width / 2,
                top + 30,
                150,
                0x3E2E1F
        );

        guiGraphics.drawString(this.font, "House Name", left + 32, top + 68, 0x3E2E1F, false);

        String preview = "House " + PlayerHouseService.normalizeHouseName(houseNameBox == null ? "" : houseNameBox.getValue());
        if (houseNameBox == null || houseNameBox.getValue().isBlank()) {
            preview = "House [Name]";
        }
        drawCenteredNoShadow(guiGraphics, preview, this.width / 2, top + 104, 0x3E2E1F);

        if (errorMessage != null && !errorMessage.getString().isBlank()) {
            drawWrappedCenteredNoShadow(guiGraphics, errorMessage, this.width / 2, top + 192, 176, 0xAA0000);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredNoShadow(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        int width = this.font.width(text);
        guiGraphics.drawString(this.font, text, centerX - width / 2, y, color, false);
    }

    private void drawWrappedCenteredNoShadow(GuiGraphics guiGraphics, Component text, int centerX, int startY, int maxWidth, int color) {
        Font font = this.font;
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, maxWidth);
        int y = startY;

        for (net.minecraft.util.FormattedCharSequence line : lines) {
            int width = font.width(line);
            guiGraphics.drawString(font, line, centerX - width / 2, y, color, false);
            y += 11;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}