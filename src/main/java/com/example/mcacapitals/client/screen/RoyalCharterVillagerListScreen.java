package com.example.mcacapitals.client.screen;

import com.example.mcacapitals.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RoyalCharterVillagerListScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("mcacapitals", "textures/gui/declaration_paper.png");

    private static final int BG_WIDTH = 160;
    private static final int BG_HEIGHT = 160;
    private static final int PAGE_SIZE = 5;

    private int page = 0;

    public RoyalCharterVillagerListScreen() {
        super(Component.literal("Choose Sovereign"));
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        List<Candidate> candidates = getCandidates();
        int startIndex = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = startIndex + i;
            if (index >= candidates.size()) {
                break;
            }

            Candidate candidate = candidates.get(index);
            int y = top + 36 + (i * 18);

            addRenderableWidget(
                    Button.builder(Component.literal(candidate.name), button -> appoint(candidate.id))
                            .bounds(left + 16, y, 128, 16)
                            .build()
            );
        }

        addRenderableWidget(
                Button.builder(Component.literal("Back"), button -> Minecraft.getInstance().setScreen(new RoyalCharterDecisionScreen()))
                        .bounds(left + 16, top + 134, 42, 18)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("<"), button -> {
                    if (page > 0) {
                        page--;
                        init();
                    }
                }).bounds(left + 64, top + 134, 18, 18).build()
        );

        addRenderableWidget(
                Button.builder(Component.literal(">"), button -> {
                    if ((page + 1) * PAGE_SIZE < candidates.size()) {
                        page++;
                        init();
                    }
                }).bounds(left + 88, top + 134, 18, 18).build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        guiGraphics.blit(BACKGROUND, left, top, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        drawCenteredNoShadow(guiGraphics, "Choose Sovereign", this.width / 2, top + 16, 0x3E2E1F);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void appoint(String villagerId) {
        ItemStack stack = getHeldCharter();
        if (stack.isEmpty() || !stack.hasTag()) {
            onClose();
            return;
        }

        String capitalId = stack.getTag().getString("CapitalId");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand("capitalfounding appoint " + capitalId + " " + villagerId);
        }
        onClose();
    }

    private List<Candidate> getCandidates() {
        List<Candidate> result = new ArrayList<>();

        ItemStack stack = getHeldCharter();
        if (stack.isEmpty() || !stack.hasTag()) {
            return result;
        }

        ListTag listTag = stack.getTag().getList("Candidates", 10);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag entry = listTag.getCompound(i);
            result.add(new Candidate(entry.getString("VillagerId"), entry.getString("VillagerName")));
        }

        return result;
    }

    private ItemStack getHeldCharter() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack main = minecraft.player.getMainHandItem();
        if (main.is(ModItems.ROYAL_CHARTER.get())) {
            return main;
        }

        ItemStack off = minecraft.player.getOffhandItem();
        if (off.is(ModItems.ROYAL_CHARTER.get())) {
            return off;
        }

        return ItemStack.EMPTY;
    }

    private void drawCenteredNoShadow(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        int width = this.font.width(text);
        guiGraphics.drawString(this.font, text, centerX - width / 2, y, color, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Candidate(String id, String name) {
    }
}