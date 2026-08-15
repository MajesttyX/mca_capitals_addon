package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.network.OpenBetrothalSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BetrothalSelectionScreen extends CapitalNoBlurScreen {

    private static final int PAGE_SIZE = 5;

    private final UUID capitalId;
    private final Component villageName;
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
        super(Component.translatable("mcacapitals.system.betrothal_selection_screen.betrothal_petition"));
        this.capitalId = capitalId;
        this.villageName = villageName == null || villageName.isBlank()
                ? Component.translatable("mcacapitals.system.common.unknown_village")
                : "Unknown Village".equals(villageName)
                ? Component.translatable("mcacapitals.system.common.unknown_village")
                : Component.literal(villageName);
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
                Button.builder(Component.translatable("mcacapitals.system.betrothal_selection_screen.your_betrothal"), btn -> switchMode(Mode.PLAYER))
                        .bounds(centerX - 110, 16, 105, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.betrothal_selection_screen.recommend_match"), btn -> switchMode(Mode.RECOMMENDATION))
                        .bounds(centerX + 5, 16, 105, 20)
                        .build()
        );

        if (mode == Mode.RECOMMENDATION && selectedRecommendationFirst != null) {
            addRenderableWidget(
                    Button.builder(Component.translatable("mcacapitals.system.betrothal_selection_screen.choose_different_first"), btn -> {
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
                    Button.builder(candidateNameComponent(candidate.name()), btn -> choose(candidate.id()))
                            .bounds(centerX - 110, y, 220, 20)
                            .build()
            );
        }

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.betrothal_selection_screen.previous"), btn -> {
                            if (page > 0) {
                                page--;
                                init();
                            }
                        })
                        .bounds(centerX - 110, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.betrothal_selection_screen.close"), btn -> onClose())
                        .bounds(centerX - 35, this.height - 40, 70, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.betrothal_selection_screen.next"), btn -> {
                            if ((page + 1) * PAGE_SIZE < visibleCandidates.size()) {
                                page++;
                                init();
                            }
                        })
                        .bounds(centerX + 40, this.height - 40, 70, 20)
                        .build()
        );
    }

    private static Component candidateNameComponent(Component name) {
        if (name == null || name.getString().isBlank()) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }
        return name;
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

    private Component getSelectedFirstName() {
        if (selectedRecommendationFirst == null) {
            return Component.empty();
        }

        for (OpenBetrothalSelectionPacket.Candidate candidate : recommendationCandidates) {
            if (candidate.id().equals(selectedRecommendationFirst)) {
                return candidate.name();
            }
        }

        return Component.literal(selectedRecommendationFirst.toString());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component title;
        if (mode == Mode.PLAYER) {
            title = Component.translatable("mcacapitals.system.betrothal_selection_screen.choose_noble_for_your_betrothal");
        } else if (selectedRecommendationFirst == null) {
            title = Component.translatable("mcacapitals.system.betrothal_selection_screen.choose_first_villager");
        } else {
            title = Component.translatable(
                    "mcacapitals.system.betrothal_selection_screen.choose_match_for",
                    candidateNameComponent(getSelectedFirstName())
            );
        }

        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2, 2, 0xFFFFFF, false);

        int villageWidth = this.font.width(villageName);
        guiGraphics.drawString(this.font, villageName, (this.width - villageWidth) / 2, mode == Mode.RECOMMENDATION && selectedRecommendationFirst != null ? 62 : 40, 0xCCCCCC, false);

        List<OpenBetrothalSelectionPacket.Candidate> visibleCandidates = getVisibleCandidates();
        int pageCount = Math.max(1, (int) Math.ceil(visibleCandidates.size() / (double) PAGE_SIZE));
        Component footer = Component.translatable(
                "mcacapitals.system.common.page",
                page + 1,
                pageCount
        );
        int footerWidth = this.font.width(footer);
        guiGraphics.drawString(this.font, footer, (this.width - footerWidth) / 2, this.height - 52, 0xAAAAAA, false);

        if (mode == Mode.PLAYER && playerCandidates.isEmpty()) {
            Component line = Component.translatable("mcacapitals.system.betrothal_selection_screen.no_eligible_nobles_available");
            int width = this.font.width(line);
            guiGraphics.drawString(this.font, line, (this.width - width) / 2, 72, 0xFFAAAA, false);
        }

        if (mode == Mode.RECOMMENDATION && recommendationCandidates.size() < 2) {
            Component line = Component.translatable("mcacapitals.system.betrothal_selection_screen.not_enough_residents_available");
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