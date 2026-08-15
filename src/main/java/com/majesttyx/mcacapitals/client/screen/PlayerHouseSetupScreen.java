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
        super(Component.translatable("mcacapitals.system.player_house_setup_screen.establish_your_house"));
        this.capitalId = capitalId;
        this.villageName = villageName == null ? "" : villageName.trim();
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        houseNameBox = new EditBox(this.font, left + 32, top + 80, 136, 18, Component.translatable("mcacapitals.system.player_house_setup_screen.house_name"));
        houseNameBox.setMaxLength(20);
        houseNameBox.setResponder(value -> errorMessage = Component.empty());
        addRenderableWidget(houseNameBox);
        setInitialFocus(houseNameBox);

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.player_house_setup_screen.continue"), button -> submit())
                        .bounds(left + 54, top + 116, 92, 20)
                        .build()
        );
    }

    private void submit() {
        String houseName = PlayerHouseService.normalizeHouseName(houseNameBox == null ? "" : houseNameBox.getValue());
        if (!PlayerHouseService.isValidHouseName(houseName)) {
            errorMessage = Component.translatable("mcacapitals.system.player_house_setup_screen.use_2_20_letters_spaces_hyphens_or_apostrophes");
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null || capitalId == null) {
            onClose();
            return;
        }

        minecraft.player.connection.sendCommand("capitalhouse set_and_open " + capitalId + " " + PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE.name() + " " + houseName);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        guiGraphics.blit(BACKGROUND, left, top, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        drawCenteredNoShadow(guiGraphics, Component.translatable("mcacapitals.system.player_house_setup_screen.establish_your_house"), this.width / 2, top + 14, 0x3E2E1F);

        drawWrappedCenteredNoShadow(
                guiGraphics,
                Component.translatable(
                        "mcacapitals.system.player_house_setup_screen.courts_will_know_house",
                        villageName.isBlank()
                                ? Component.translatable("mcacapitals.system.common.this_capital")
                                : "Unknown Village".equals(villageName)
                                ? Component.translatable("mcacapitals.system.common.unknown_village")
                                : Component.literal(villageName)
                ),
                this.width / 2,
                top + 30,
                150,
                0x3E2E1F
        );

        guiGraphics.drawString(this.font, Component.translatable("mcacapitals.system.player_house_setup_screen.house_name"), left + 32, top + 68, 0x3E2E1F, false);

        String normalizedHouseName = PlayerHouseService.normalizeHouseName(houseNameBox == null ? "" : houseNameBox.getValue());
        Component preview = houseNameBox == null || houseNameBox.getValue().isBlank()
                ? Component.translatable("mcacapitals.system.player_house_setup_screen.house_preview_placeholder")
                : Component.translatable(
                        "mcacapitals.system.player_house_setup_screen.house_preview",
                        Component.literal(normalizedHouseName)
                );
        drawCenteredNoShadow(guiGraphics, preview, this.width / 2, top + 104, 0x3E2E1F);

        if (errorMessage != null && !errorMessage.getString().isBlank()) {
            drawWrappedCenteredNoShadow(guiGraphics, errorMessage, this.width / 2, top + 138, 176, 0xAA0000);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredNoShadow(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
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