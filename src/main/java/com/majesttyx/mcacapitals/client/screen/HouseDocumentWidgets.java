package com.majesttyx.mcacapitals.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class HouseDocumentWidgets {

    public static final ResourceLocation SCROLL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "mcacapitals",
                    "textures/gui/house_document_scroll.png"
            );

    public static final int SOURCE_WIDTH = 81;
    public static final int SOURCE_HEIGHT = 148;

    public static final int LEFT_SLICE = 20;
    public static final int CENTER_SLICE = 41;
    public static final int RIGHT_SLICE = 20;

    private HouseDocumentWidgets() {
    }

    public static void renderWideScroll(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        int leftWidth =
                Math.max(
                        1,
                        Math.round(
                                LEFT_SLICE
                                        * (height / (float) SOURCE_HEIGHT)
                        )
                );

        int rightWidth = leftWidth;
        int centerWidth =
                Math.max(
                        1,
                        width
                                - leftWidth
                                - rightWidth
                );

        graphics.blit(
                SCROLL_TEXTURE,
                x,
                y,
                leftWidth,
                height,
                0.0F,
                0.0F,
                LEFT_SLICE,
                SOURCE_HEIGHT,
                SOURCE_WIDTH,
                SOURCE_HEIGHT
        );

        graphics.blit(
                SCROLL_TEXTURE,
                x + leftWidth,
                y,
                centerWidth,
                height,
                LEFT_SLICE,
                0.0F,
                CENTER_SLICE,
                SOURCE_HEIGHT,
                SOURCE_WIDTH,
                SOURCE_HEIGHT
        );

        graphics.blit(
                SCROLL_TEXTURE,
                x + leftWidth + centerWidth,
                y,
                rightWidth,
                height,
                LEFT_SLICE + CENTER_SLICE,
                0.0F,
                RIGHT_SLICE,
                SOURCE_HEIGHT,
                SOURCE_WIDTH,
                SOURCE_HEIGHT
        );
    }

    public static final class FlatButton
            extends AbstractButton {

        private final Runnable action;

        public FlatButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                Runnable action
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    message
            );
            this.action = action;
        }

        @Override
        public void onPress() {
            if (action != null) {
                action.run();
            }
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output
        ) {
            output.add(
                    NarratedElementType.TITLE,
                    this.createNarrationMessage()
            );
        }

        @Override
        protected void renderWidget(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            int background =
                    this.isHoveredOrFocused()
                            ? 0x55FFFFFF
                            : 0x22FFFFFF;

            int border =
                    this.active
                            ? 0xFF5A4632
                            : 0xFF8A8074;

            int textColor =
                    this.active
                            ? 0xFF2F2418
                            : 0xFF8A8074;

            graphics.fill(
                    getX(),
                    getY(),
                    getX() + this.width,
                    getY() + this.height,
                    background
            );

            graphics.fill(
                    getX(),
                    getY(),
                    getX() + this.width,
                    getY() + 1,
                    border
            );
            graphics.fill(
                    getX(),
                    getY() + this.height - 1,
                    getX() + this.width,
                    getY() + this.height,
                    border
            );
            graphics.fill(
                    getX(),
                    getY(),
                    getX() + 1,
                    getY() + this.height,
                    border
            );
            graphics.fill(
                    getX() + this.width - 1,
                    getY(),
                    getX() + this.width,
                    getY() + this.height,
                    border
            );

            Font font =
                    Minecraft.getInstance().font;

            int textX =
                    getX()
                            + (this.width
                            - font.width(
                                    getMessage()
                            )) / 2;

            int textY =
                    getY()
                            + (this.height - 8) / 2;

            graphics.drawString(
                    font,
                    getMessage(),
                    textX,
                    textY,
                    textColor,
                    false
            );
        }
    }
}
