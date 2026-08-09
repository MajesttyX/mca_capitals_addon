package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.menu.DiplomaticPackageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DiplomaticPackageScreen
        extends AbstractContainerScreen<DiplomaticPackageMenu> {

    public DiplomaticPackageScreen(
            DiplomaticPackageMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 140;
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelX = 8;
        inventoryLabelY = 47;
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, 0x55000000);
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int left = leftPos;
        int top = topPos;
        guiGraphics.fill(
                left,
                top,
                left + imageWidth,
                top + imageHeight,
                0xFFF4E7C5
        );
        guiGraphics.fill(
                left + 1,
                top + 1,
                left + imageWidth - 1,
                top + imageHeight - 1,
                0xFF2F251B
        );
        guiGraphics.fill(
                left + 3,
                top + 3,
                left + imageWidth - 3,
                top + imageHeight - 3,
                0xFFF4E7C5
        );
        drawSlot(guiGraphics, left + 61, top + 24);
        drawSlot(guiGraphics, left + 79, top + 24);
        drawSlot(guiGraphics, left + 97, top + 24);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(
                        guiGraphics,
                        left + 7 + column * 18,
                        top + 57 + row * 18
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(
                    guiGraphics,
                    left + 7 + column * 18,
                    top + 115
            );
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF5B4632);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFD8C79D);
    }
}
