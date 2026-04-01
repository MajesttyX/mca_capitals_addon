package com.example.mcacapitals.client.screen;

import com.example.mcacapitals.network.OpenBetrothalSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BetrothalSelectionScreen extends Screen {

    private static final int PAGE_SIZE = 5;

    private final UUID capitalId;
    private final String villageName;
    private final List<OpenBetrothalSelectionPacket.Candidate> playerCandidates;
    private final List<OpenBetrothalSelectionPacket.Candidate> recommendationCandidates;

    private Mode mode;
    private UUID selectedRecommendationFirst;
    private int page = 0;

    public BetrothalSelectionScreen(
            UUID capitalId,
            String villageName,
            List<OpenBetrothalSelectionPacket.Candidate> playerCandidates,
            List<OpenBetrothalSelectionPacket.Candidate> recommendationCandidates
    ) {
        super(Component.literal("Betrothal Petition"));
        this.capitalId = capitalId;
        this.villageName = villageName;
        this.playerCandidates = new ArrayList<>(playerCandidates);
        this.recommendationCandidates = new ArrayList<>(recommendationCandidates);

        if (!this.playerCandidates.isEmpty()) {
            this.mode = Mode.PLAYER;
        } else {
            this.mode = Mode.RECOMMENDATION;
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = this.width / 2;

        addRenderableWidget(
                Button.builder(Component.literal("Your Betrothal"), btn -> switchMode(Mode.PLAYER))
                        .bounds(centerX - 110, 16, 105, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Recommend Match"), btn -> switchMode(Mode.RECOMMENDATION))
                        .bounds(centerX + 5, 16, 105, 20)
                        .build()
        );

        if (mode == Mode.RECOMMENDATION && selectedRecommendationFirst != null) {
            addRenderableWidget(
                    Button.builder(Component.literal("Choose Different First"), btn -> {
                                selectedRecommendationFirst = null;
                                page = 0;
                                init();
                            })
                            .bounds(centerX - 80, 42, 160, 20)
                            .build()
            );
        }

        List<OpenBetrothalSelectionPacket.Candidate> visibleCandidates = getVisibleCandidates();
        int startY = mode == Mode.RECOMMENDATION && selectedRecommendationFirst != null ? 72 : 50;
        int startIndex = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = startIndex + i;
            if (index >= visibleCandidates.size()) {
                break;
            }

            OpenBetrothalSelectionPacket.Candidate candidate = visibleCandidates.get(index);
            int y = startY + (i * 24);

            addRenderableWidget(
                    Button.builder(Component.literal(candidate.name()), btn -> choose(candidate.id()))
                            .bounds(centerX - 110, y, 220, 20)
                            .build()
            );
        }

        addRenderableWidget(
                Button.builder(Component.literal("Previous"), btn -> {
                            if (page > 0) {
                                page--;
                                init();
                            }
                        })
                        .bounds(centerX - 110, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Close"), btn -> onClose())
                        .bounds(centerX - 35, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Next"), btn -> {
                            if ((page + 1) * PAGE_SIZE < visibleCandidates.size()) {
                                page++;
                                init();
                            }
                        })
                        .bounds(centerX + 40, this.height - 40, 70, 20)
                        .build()
        );
    }

    private void switchMode(Mode nextMode) {
        this.mode = nextMode;
        this.page = 0;
        this.selectedRecommendationFirst = null;
        init();
    }

    private List<OpenBetrothalSelectionPacket.Candidate> getVisibleCandidates() {
        if (mode == Mode.PLAYER) {
            return playerCandidates;
        }

        if (selectedRecommendationFirst == null) {
            return recommendationCandidates;
        }

        List<OpenBetrothalSelectionPacket.Candidate> result = new ArrayList<>();
        for (OpenBetrothalSelectionPacket.Candidate candidate : recommendationCandidates) {
            if (!candidate.id().equals(selectedRecommendationFirst)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private void choose(UUID targetId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            onClose();
            return;
        }

        if (mode == Mode.PLAYER) {
            minecraft.player.connection.sendCommand("capitalpetition betrothal " + capitalId + " " + targetId);
            onClose();
            return;
        }

        if (selectedRecommendationFirst == null) {
            selectedRecommendationFirst = targetId;
            page = 0;
            init();
            return;
        }

        minecraft.player.connection.sendCommand(
                "capitalpetition recommend_betrothal " + capitalId + " " + selectedRecommendationFirst + " " + targetId
        );
        onClose();
    }

    private String getSelectedFirstName() {
        if (selectedRecommendationFirst == null) {
            return "";
        }

        for (OpenBetrothalSelectionPacket.Candidate candidate : recommendationCandidates) {
            if (candidate.id().equals(selectedRecommendationFirst)) {
                return candidate.name();
            }
        }

        return selectedRecommendationFirst.toString();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        String title;
        if (mode == Mode.PLAYER) {
            title = "Choose a noble for your betrothal";
        } else if (selectedRecommendationFirst == null) {
            title = "Choose the first villager";
        } else {
            title = "Choose a match for " + getSelectedFirstName();
        }

        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2, 2, 0xFFFFFF, false);

        int villageWidth = this.font.width(villageName);
        guiGraphics.drawString(this.font, villageName, (this.width - villageWidth) / 2, mode == Mode.RECOMMENDATION && selectedRecommendationFirst != null ? 62 : 40, 0xCCCCCC, false);

        List<OpenBetrothalSelectionPacket.Candidate> visibleCandidates = getVisibleCandidates();
        int pageCount = Math.max(1, (int) Math.ceil(visibleCandidates.size() / (double) PAGE_SIZE));
        String footer = "Page " + (page + 1) + " / " + pageCount;
        int footerWidth = this.font.width(footer);
        guiGraphics.drawString(this.font, footer, (this.width - footerWidth) / 2, this.height - 52, 0xAAAAAA, false);

        if (mode == Mode.PLAYER && playerCandidates.isEmpty()) {
            String line = "No eligible teen or adult nobles are available.";
            int width = this.font.width(line);
            guiGraphics.drawString(this.font, line, (this.width - width) / 2, 72, 0xFFAAAA, false);
        }

        if (mode == Mode.RECOMMENDATION && recommendationCandidates.size() < 2) {
            String line = "Not enough capital residents are available.";
            int width = this.font.width(line);
            guiGraphics.drawString(this.font, line, (this.width - width) / 2, 72, 0xFFAAAA, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Mode {
        PLAYER,
        RECOMMENDATION
    }
}