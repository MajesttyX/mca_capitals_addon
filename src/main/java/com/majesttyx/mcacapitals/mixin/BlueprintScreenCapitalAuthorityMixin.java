package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalRankRequirements;
import com.majesttyx.mcacapitals.client.BlueprintAuthorityClientCache;
import com.majesttyx.mcacapitals.network.SyncBlueprintAuthorityPacket;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.conczin.mca.client.gui.BlueprintScreen", remap = false)
public abstract class BlueprintScreenCapitalAuthorityMixin {

    @Shadow
    private Village village;

    @Shadow
    private String page;

    @Inject(method = "renderStats", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcacapitals$renderCapitalStats(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();

        if (village == null) {
            return;
        }

        Screen screen = (Screen) (Object) this;
        Font font = Minecraft.getInstance().font;
        SyncBlueprintAuthorityPacket authority = BlueprintAuthorityClientCache.getForVillage(village.getId());

        String displayTitle = authority == null ? "Stranger" : authority.displayTitle();
        int reputation = authority == null ? 0 : authority.reputation();
        int x = screen.width / 2 + ("rank".equals(page) ? -70 : 105);
        int y = screen.height / 2 - 50;

        graphics.drawString(font, Component.literal("Current Rank: " + displayTitle), x, y, -1);
        graphics.drawString(font, Component.literal("Reputation: " + reputation), x, y + 11, -1);
        graphics.drawString(font, Component.literal("Buildings: " + village.getBuildings().size()), x, y + 22, -1);
        graphics.drawString(
                font,
                Component.literal("Population: " + village.getPopulation() + "/" + village.getMaxPopulation()),
                x,
                y + 33,
                -1
        );
    }

    @Inject(method = "renderTasks", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcacapitals$renderCapitalRankRequirements(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();

        if (village == null) {
            return;
        }

        Screen screen = (Screen) (Object) this;
        Font font = Minecraft.getInstance().font;
        SyncBlueprintAuthorityPacket authority = BlueprintAuthorityClientCache.getForVillage(village.getId());
        int x = screen.width / 2 - 70;
        int y = screen.height / 2 + 2;

        graphics.drawString(
                font,
                Component.literal("Rank Requirements").withStyle(ChatFormatting.BOLD),
                x,
                y,
                -1
        );

        if (authority == null || !authority.activeCapital()) {
            graphics.drawString(
                    font,
                    Component.literal("Available only in an active Capital.").withStyle(ChatFormatting.RED),
                    x,
                    y + 14,
                    -1
            );
            return;
        }

        drawRequirement(
                graphics,
                font,
                x,
                y + 14,
                "Lordship",
                authority.reputation() + "/" + CapitalRankRequirements.LORD_REPUTATION + " reputation | "
                        + authority.masterProfessionals() + "/" + CapitalRankRequirements.LORD_MASTER_PROFESSIONALS
                        + " Master professionals",
                authority.reputation() >= CapitalRankRequirements.LORD_REPUTATION
                        && authority.masterProfessionals() >= CapitalRankRequirements.LORD_MASTER_PROFESSIONALS
        );

        drawRequirement(
                graphics,
                font,
                x,
                y + 36,
                "Lord Commander — one per Capital",
                authority.population() + "/" + CapitalRankRequirements.LORD_COMMANDER_POPULATION + " residents | "
                        + authority.reputation() + "/" + CapitalRankRequirements.LORD_COMMANDER_REPUTATION
                        + " reputation",
                authority.population() >= CapitalRankRequirements.LORD_COMMANDER_POPULATION
                        && authority.reputation() >= CapitalRankRequirements.LORD_COMMANDER_REPUTATION
        );

        drawRequirement(
                graphics,
                font,
                x,
                y + 58,
                "Dukedom",
                authority.population() + "/" + CapitalRankRequirements.DUKE_POPULATION + " residents | "
                        + authority.reputation() + "/" + CapitalRankRequirements.DUKE_REPUTATION + " reputation",
                authority.population() >= CapitalRankRequirements.DUKE_POPULATION
                        && authority.reputation() >= CapitalRankRequirements.DUKE_REPUTATION
        );

        String handProgress = authority.villagerSovereign()
                ? authority.reputation() + "/" + CapitalRankRequirements.HAND_CAPITAL_REPUTATION + " Capital | "
                + authority.sovereignReputation() + "/"
                + CapitalRankRequirements.HAND_SOVEREIGN_REPUTATION + " Sovereign"
                : "Requires a reigning villager Sovereign";

        drawRequirement(
                graphics,
                font,
                x,
                y + 80,
                "Hand of the Sovereign",
                handProgress,
                authority.villagerSovereign()
                        && authority.reputation() >= CapitalRankRequirements.HAND_CAPITAL_REPUTATION
                        && authority.sovereignReputation() >= CapitalRankRequirements.HAND_SOVEREIGN_REPUTATION
        );
    }

    @Unique
    private static void drawRequirement(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            String title,
            String progress,
            boolean complete
    ) {
        graphics.drawString(
                font,
                Component.literal(title).withStyle(ChatFormatting.GOLD),
                x,
                y,
                -1
        );
        graphics.drawString(
                font,
                Component.literal(progress)
                        .withStyle(complete ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                x,
                y + 10,
                -1
        );
    }
}