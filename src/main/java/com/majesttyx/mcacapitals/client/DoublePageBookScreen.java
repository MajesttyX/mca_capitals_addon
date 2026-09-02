package com.majesttyx.mcacapitals.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import com.majesttyx.mcacapitals.client.screen.CapitalNoBlurScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class DoublePageBookScreen extends CapitalNoBlurScreen {

    private static final ResourceLocation BOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "mcacapitals",
                    "textures/gui/open_book_double_page.png"
            );

    private static final int TEXTURE_WIDTH = 192;
    private static final int TEXTURE_HEIGHT = 128;

    /*
     * Safe writing areas measured inside the supplied parchment artwork.
     * These deliberately stay clear of the dark borders, center binding,
     * and navigation arrows.
     */
    private static final int LEFT_TEXT_X = 13;
    private static final int RIGHT_TEXT_X = 103;
    private static final int TEXT_Y = 11;
    private static final int TEXT_WIDTH = 76;
    private static final int TEXT_BOTTOM = 109;

    private static final int LEFT_ARROW_X = 4;
    private static final int RIGHT_ARROW_X = 174;
    private static final int ARROW_Y = 110;
    private static final int ARROW_WIDTH = 14;
    private static final int ARROW_HEIGHT = 14;

    private static final int LINE_HEIGHT = 9;
    private static final int TEXT_COLOR = 0x2F2418;

    private final BookViewScreen.BookAccess source;
    private final boolean separateSourcePages;

    private final List<List<FormattedCharSequence>>
            displayPages =
            new ArrayList<>();

    private int leftPageIndex;

    private float bookScale;
    private int bookX;
    private int bookY;
    private int renderedTextWidth;
    private int renderedTextHeight;
    private int maxLinesPerPage;

    public DoublePageBookScreen(
            BookViewScreen.BookAccess source,
            Component title,
            boolean separateSourcePages
    ) {
        super(title);
        this.source = source;
        this.separateSourcePages = separateSourcePages;
    }

    @Override
    protected void init() {
        updateLayout();
        rebuildDisplayPages();
    }

    private void updateLayout() {
        float widthScale =
                (this.width - 20.0F)
                        / TEXTURE_WIDTH;

        float heightScale =
                (this.height - 20.0F)
                        / TEXTURE_HEIGHT;

        this.bookScale = Math.min(
                2.0F,
                Math.min(
                        widthScale,
                        heightScale
                )
        );

        if (this.bookScale < 1.0F) {
            this.bookScale = 1.0F;
        }

        int renderedWidth =
                Math.round(
                        TEXTURE_WIDTH
                                * this.bookScale
                );

        int renderedHeight =
                Math.round(
                        TEXTURE_HEIGHT
                                * this.bookScale
                );

        this.bookX =
                (this.width - renderedWidth)
                        / 2;

        this.bookY =
                (this.height - renderedHeight)
                        / 2;

        this.renderedTextWidth =
                Math.max(
                        1,
                        Math.round(
                                TEXT_WIDTH
                                        * this.bookScale
                        )
                );

        this.renderedTextHeight =
                Math.max(
                        LINE_HEIGHT,
                        Math.round(
                                (TEXT_BOTTOM - TEXT_Y)
                                        * this.bookScale
                        )
                );

        this.maxLinesPerPage =
                Math.max(
                        1,
                        this.renderedTextHeight
                                / LINE_HEIGHT
                );
    }

    private void rebuildDisplayPages() {
        displayPages.clear();

        List<FormattedCharSequence> current =
                new ArrayList<>();

        if (source != null) {
            for (int sourceIndex = 0;
                 sourceIndex < source.getPageCount();
                 sourceIndex++) {

                FormattedText sourcePage =
                        source.getPage(
                                sourceIndex
                        );

                List<FormattedCharSequence> wrapped =
                        this.font.split(
                                sourcePage,
                                renderedTextWidth
                        );

                for (FormattedCharSequence line
                        : wrapped) {
                    if (current.size()
                            >= maxLinesPerPage) {
                        displayPages.add(
                                List.copyOf(current)
                        );
                        current.clear();
                    }

                    current.add(line);
                }

                if (separateSourcePages
                        && sourceIndex + 1 < source.getPageCount()
                        && !current.isEmpty()
                        && current.size() < maxLinesPerPage) {
                    current.add(
                            FormattedCharSequence.EMPTY
                    );
                }
            }
        }

        if (!current.isEmpty()) {
            displayPages.add(
                    List.copyOf(current)
            );
        }

        if (displayPages.isEmpty()) {
            displayPages.add(
                    List.of()
            );
        }

        if ((displayPages.size() & 1) != 0) {
            displayPages.add(
                    List.of()
            );
        }

        leftPageIndex =
                Math.max(
                        0,
                        Math.min(
                                leftPageIndex,
                                Math.max(
                                        0,
                                        displayPages.size() - 2
                                )
                        )
                );

        if ((leftPageIndex & 1) != 0) {
            leftPageIndex--;
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        renderBookTexture(graphics);

        renderPageText(
                graphics,
                leftPageIndex,
                LEFT_TEXT_X
        );

        renderPageText(
                graphics,
                leftPageIndex + 1,
                RIGHT_TEXT_X
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void renderBookTexture(
            GuiGraphics graphics
    ) {
        graphics.pose().pushPose();

        graphics.pose().translate(
                bookX,
                bookY,
                0.0F
        );

        graphics.pose().scale(
                bookScale,
                bookScale,
                1.0F
        );

        graphics.blit(
                BOOK_TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );

        graphics.pose().popPose();
    }

    private void renderPageText(
            GuiGraphics graphics,
            int pageIndex,
            int nativeTextX
    ) {
        if (pageIndex < 0
                || pageIndex
                >= displayPages.size()) {
            return;
        }

        List<FormattedCharSequence> lines =
                displayPages.get(pageIndex);

        int textX =
                bookX
                        + Math.round(
                                nativeTextX
                                        * bookScale
                        );

        int textY =
                bookY
                        + Math.round(
                                TEXT_Y
                                        * bookScale
                        );

        int lineCount =
                Math.min(
                        lines.size(),
                        maxLinesPerPage
                );

        for (int i = 0;
             i < lineCount;
             i++) {
            graphics.drawString(
                    this.font,
                    lines.get(i),
                    textX,
                    textY
                            + i * LINE_HEIGHT,
                    TEXT_COLOR,
                    false
            );
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (insideNativeBounds(
                    mouseX,
                    mouseY,
                    LEFT_ARROW_X,
                    ARROW_Y,
                    ARROW_WIDTH,
                    ARROW_HEIGHT
            )) {
                previousSpread();
                return true;
            }

            if (insideNativeBounds(
                    mouseX,
                    mouseY,
                    RIGHT_ARROW_X,
                    ARROW_Y,
                    ARROW_WIDTH,
                    ARROW_HEIGHT
            )) {
                nextSpread();
                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode
                == InputConstants.KEY_LEFT) {
            previousSpread();
            return true;
        }

        if (keyCode
                == InputConstants.KEY_RIGHT) {
            nextSpread();
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    private boolean insideNativeBounds(
            double mouseX,
            double mouseY,
            int nativeX,
            int nativeY,
            int nativeWidth,
            int nativeHeight
    ) {
        double x =
                bookX
                        + nativeX
                        * bookScale;

        double y =
                bookY
                        + nativeY
                        * bookScale;

        double width =
                nativeWidth
                        * bookScale;

        double height =
                nativeHeight
                        * bookScale;

        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private void previousSpread() {
        if (leftPageIndex > 0) {
            leftPageIndex =
                    Math.max(
                            0,
                            leftPageIndex - 2
                    );
        }
    }

    private void nextSpread() {
        if (leftPageIndex + 2
                < displayPages.size()) {
            leftPageIndex += 2;
        }
    }
}
