package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.network.OpenDecreeOfTheHousePacket;
import com.majesttyx.mcacapitals.network.SubmitDecreeOfTheHousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class DecreeOfTheHouseScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 150;

    private final UUID targetId;
    private final boolean playerTarget;
    private final boolean houseFounded;

    private EditBox surnameBox;
    private EditBox houseWordsBox;
    private Component errorMessage = Component.empty();

    private String firstName = "";
    private String surname = "";
    private String houseWords = "";

    public DecreeOfTheHouseScreen(OpenDecreeOfTheHousePacket packet) {
        super(Component.literal(packet.playerTarget() ? "Revise Your House" : "Decree of the House"));
        this.targetId = packet.targetId();
        this.playerTarget = packet.playerTarget();
        this.houseFounded = packet.houseFounded() || packet.playerTarget();
    }

    public DecreeOfTheHouseScreen withInitialValues(String firstName, String surname, String houseWords) {
        this.firstName = firstName == null ? "" : firstName;
        this.surname = surname == null ? "" : surname;
        this.houseWords = houseWords == null ? "" : houseWords;
        return this;
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        surnameBox = new EditBox(
                this.font,
                left + 28,
                top + 48,
                184,
                18,
                Component.literal(playerTarget ? "House Name" : "Surname / House Name")
        );
        surnameBox.setMaxLength(playerTarget ? 20 : 40);
        surnameBox.setValue(surname);
        surnameBox.setResponder(value -> errorMessage = Component.empty());
        addRenderableWidget(surnameBox);

        houseWordsBox = new EditBox(
                this.font,
                left + 28,
                top + 86,
                184,
                18,
                Component.literal("House Words")
        );
        houseWordsBox.setMaxLength(80);
        houseWordsBox.setValue(houseWords);
        houseWordsBox.setEditable(houseFounded);
        houseWordsBox.active = houseFounded;
        houseWordsBox.setResponder(value -> errorMessage = Component.empty());
        addRenderableWidget(houseWordsBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> submit())
                .bounds(left + 42, top + 118, 70, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left + 128, top + 118, 70, 20)
                .build());

        setInitialFocus(surnameBox);
    }

    private void submit() {
        String surnameValue = surnameBox == null ? "" : surnameBox.getValue();
        String houseWordsValue = houseWordsBox == null ? "" : houseWordsBox.getValue();

        if (playerTarget) {
            if (!isValidNamePart(surnameValue, 2, 20)) {
                errorMessage = Component.literal("House name must be 2-20 valid characters.");
                return;
            }

            if (!houseWordsValue.trim().isBlank() && !isValidHouseWords(houseWordsValue)) {
                errorMessage = Component.literal("House Words must be 2-80 valid characters.");
                return;
            }
        } else {
            if (!isValidNamePart(surnameValue, 2, 40)) {
                errorMessage = Component.literal("Surname must be 2-40 valid characters.");
                return;
            }

            if (houseFounded && !isValidHouseWords(houseWordsValue)) {
                errorMessage = Component.literal("House Words must be 2-80 valid characters.");
                return;
            }
        }

        PacketDistributor.sendToServer(new SubmitDecreeOfTheHousePacket(
                targetId,
                playerTarget,
                firstName,
                surnameValue,
                houseFounded ? houseWordsValue : ""
        ));

        Minecraft.getInstance().setScreen(null);
    }

    private boolean isValidNamePart(String value, int min, int max) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() < min || normalized.length() > max || normalized.contains("§")) {
            return false;
        }

        return normalized.matches("[A-Za-z][A-Za-z '\\-]*");
    }

    private boolean isValidHouseWords(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() < 2 || normalized.length() > 80 || normalized.contains("§")) {
            return false;
        }

        return normalized.matches("[A-Za-z][A-Za-z '\\-,]*");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xCC1F1A12);
        graphics.drawString(this.font, this.title, left + 68, top + 12, 0xFFFFFF, false);

        graphics.drawString(
                this.font,
                playerTarget ? "House Name" : "Surname / House Name",
                left + 28,
                top + 37,
                0xE8D8B0,
                false
        );

        graphics.drawString(
                this.font,
                "House Words",
                left + 28,
                top + 75,
                houseFounded ? 0xE8D8B0 : 0x777777,
                false
        );

        if (!houseFounded) {
            graphics.drawString(this.font, "Only established Houses have House Words.", left + 28, top + 106, 0x888888, false);
        }

        if (playerTarget) {
            graphics.drawString(this.font, "Leave House Words blank to set them later.", left + 28, top + 106, 0x888888, false);
        }

        if (errorMessage != null && errorMessage != Component.empty()) {
            graphics.drawString(this.font, errorMessage, left + 18, top + PANEL_HEIGHT + 8, 0xFF5555, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}