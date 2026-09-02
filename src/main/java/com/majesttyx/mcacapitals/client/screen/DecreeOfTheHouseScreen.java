package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenDecreeOfTheHousePacket;
import com.majesttyx.mcacapitals.network.SubmitDecreeOfTheHousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class DecreeOfTheHouseScreen
        extends CapitalNoBlurScreen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 300;

    private static final int CONTENT_MARGIN = 46;
    private static final int TEXT_COLOR = 0x2F2418;
    private static final int MUTED_COLOR = 0x625849;
    private static final int ERROR_COLOR = 0x9A2929;

    private final UUID targetId;
    private final boolean playerTarget;
    private final boolean houseFounded;

    private EditBox surnameBox;
    private EditBox houseWordsBox;
    private Component errorMessage =
            Component.empty();

    private String firstName = "";
    private String surname = "";
    private String houseWords = "";

    private int panelX;
    private int panelY;

    public DecreeOfTheHouseScreen(
            OpenDecreeOfTheHousePacket packet
    ) {
        super(
                Component.translatable(
                        packet.playerTarget()
                                ? "mcacapitals.ui.decree_house.title.player"
                                : "mcacapitals.ui.decree_house.title.villager"
                )
        );

        this.targetId = packet.targetId();
        this.playerTarget = packet.playerTarget();
        this.houseFounded =
                packet.houseFounded()
                        || packet.playerTarget();
    }

    public DecreeOfTheHouseScreen
    withInitialValues(
            String firstName,
            String surname,
            String houseWords
    ) {
        this.firstName =
                firstName == null ? "" : firstName;
        this.surname =
                surname == null ? "" : surname;
        this.houseWords =
                houseWords == null ? "" : houseWords;
        return this;
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

        surnameBox =
                new EditBox(
                        this.font,
                        fieldX,
                        panelY + 98,
                        fieldWidth,
                        20,
                        Component.translatable(
                                playerTarget
                                        ? "mcacapitals.ui.decree_house.house_name"
                                        : "mcacapitals.ui.decree_house.surname_house_name"
                        )
                );
        surnameBox.setMaxLength(
                playerTarget ? 20 : 40
        );
        surnameBox.setValue(surname);
        surnameBox.setResponder(
                value ->
                        errorMessage =
                                Component.empty()
        );
        addRenderableWidget(surnameBox);

        houseWordsBox =
                new EditBox(
                        this.font,
                        fieldX,
                        panelY + 154,
                        fieldWidth,
                        20,
                        Component.translatable(
                                "mcacapitals.ui.decree_house.house_words"
                        )
                );
        houseWordsBox.setMaxLength(80);
        houseWordsBox.setValue(houseWords);
        houseWordsBox.setEditable(
                houseFounded
        );
        houseWordsBox.active =
                houseFounded;
        houseWordsBox.setResponder(
                value ->
                        errorMessage =
                                Component.empty()
        );
        addRenderableWidget(houseWordsBox);

        int gap = 10;
        int buttonWidth =
                (fieldWidth - gap) / 2;

        addRenderableWidget(
                new HouseDocumentWidgets.FlatButton(
                        fieldX,
                        panelY + 224,
                        buttonWidth,
                        22,
                        Component.translatable(
                                "mcacapitals.ui.decree_house.save"
                        ),
                        this::submit
                )
        );

        addRenderableWidget(
                new HouseDocumentWidgets.FlatButton(
                        fieldX + buttonWidth + gap,
                        panelY + 224,
                        buttonWidth,
                        22,
                        Component.translatable(
                                "mcacapitals.ui.decree_house.cancel"
                        ),
                        this::onClose
                )
        );

        setInitialFocus(surnameBox);
    }

    private void submit() {
        String surnameValue =
                surnameBox == null
                        ? ""
                        : surnameBox.getValue();

        String houseWordsValue =
                houseWordsBox == null
                        ? ""
                        : houseWordsBox.getValue();

        if (playerTarget) {
            if (!isValidNamePart(
                    surnameValue,
                    2,
                    20
            )) {
                errorMessage =
                        Component.translatable(
                                "mcacapitals.ui.decree_house.error.house_name"
                        );
                return;
            }

            if (!houseWordsValue
                    .trim()
                    .isBlank()
                    && !isValidHouseWords(
                    houseWordsValue
            )) {
                errorMessage =
                        Component.translatable(
                                "mcacapitals.ui.decree_house.error.house_words"
                        );
                return;
            }
        } else {
            if (!isValidNamePart(
                    surnameValue,
                    2,
                    40
            )) {
                errorMessage =
                        Component.translatable(
                                "mcacapitals.ui.decree_house.error.surname"
                        );
                return;
            }

            if (houseFounded
                    && !isValidHouseWords(
                    houseWordsValue
            )) {
                errorMessage =
                        Component.translatable(
                                "mcacapitals.ui.decree_house.error.house_words"
                        );
                return;
            }
        }

        ModNetwork.CHANNEL.sendToServer(
                new SubmitDecreeOfTheHousePacket(
                        targetId,
                        playerTarget,
                        firstName,
                        surnameValue,
                        houseFounded
                                ? houseWordsValue
                                : ""
                )
        );

        Minecraft.getInstance()
                .setScreen(null);
    }

    private boolean isValidNamePart(
            String value,
            int min,
            int max
    ) {
        if (value == null) {
            return false;
        }

        String normalized =
                value.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (normalized.length() < min
                || normalized.length() > max
                || normalized.contains("§")) {
            return false;
        }

        return normalized.matches(
                "[A-Za-z][A-Za-z '\\-]*"
        );
    }

    private boolean isValidHouseWords(
            String value
    ) {
        if (value == null) {
            return false;
        }

        String normalized =
                value.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (normalized.length() < 2
                || normalized.length() > 80
                || normalized.contains("§")) {
            return false;
        }

        return normalized.matches(
                "[A-Za-z][A-Za-z '\\-,]*"
        );
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
                panelY + 46,
                TEXT_COLOR
        );

        graphics.drawString(
                this.font,
                Component.translatable(
                        playerTarget
                                ? "mcacapitals.ui.decree_house.house_name"
                                : "mcacapitals.ui.decree_house.surname_house_name"
                ),
                fieldX,
                panelY + 84,
                TEXT_COLOR,
                false
        );

        graphics.drawString(
                this.font,
                Component.translatable(
                        "mcacapitals.ui.decree_house.house_words"
                ),
                fieldX,
                panelY + 140,
                houseFounded
                        ? TEXT_COLOR
                        : MUTED_COLOR,
                false
        );

        Component message =
                Component.empty();

        int messageColor =
                MUTED_COLOR;

        if (errorMessage != null
                && !errorMessage
                .getString()
                .isBlank()) {
            message = errorMessage;
            messageColor = ERROR_COLOR;
        } else if (!houseFounded) {
            message =
                    Component.translatable(
                            "mcacapitals.ui.decree_house.house_words_unavailable"
                    );
        } else if (playerTarget) {
            message =
                    Component.translatable(
                            "mcacapitals.ui.decree_house.house_words_optional"
                    );
        }

        if (!message
                .getString()
                .isBlank()) {
            List<net.minecraft.util.FormattedCharSequence>
                    lines =
                    this.font.split(
                            message,
                            fieldWidth
                    );

            int maxLines =
                    Math.min(
                            3,
                            lines.size()
                    );

            for (int i = 0;
                 i < maxLines;
                 i++) {
                graphics.drawString(
                        this.font,
                        lines.get(i),
                        fieldX,
                        panelY + 184 + i * 10,
                        messageColor,
                        false
                );
            }
        }

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
