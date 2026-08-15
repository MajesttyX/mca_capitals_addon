package com.majesttyx.mcacapitals.client.screen;

import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class RoyalCharterDecisionScreen extends CapitalNoBlurScreen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("mcacapitals", "textures/gui/declaration_paper.png");

    private static final int BG_WIDTH = 160;
    private static final int BG_HEIGHT = 160;

    public RoyalCharterDecisionScreen() {
        super(Component.translatable("mcacapitals.system.royal_charter_decision_screen.royal_charter"));
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.royal_charter_decision_screen.choose_sovereign"), button -> {
                    if (getHeldCharter().isEmpty()) {
                        onClose();
                        return;
                    }
                    Minecraft.getInstance().setScreen(new RoyalCharterVillagerListScreen());
                }).bounds(left + 24, top + 82, 112, 20).build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.royal_charter_decision_screen.declare_myself"), button -> {
                    ItemStack stack = getHeldCharter();
                    if (stack.isEmpty() || !ModItemStackData.hasCustomData(stack)) {
                        onClose();
                        return;
                    }

                    CompoundTag tag = ModItemStackData.getCustomData(stack);
                    String capitalId = tag.getString(ModDataKeys.CAPITAL_ID);
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player != null && minecraft.player.connection != null) {
                        minecraft.player.connection.sendCommand("capitalfounding claimself " + capitalId);
                    }
                    onClose();
                }).bounds(left + 24, top + 106, 112, 20).build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("mcacapitals.system.royal_charter_decision_screen.remain_ungoverned"), button -> {
                    ItemStack stack = getHeldCharter();
                    if (stack.isEmpty() || !ModItemStackData.hasCustomData(stack)) {
                        onClose();
                        return;
                    }

                    CompoundTag tag = ModItemStackData.getCustomData(stack);
                    String capitalId = tag.getString(ModDataKeys.CAPITAL_ID);
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player != null && minecraft.player.connection != null) {
                        minecraft.player.connection.sendCommand("capitalfounding reject " + capitalId);
                    }
                    onClose();
                }).bounds(left + 24, top + 130, 112, 20).build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - BG_WIDTH) / 2;
        int top = (this.height - BG_HEIGHT) / 2;

        guiGraphics.blit(BACKGROUND, left, top, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        drawCenteredNoShadow(guiGraphics, Component.translatable("mcacapitals.system.royal_charter_decision_screen.royal_charter"), this.width / 2, top + 16, 0x3E2E1F);

        ItemStack stack = getHeldCharter();
        String villageName = ModItemStackData.hasCustomData(stack)
                ? ModItemStackData.getCustomData(stack).getString(ModDataKeys.VILLAGE_NAME)
                : "";
        Component villageNameComponent = villageName == null
                || villageName.isBlank()
                || "Unknown Village".equals(villageName)
                ? Component.translatable("mcacapitals.system.common.unknown_village")
                : Component.literal(villageName);

        drawWrappedCenteredNoShadow(guiGraphics,
                Component.translatable(
                        "mcacapitals.system.royal_charter_decision_screen.capital_status_prompt",
                        villageNameComponent
                ),
                this.width / 2, top + 36, 118, 0x3E2E1F);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
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

    private void drawCenteredNoShadow(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        int width = this.font.width(text);
        guiGraphics.drawString(this.font, text, centerX - width / 2, y, color, false);
    }

    private void drawWrappedCenteredNoShadow(GuiGraphics guiGraphics, Component text, int centerX, int startY, int maxWidth, int color) {
        Font font = this.font;
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, maxWidth);
        int y = startY;

        for (net.minecraft.util.FormattedCharSequence line : lines) {
            int width = font.width(line);
            guiGraphics.drawString(font, line, centerX - width / 2, y, color, false);
            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}