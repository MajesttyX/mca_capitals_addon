package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.house.PlayerHouseInheritanceMode;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PlayerHouseSetupScreen extends CapitalNoBlurScreen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("mcacapitals", "textures/gui/declaration_paper.png");
    private static final int BG_WIDTH = 200;
    private static final int BG_HEIGHT = 150;

    private final UUID capitalId;
    private final String villageName;
    private EditBox houseNameBox;
    private Component errorMessage = Component.empty();

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

        addRenderableWidget(Button.builder(Component.literal("Continue"), button -> submit())
                .bounds(left + 54, top + 116, 92, 20)
                .build());
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
        minecraft.player.connection.sendCommand(
                "capitalhouse set_and_open " + capitalId + " "
                        + PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE.name() + " " + houseName
        );
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
            drawWrappedCenteredNoShadow(guiGraphics, errorMessage, this.width / 2, top + 138, 176, 0xAA0000);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredNoShadow(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    private void drawWrappedCenteredNoShadow(GuiGraphics guiGraphics, Component text, int centerX, int startY, int maxWidth, int color) {
        Font font = this.font;
        int y = startY;
        for (net.minecraft.util.FormattedCharSequence line : font.split(text, maxWidth)) {
            guiGraphics.drawString(font, line, centerX - font.width(line) / 2, y, color, false);
            y += 10;
        }
    }
}
