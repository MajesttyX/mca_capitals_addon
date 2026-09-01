package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.client.screen.CapitalNoBlurScreen;
import com.majesttyx.mcacapitals.client.screen.HouseDocumentWidgets;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.SubmitDeclarationOfSeparationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public final class DeclarationOfSeparationClient {

    private DeclarationOfSeparationClient() {
    }

    public static void open(
            UUID targetId,
            String targetName,
            String currentHouse,
            String currentHouseWords
    ) {
        Minecraft.getInstance()
                .setScreen(
                        new DeclarationScreen(
                                targetId,
                                targetName,
                                currentHouse,
                                currentHouseWords
                        )
                );
    }

    private static final class DeclarationScreen
            extends CapitalNoBlurScreen {

        private static final int PANEL_WIDTH = 300;
        private static final int PANEL_HEIGHT = 300;
        private static final int CONTENT_MARGIN = 46;

        private static final int TEXT_COLOR = 0x2F2418;
        private static final int MUTED_COLOR = 0x625849;

        private final UUID targetId;
        private final String targetName;
        private final String currentHouse;
        private final String currentHouseWords;

        private EditBox houseName;
        private EditBox houseWords;

        private int panelX;
        private int panelY;

        private DeclarationScreen(
                UUID targetId,
                String targetName,
                String currentHouse,
                String currentHouseWords
        ) {
            super(
                    Component.translatable(
                            "mcacapitals.declaration_of_separation.screen.title"
                    )
            );

            this.targetId = targetId;
            this.targetName =
                    targetName == null
                            ? ""
                            : targetName;
            this.currentHouse =
                    currentHouse == null
                            ? ""
                            : currentHouse;
            this.currentHouseWords =
                    currentHouseWords == null
                            ? ""
                            : currentHouseWords;
        }

        @Override
        protected void init() {
            clearWidgets();

            panelX =
                    (this.width - PANEL_WIDTH) / 2;
            panelY =
                    (this.height - PANEL_HEIGHT) / 2;

            int fieldX =
                    panelX + CONTENT_MARGIN;
            int fieldWidth =
                    PANEL_WIDTH
                            - CONTENT_MARGIN * 2;

            this.houseName =
                    new EditBox(
                            this.font,
                            fieldX,
                            panelY + 128,
                            fieldWidth,
                            20,
                            Component.translatable(
                                    "mcacapitals.declaration_of_separation.screen.new_house_name"
                            )
                    );
            this.houseName.setMaxLength(20);
            this.addRenderableWidget(
                    this.houseName
            );

            this.houseWords =
                    new EditBox(
                            this.font,
                            fieldX,
                            panelY + 174,
                            fieldWidth,
                            20,
                            Component.translatable(
                                    "mcacapitals.declaration_of_separation.screen.house_words"
                            )
                    );
            this.houseWords.setMaxLength(80);
            this.houseWords.setValue(
                    currentHouseWords
            );
            this.addRenderableWidget(
                    this.houseWords
            );

            this.addRenderableWidget(
                    new HouseDocumentWidgets.FlatButton(
                            fieldX,
                            panelY + 220,
                            fieldWidth,
                            22,
                            Component.translatable(
                                    "mcacapitals.declaration_of_separation.screen.confirm"
                            ),
                            this::confirm
                    )
            );

            this.addRenderableWidget(
                    new HouseDocumentWidgets.FlatButton(
                            fieldX,
                            panelY + 250,
                            fieldWidth,
                            22,
                            Component.translatable(
                                    "gui.cancel"
                            ),
                            this::onClose
                    )
            );

            setInitialFocus(houseName);
        }

        private void confirm() {
            ModNetwork.sendToServer(
                    new SubmitDeclarationOfSeparationPacket(
                            targetId,
                            houseName.getValue(),
                            houseWords.getValue()
                    )
            );
            onClose();
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            HouseDocumentWidgets.renderWideScroll(
                    graphics,
                    panelX,
                    panelY,
                    PANEL_WIDTH,
                    PANEL_HEIGHT
            );

            int fieldX =
                    panelX + CONTENT_MARGIN;
            int fieldWidth =
                    PANEL_WIDTH
                            - CONTENT_MARGIN * 2;

            drawCenteredNoShadow(
                    graphics,
                    this.title,
                    panelX + PANEL_WIDTH / 2,
                    panelY + 42,
                    TEXT_COLOR
            );

            String shortTarget =
                    this.font.plainSubstrByWidth(
                            targetName,
                            fieldWidth
                    );

            drawCenteredNoShadow(
                    graphics,
                    Component.literal(
                            shortTarget
                    ),
                    panelX + PANEL_WIDTH / 2,
                    panelY + 64,
                    TEXT_COLOR
            );

            List<net.minecraft.util.FormattedCharSequence>
                    currentHouseLines =
                    this.font.split(
                            Component.translatable(
                                    "mcacapitals.declaration_of_separation.screen.current_house",
                                    currentHouse
                            ),
                            fieldWidth
                    );

            int lineCount =
                    Math.min(
                            2,
                            currentHouseLines.size()
                    );

            for (int i = 0;
                 i < lineCount;
                 i++) {
                graphics.drawString(
                        this.font,
                        currentHouseLines.get(i),
                        fieldX,
                        panelY + 88 + i * 10,
                        MUTED_COLOR,
                        false
                );
            }

            graphics.drawString(
                    this.font,
                    Component.translatable(
                            "mcacapitals.declaration_of_separation.screen.new_house_name"
                    ),
                    fieldX,
                    panelY + 114,
                    TEXT_COLOR,
                    false
            );

            graphics.drawString(
                    this.font,
                    Component.translatable(
                            "mcacapitals.declaration_of_separation.screen.house_words"
                    ),
                    fieldX,
                    panelY + 160,
                    TEXT_COLOR,
                    false
            );

            super.render(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }

        private void drawCenteredNoShadow(
                GuiGraphics graphics,
                Component component,
                int centerX,
                int y,
                int color
        ) {
            graphics.drawString(
                    this.font,
                    component,
                    centerX
                            - this.font.width(component) / 2,
                    y,
                    color,
                    false
            );
        }
    }
}
