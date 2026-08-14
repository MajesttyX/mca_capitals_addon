package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.network.OpenAccusationSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccusationSelectionScreen extends CapitalNoBlurScreen {

    private static final int PAGE_SIZE = 6;

    private final UUID capitalId;
    private final Component villageName;
    private final List<OpenAccusationSelectionPacket.Candidate> candidates;

    private int page = 0;

    public AccusationSelectionScreen(
            UUID capitalId,
            String villageName,
            List<OpenAccusationSelectionPacket.Candidate> candidates
    ) {
        super(Component.translatable("mcacapitals.system.accusation_selection_screen.name_the_accused"));
        this.capitalId = capitalId;
        this.villageName = villageName == null || villageName.isBlank()
                ? Component.translatable("mcacapitals.ui.common.this_capital")
                : "Unknown Village".equals(villageName)
                ? Component.translatable("mcacapitals.system.common.unknown_village")
                : Component.literal(villageName);
        this.candidates = new ArrayList<>(candidates);
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = this.width / 2;
        int startY = 64;
        int startIndex = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = startIndex + i;
            if (index >= candidates.size()) {
                break;
            }

            OpenAccusationSelectionPacket.Candidate candidate = candidates.get(index);
            int y = startY + (i * 24);

            addRenderableWidget(
                    Button.builder(candidateNameComponent(candidate.name()), button -> accuse(candidate.id()))
                            .bounds(centerX - 110, y, 220, 20)
                            .build()
            );
        }

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.accusation_selection_screen.previous"), button -> {
                            if (page > 0) {
                                page--;
                                init();
                            }
                        })
                        .bounds(centerX - 110, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.accusation_selection_screen.cancel"), button -> onClose())
                        .bounds(centerX - 35, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.accusation_selection_screen.next"), button -> {
                            if ((page + 1) * PAGE_SIZE < candidates.size()) {
                                page++;
                                init();
                            }
                        })
                        .bounds(centerX + 40, this.height - 40, 70, 20)
                        .build()
        );
    }

    private static Component candidateNameComponent(String name) {
        if (name == null || name.isBlank() || "Unnamed".equals(name)) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }
        if ("Unknown".equals(name)) {
            return Component.translatable("mcacapitals.system.common.unknown");
        }
        return Component.literal(name);
    }

    private void accuse(UUID targetId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand("capitallaw accuse " + capitalId + " " + targetId);
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("mcacapitals.ui.accusation.title"),
                centerX,
                18,
                0xFFFFFF
        );
        guiGraphics.drawCenteredString(this.font, villageName, centerX, 32, 0xCCCCCC);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("mcacapitals.ui.accusation.instructions"),
                centerX,
                46,
                0xAAAAAA
        );

        int pageCount = Math.max(1, (int) Math.ceil(candidates.size() / (double) PAGE_SIZE));
        Component footer = Component.translatable(
                "mcacapitals.ui.pagination.page",
                page + 1,
                pageCount
        );
        guiGraphics.drawCenteredString(this.font, footer, centerX, this.height - 54, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}