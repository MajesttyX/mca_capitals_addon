package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.client.VillagerIdentityClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Pseudo
@Mixin(targets = "net.conczin.mca.client.gui.InteractScreen", remap = false)
public abstract class InteractScreenIdentityMixin {

    @Unique
    private static final int MCACAPITALS_IDENTITY_X = 10;

    @Unique
    private static final int MCACAPITALS_SCREEN_MARGIN = 10;

    @Unique
    private static final int MCACAPITALS_BLOCK_GAP = 6;

    @Unique
    private static final int MCACAPITALS_TOOLTIP_HEIGHT = 12;

    @Inject(method = "drawTextPopups", at = @At("TAIL"), remap = false)
    private void mcacapitals$drawOriginAndHouse(GuiGraphics graphics, CallbackInfo ci) {
        Entity villager = mcacapitals$getVillagerEntity();
        if (villager == null) {
            return;
        }

        UUID villagerId = villager.getUUID();
        if (villagerId == null) {
            return;
        }

        VillagerIdentityClientCache.ClientVillagerIdentity identity = VillagerIdentityClientCache.get(villagerId);
        if (identity == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.font == null) {
            return;
        }

        List<Component> bottomUpBlocks = new ArrayList<>();

        String wordsLine = identity.houseWordsDisplayLine();
        if (!wordsLine.isBlank()) {
            bottomUpBlocks.add(Component.literal(wordsLine));
        }

        String surnameOrHouseLine = identity.surnameOrHouseDisplayLine();
        if (!surnameOrHouseLine.isBlank()) {
            bottomUpBlocks.add(Component.literal(surnameOrHouseLine));
        }

        String originLine = identity.originDisplayLine();
        if (!originLine.isBlank()) {
            bottomUpBlocks.add(Component.literal(originLine));
        }

        if (bottomUpBlocks.isEmpty()) {
            return;
        }

        int screenHeight = mcacapitals$getScreenHeight();
        int y = screenHeight - MCACAPITALS_SCREEN_MARGIN - MCACAPITALS_TOOLTIP_HEIGHT;

        for (Component block : bottomUpBlocks) {
            graphics.renderTooltip(
                    minecraft.font,
                    block,
                    MCACAPITALS_IDENTITY_X,
                    y
            );
            y -= MCACAPITALS_TOOLTIP_HEIGHT + MCACAPITALS_BLOCK_GAP;
        }
    }

    @Unique
    private Entity mcacapitals$getVillagerEntity() {
        try {
            Field villagerField = mcacapitals$findField(this.getClass(), "villager");
            if (villagerField == null) {
                return null;
            }

            villagerField.setAccessible(true);
            Object villagerLike = villagerField.get(this);
            if (villagerLike == null) {
                return null;
            }

            Method asEntity = villagerLike.getClass().getMethod("asEntity");
            Object entity = asEntity.invoke(villagerLike);
            return entity instanceof Entity e ? e : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private int mcacapitals$getScreenHeight() {
        try {
            return ((Screen) (Object) this).height;
        } catch (Throwable ignored) {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft == null ? 240 : minecraft.getWindow().getGuiScaledHeight();
        }
    }

    @Unique
    private static Field mcacapitals$findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}