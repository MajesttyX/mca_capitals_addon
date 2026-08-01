package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.network.OpenSealedPurseCaseSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SealedPurseCaseSelectionScreen extends CapitalNoBlurScreen {

    private static final int PAGE_SIZE = 5;

    private final UUID capitalId;
    private final String villageName;
    private final List<OpenSealedPurseCaseSelectionPacket.CaseEntry> cases;

    private int page = 0;

    public SealedPurseCaseSelectionScreen(
            UUID capitalId,
            String villageName,
            List<OpenSealedPurseCaseSelectionPacket.CaseEntry> cases
    ) {
        super(Component.literal("Gift Sealed Purse"));
        this.capitalId = capitalId;
        this.villageName = villageName == null || villageName.isBlank() ? "this capital" : villageName;
        this.cases = new ArrayList<>(cases);
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = this.width / 2;
        int startY = 72;
        int startIndex = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = startIndex + i;
            if (index >= cases.size()) {
                break;
            }

            OpenSealedPurseCaseSelectionPacket.CaseEntry caseEntry = cases.get(index);
            int y = startY + (i * 28);
            String label = trim(caseEntry.name() + " - " + caseEntry.status(), 220);

            addRenderableWidget(
                    Button.builder(Component.literal(label), button -> choose(caseEntry.id()))
                            .bounds(centerX - 120, y, 240, 20)
                            .build()
            );
        }

        addRenderableWidget(
                Button.builder(Component.literal("Previous"), button -> {
                            if (page > 0) {
                                page--;
                                init();
                            }
                        })
                        .bounds(centerX - 110, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Cancel"), button -> onClose())
                        .bounds(centerX - 35, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Next"), button -> {
                            if ((page + 1) * PAGE_SIZE < cases.size()) {
                                page++;
                                init();
                            }
                        })
                        .bounds(centerX + 40, this.height - 40, 70, 20)
                        .build()
        );
    }

    private void choose(UUID targetId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand("capitallaw sealed_purse " + capitalId + " " + targetId);
        }
        onClose();
    }

    private String trim(String text, int width) {
        if (text == null) {
            return "Unknown Case";
        }

        return this.font.plainSubstrByWidth(text, width);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(this.font, "Gift Sealed Purse", centerX, 18, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, villageName, centerX, 32, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, "Only one record may be softened by this purse.", centerX, 48, 0xAAAAAA);

        int pageCount = Math.max(1, (int) Math.ceil(cases.size() / (double) PAGE_SIZE));
        String footer = "Page " + (page + 1) + " / " + pageCount;
        guiGraphics.drawCenteredString(this.font, footer, centerX, this.height - 54, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}