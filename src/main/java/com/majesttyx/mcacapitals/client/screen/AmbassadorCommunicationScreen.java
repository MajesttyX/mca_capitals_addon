package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class AmbassadorCommunicationScreen extends CapitalNoBlurScreen {

    private final OpenAmbassadorCommunicationPacket packet;
    private final List<HoverTarget> hoverTargets = new ArrayList<>();

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
        hoverTargets.clear();

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
                            7,
                            availableHeight / 24
                    )
            );
        }

        return Math.max(
                1,
                Math.min(
                        4,
                        availableHeight / 48
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
                            + visibleIndex * 48;

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

            String tooltip =
                    entry.enabled()
                            ? entry.lineOne()
                            : entry.disabledReason();

            if (!tooltip.isBlank()) {
                hoverTargets.add(
                        new HoverTarget(
                                button,
                                Component.literal(tooltip)
                        )
                );
            }
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
                            + visibleIndex * 24;

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
                                    centerX - 120,
                                    y,
                                    240,
                                    20
                            )
                            .build();

            button.active =
                    action.enabled()
                            && !action.command().isBlank();

            addRenderableWidget(button);

            if (!action.description().isBlank()) {
                hoverTargets.add(
                        new HoverTarget(
                                button,
                                Component.literal(
                                        action.description()
                                )
                        )
                );
            }
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

        guiGraphics.drawCenteredString(
                this.font,
                packet.title(),
                centerX,
                16,
                0xFFFFFF
        );

        if (!packet.subtitle().isBlank()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    packet.subtitle(),
                    centerX,
                    30,
                    0xCCCCCC
            );
        }

        if (!packet.message().isBlank()) {
            drawWrappedCentered(
                    guiGraphics,
                    packet.message(),
                    44,
                    0xAAAAAA
            );
        }

        if (packet.mode()
                != OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS) {
            renderEntries(guiGraphics);
        }

        int pageCount =
                getPageCount();

        if (pageCount > 1) {
            guiGraphics.drawCenteredString(
                    this.font,
                    "Page "
                            + (page + 1)
                            + " / "
                            + pageCount,
                    centerX,
                    this.height - 46,
                    0xAAAAAA
            );
        }

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        for (HoverTarget hoverTarget :
                hoverTargets) {
            if (hoverTarget.button()
                    .isMouseOver(
                            mouseX,
                            mouseY
                    )) {
                guiGraphics.renderTooltip(
                        this.font,
                        hoverTarget.tooltip(),
                        mouseX,
                        mouseY
                );

                break;
            }
        }
    }

    private void renderEntries(
            GuiGraphics guiGraphics
    ) {
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

            int y =
                    startY
                            + visibleIndex * 48;

            boolean hasButton =
                    !entry.buttonLabel().isBlank()
                            && !entry.command().isBlank();

            if (!hasButton) {
                guiGraphics.drawCenteredString(
                        this.font,
                        entry.heading(),
                        centerX,
                        y,
                        0xFFD36A
                );
            }

            int detailY =
                    hasButton
                            ? y + 23
                            : y + 12;

            drawDetailLine(
                    guiGraphics,
                    entry.lineOne(),
                    centerX,
                    detailY,
                    0xDDDDDD
            );

            drawDetailLine(
                    guiGraphics,
                    entry.lineTwo(),
                    centerX,
                    detailY + 10,
                    0xBBBBBB
            );

            drawDetailLine(
                    guiGraphics,
                    entry.lineThree(),
                    centerX,
                    detailY + 20,
                    0x999999
            );
        }
    }

    private void drawDetailLine(
            GuiGraphics guiGraphics,
            String text,
            int centerX,
            int y,
            int color
    ) {
        if (text == null
                || text.isBlank()) {
            return;
        }

        guiGraphics.drawCenteredString(
                this.font,
                text,
                centerX,
                y,
                color
        );
    }

    private void drawWrappedCentered(
            GuiGraphics guiGraphics,
            String text,
            int startY,
            int color
    ) {
        List<net.minecraft.util.FormattedCharSequence> lines =
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

        for (net.minecraft.util.FormattedCharSequence line :
                lines) {
            guiGraphics.drawCenteredString(
                    this.font,
                    line,
                    centerX,
                    y,
                    color
            );

            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record HoverTarget(
            Button button,
            Component tooltip
    ) {
    }
}