package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalRankRequirements;
import com.majesttyx.mcacapitals.client.BlueprintAuthorityClientCache;
import com.majesttyx.mcacapitals.network.SyncBlueprintAuthorityPacket;
import fabric.net.conczin.mca.server.world.data.Village;
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
@Mixin(targets = "fabric.net.conczin.mca.client.gui.BlueprintScreen", remap = false)
public abstract class BlueprintScreenCapitalAuthorityMixin {

    @Shadow
    private Village village;

    @Shadow
    private String page;

    @Inject(
            method = "renderStats",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$renderCapitalStats(
            GuiGraphics graphics,
            CallbackInfo ci
    ) {
        ci.cancel();

        if (village == null) {
            return;
        }

        Screen screen = (Screen) (Object) this;
        Font font = Minecraft.getInstance().font;
        SyncBlueprintAuthorityPacket authority = BlueprintAuthorityClientCache.getForVillage(village.getId());

        Component displayTitle = authority == null
                ? Component.translatable("mcacapitals.dynamic.rank.stranger")
                : authority.displayTitle();
        int reputation = authority == null ? 0 : authority.reputation();
        int x = screen.width / 2 + ("rank".equals(page) ? -70 : 105);
        int y = screen.height / 2 - 50;

        graphics.drawString(
                font,
                Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.current_rank",
                        displayTitle
                ),
                x,
                y,
                -1
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.reputation",
                        reputation
                ),
                x,
                y + 11,
                -1
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.buildings",
                        village.getBuildings().size()
                ),
                x,
                y + 22,
                -1
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.population",
                        village.getPopulation(),
                        village.getMaxPopulation()
                ),
                x,
                y + 33,
                -1
        );
    }

    @Inject(
            method = "renderTasks",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$renderCapitalRankRequirements(
            GuiGraphics graphics,
            CallbackInfo ci
    ) {
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
                Component.translatable("mcacapitals.system.blueprint_screen_capital_authority_mixin.rank_requirements").withStyle(ChatFormatting.BOLD),
                x,
                y,
                -1
        );

        if (authority == null || !authority.activeCapital()) {
            graphics.drawString(
                    font,
                    Component.translatable("mcacapitals.system.blueprint_screen_capital_authority_mixin.available_only_in_an_active_capital").withStyle(ChatFormatting.RED),
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
                Component.translatable("mcacapitals.system.blueprint_screen_capital_authority_mixin.lordship"),
                Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.lordship_progress",
                        authority.reputation(),
                        CapitalRankRequirements.LORD_REPUTATION,
                        authority.masterProfessionals(),
                        CapitalRankRequirements.LORD_MASTER_PROFESSIONALS
                ),
                authority.reputation() >= CapitalRankRequirements.LORD_REPUTATION
                        && authority.masterProfessionals() >= CapitalRankRequirements.LORD_MASTER_PROFESSIONALS
        );

        drawRequirement(
                graphics,
                font,
                x,
                y + 36,
                Component.translatable("mcacapitals.system.blueprint_screen_capital_authority_mixin.lord_commander_one_per_capital"),
                Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.lord_commander_progress",
                        authority.population(),
                        CapitalRankRequirements.LORD_COMMANDER_POPULATION,
                        authority.reputation(),
                        CapitalRankRequirements.LORD_COMMANDER_REPUTATION
                ),
                authority.population() >= CapitalRankRequirements.LORD_COMMANDER_POPULATION
                        && authority.reputation() >= CapitalRankRequirements.LORD_COMMANDER_REPUTATION
        );

        drawRequirement(
                graphics,
                font,
                x,
                y + 58,
                Component.translatable("mcacapitals.system.blueprint_screen_capital_authority_mixin.dukedom"),
                Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.dukedom_progress",
                        authority.population(),
                        CapitalRankRequirements.DUKE_POPULATION,
                        authority.reputation(),
                        CapitalRankRequirements.DUKE_REPUTATION
                ),
                authority.population() >= CapitalRankRequirements.DUKE_POPULATION
                        && authority.reputation() >= CapitalRankRequirements.DUKE_REPUTATION
        );

        Component handProgress = authority.villagerSovereign()
                ? Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.hand_progress",
                        authority.reputation(),
                        CapitalRankRequirements.HAND_CAPITAL_REPUTATION,
                        authority.sovereignReputation(),
                        CapitalRankRequirements.HAND_SOVEREIGN_REPUTATION
                )
                : Component.translatable(
                        "mcacapitals.system.blueprint_screen_capital_authority_mixin.requires_reigning_villager_sovereign"
                );

        drawRequirement(
                graphics,
                font,
                x,
                y + 80,
                Component.translatable("mcacapitals.system.blueprint_screen_capital_authority_mixin.hand_of_the_sovereign"),
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
            Component title,
            Component progress,
            boolean complete
    ) {
        graphics.drawString(
                font,
                title.copy().withStyle(ChatFormatting.GOLD),
                x,
                y,
                -1
        );
        graphics.drawString(
                font,
                progress.copy()
                        .withStyle(complete ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                x,
                y + 10,
                -1
        );
    }
}