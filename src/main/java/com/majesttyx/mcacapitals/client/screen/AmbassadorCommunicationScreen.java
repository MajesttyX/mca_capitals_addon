package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class AmbassadorCommunicationScreen extends CapitalNoBlurScreen {

    private static final int TEXT_COLOR = 0xFFFFFF;

    private final OpenAmbassadorCommunicationPacket packet;

    private int page;
    private int pageSize;

    public AmbassadorCommunicationScreen(
            OpenAmbassadorCommunicationPacket packet
    ) {
        super(Component.literal(packet.title()));
        this.packet = packet;
    }

    @Override
    protected void init() {
        clearWidgets();

        pageSize = calculatePageSize();

        int pageCount = getPageCount();

        if (page >= pageCount) {
            page = Math.max(0, pageCount - 1);
        }

        if (packet.mode()
                == OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS) {
            addActionButtons();
        } else {
            addEntryButtons();
        }

        addNavigationButtons(pageCount);
    }

    private int calculatePageSize() {
        int availableHeight =
                this.height
                        - getContentStartY()
                        - 50;

        if (packet.mode()
                == OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS) {
            return Math.max(
                    1,
                    Math.min(
                            5,
                            availableHeight / 44
                    )
            );
        }

        return Math.max(
                1,
                Math.min(
                        5,
                        availableHeight / 80
                )
        );
    }

    private int getPageCount() {
        int itemCount =
                packet.mode()
                        == OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS
                        ? packet.actions().size()
                        : packet.entries().size();

        return Math.max(
                1,
                (int) Math.ceil(
                        itemCount
                                / (double) pageSize
                )
        );
    }

    private void addEntryButtons() {
        int centerX = this.width / 2;
        int startIndex = page * pageSize;
        int startY = getContentStartY();

        for (int visibleIndex = 0;
             visibleIndex < pageSize;
             visibleIndex++) {
            int entryIndex =
                    startIndex + visibleIndex;

            if (entryIndex >= packet.entries().size()) {
                break;
            }

            OpenAmbassadorCommunicationPacket.Entry entry =
                    packet.entries().get(entryIndex);

            if (entry.buttonLabel().isBlank()
                    || entry.command().isBlank()) {
                continue;
            }

            int y =
                    startY
                            + visibleIndex * 80
                            + 14;

            Button button =
                    Button.builder(
                                    Component.literal(
                                            entry.buttonLabel()
                                    ),
                                    pressed ->
                                            executeCommand(
                                                    entry.command()
                                            )
                            )
                            .bounds(
                                    centerX - 130,
                                    y,
                                    260,
                                    20
                            )
                            .build();

            button.active = entry.enabled();

            addRenderableWidget(button);
        }
    }

    private void addActionButtons() {
        int centerX = this.width / 2;
        int startIndex = page * pageSize;
        int startY = getContentStartY();

        for (int visibleIndex = 0;
             visibleIndex < pageSize;
             visibleIndex++) {
            int actionIndex =
                    startIndex + visibleIndex;

            if (actionIndex >= packet.actions().size()) {
                break;
            }

            OpenAmbassadorCommunicationPacket.Action action =
                    packet.actions().get(actionIndex);

            int y =
                    startY
                            + visibleIndex * 44;

            Button button =
                    Button.builder(
                                    Component.literal(
                                            action.label()
                                    ),
                                    pressed ->
                                            executeCommand(
                                                    action.command()
                                            )
                            )
                            .bounds(
                                    centerX - 150,
                                    y,
                                    300,
                                    20
                            )
                            .build();

            button.active =
                    action.enabled()
                            && !action.command().isBlank();

            addRenderableWidget(button);
        }
    }

    private int getContentStartY() {
        if (packet.message().isBlank()) {
            return 56;
        }

        int lineCount =
                this.font.split(
                        Component.literal(
                                packet.message()
                        ),
                        Math.min(
                                360,
                                Math.max(
                                        80,
                                        this.width - 40
                                )
                        )
                ).size();

        return 48
                + lineCount * 10
                + 8;
    }

    private void addNavigationButtons(
            int pageCount
    ) {
        int centerX = this.width / 2;
        int bottomY = this.height - 32;

        String centerLabel =
                packet.backCommand().isBlank()
                        ? "Close"
                        : "Back";

        if (pageCount <= 1) {
            addRenderableWidget(
                    Button.builder(
                                    Component.literal(
                                            centerLabel
                                    ),
                                    pressed -> {
                                        if (packet.backCommand()
                                                .isBlank()) {
                                            onClose();
                                        } else {
                                            executeCommand(
                                                    packet.backCommand()
                                            );
                                        }
                                    }
                            )
                            .bounds(
                                    centerX - 50,
                                    bottomY,
                                    100,
                                    20
                            )
                            .build()
            );

            return;
        }

        Button previous =
                Button.builder(
                                Component.literal(
                                        "Previous"
                                ),
                                pressed -> {
                                    if (page > 0) {
                                        page--;
                                        init();
                                    }
                                }
                        )
                        .bounds(
                                centerX - 120,
                                bottomY,
                                72,
                                20
                        )
                        .build();

        previous.active = page > 0;

        addRenderableWidget(previous);

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        centerLabel
                                ),
                                pressed -> {
                                    if (packet.backCommand()
                                            .isBlank()) {
                                        onClose();
                                    } else {
                                        executeCommand(
                                                packet.backCommand()
                                        );
                                    }
                                }
                        )
                        .bounds(
                                centerX - 36,
                                bottomY,
                                72,
                                20
                        )
                        .build()
        );

        Button next =
                Button.builder(
                                Component.literal(
                                        "Next"
                                ),
                                pressed -> {
                                    if (page + 1
                                            < pageCount) {
                                        page++;
                                        init();
                                    }
                                }
                        )
                        .bounds(
                                centerX + 48,
                                bottomY,
                                72,
                                20
                        )
                        .build();

        next.active =
                page + 1
                        < pageCount;

        addRenderableWidget(next);
    }

    private void executeCommand(
            String command
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.player.connection == null
                || command == null
                || command.isBlank()) {
            return;
        }

        String normalized =
                command.startsWith("/")
                        ? command.substring(1)
                        : command;

        minecraft.setScreen(null);

        minecraft.player.connection.sendCommand(
                normalized
        );
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int centerX = this.width / 2;

        renderAmbassadorPanel(guiGraphics);

        drawCenteredNoShadow(
                guiGraphics,
                packet.title(),
                centerX,
                16,
                TEXT_COLOR
        );

        if (!packet.subtitle().isBlank()) {
            drawCenteredNoShadow(
                    guiGraphics,
                    packet.subtitle(),
                    centerX,
                    30,
                    TEXT_COLOR
            );
        }

        if (!packet.message().isBlank()) {
            drawWrappedCentered(
                    guiGraphics,
                    packet.message(),
                    44,
                    TEXT_COLOR
            );
        }

        if (packet.mode()
                == OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS) {
            renderActions(guiGraphics);
        } else {
            renderEntries(guiGraphics);
        }

        int pageCount =
                getPageCount();

        if (pageCount > 1) {
            drawCenteredNoShadow(
                    guiGraphics,
                    "Page "
                            + (page + 1)
                            + " / "
                            + pageCount,
                    centerX,
                    this.height - 46,
                    TEXT_COLOR
            );
        }

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void renderAmbassadorPanel(
            GuiGraphics guiGraphics
    ) {
        int panelWidth = Math.min(
                440,
                Math.max(
                        180,
                        this.width - 16
                )
        );
        int left = (this.width - panelWidth) / 2;
        int right = left + panelWidth;
        int top = 8;
        int bottom = Math.max(top + 40, this.height - 40);

        guiGraphics.fill(
                left,
                top,
                right,
                bottom,
                0xA0404040
        );
        guiGraphics.fill(
                left,
                top,
                right,
                top + 1,
                0xC0909090
        );
        guiGraphics.fill(
                left,
                bottom - 1,
                right,
                bottom,
                0xC0909090
        );
        guiGraphics.fill(
                left,
                top,
                left + 1,
                bottom,
                0xC0909090
        );
        guiGraphics.fill(
                right - 1,
                top,
                right,
                bottom,
                0xC0909090
        );
    }

    private void renderActions(
            GuiGraphics guiGraphics
    ) {
        int centerX = this.width / 2;
        int startIndex = page * pageSize;
        int startY = getContentStartY();
        int textWidth = Math.min(
                360,
                Math.max(120, this.width - 40)
        );

        for (int visibleIndex = 0;
             visibleIndex < pageSize;
             visibleIndex++) {
            int actionIndex = startIndex + visibleIndex;

            if (actionIndex >= packet.actions().size()) {
                break;
            }

            OpenAmbassadorCommunicationPacket.Action action =
                    packet.actions().get(actionIndex);

            if (action.description().isBlank()) {
                continue;
            }

            int y = startY + visibleIndex * 44 + 23;
            List<FormattedCharSequence> lines =
                    this.font.split(
                            Component.literal(action.description()),
                            textWidth
                    );

            for (int lineIndex = 0;
                 lineIndex < Math.min(2, lines.size());
                 lineIndex++) {
                drawCenteredNoShadow(
                        guiGraphics,
                        lines.get(lineIndex),
                        centerX,
                        y + lineIndex * 10,
                        TEXT_COLOR
                );
            }
        }
    }

    private void renderEntries(
            GuiGraphics guiGraphics
    ) {
        int centerX = this.width / 2;
        int startIndex = page * pageSize;
        int startY = getContentStartY();
        int textWidth = Math.min(380, Math.max(120, this.width - 48));

        for (int visibleIndex = 0; visibleIndex < pageSize; visibleIndex++) {
            int entryIndex = startIndex + visibleIndex;
            if (entryIndex >= packet.entries().size()) {
                break;
            }

            OpenAmbassadorCommunicationPacket.Entry entry = packet.entries().get(entryIndex);
            int y = startY + visibleIndex * 80;
            boolean hasButton = !entry.buttonLabel().isBlank() && !entry.command().isBlank();

            drawCenteredNoShadow(
                    guiGraphics,
                    entry.heading(),
                    centerX,
                    y,
                    TEXT_COLOR
            );

            int detailY = hasButton ? y + 38 : y + 14;
            String firstLine = !entry.enabled() && !entry.disabledReason().isBlank()
                    ? entry.disabledReason()
                    : entry.lineOne();
            String details = joinDetails(firstLine, entry.lineTwo(), entry.lineThree());
            List<FormattedCharSequence> lines = this.font.split(
                    Component.literal(details),
                    textWidth
            );

            for (int lineIndex = 0; lineIndex < Math.min(3, lines.size()); lineIndex++) {
                drawCenteredNoShadow(
                        guiGraphics,
                        lines.get(lineIndex),
                        centerX,
                        detailY + lineIndex * 10,
                        TEXT_COLOR
                );
            }
        }
    }

    private String joinDetails(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(" — ");
            }
            result.append(value.trim());
        }
        return result.toString();
    }

    private void drawDetailLine(
            GuiGraphics guiGraphics,
            String text,
            int centerX,
            int y
    ) {
        if (text == null
                || text.isBlank()) {
            return;
        }

        drawCenteredNoShadow(
                guiGraphics,
                text,
                centerX,
                y,
                TEXT_COLOR
        );
    }

    private void drawWrappedCentered(
            GuiGraphics guiGraphics,
            String text,
            int startY,
            int color
    ) {
        List<FormattedCharSequence> lines =
                this.font.split(
                        Component.literal(text),
                        Math.min(
                                360,
                                Math.max(
                                        80,
                                        this.width - 40
                                )
                        )
                );

        int centerX = this.width / 2;
        int y = startY;

        for (FormattedCharSequence line : lines) {
            drawCenteredNoShadow(
                    guiGraphics,
                    line,
                    centerX,
                    y,
                    color
            );

            y += 10;
        }
    }

    private void drawCenteredNoShadow(
            GuiGraphics guiGraphics,
            String text,
            int centerX,
            int y,
            int color
    ) {
        guiGraphics.drawString(
                this.font,
                text,
                centerX - this.font.width(text) / 2,
                y,
                color,
                false
        );
    }

    private void drawCenteredNoShadow(
            GuiGraphics guiGraphics,
            FormattedCharSequence text,
            int centerX,
            int y,
            int color
    ) {
        guiGraphics.drawString(
                this.font,
                text,
                centerX - this.font.width(text) / 2,
                y,
                color,
                false
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}