package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.client.RoyalDecreeBetrothalClientHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

@Pseudo
@Mixin(targets = "net.mca.client.gui.InteractScreen", remap = false)
public abstract class InteractScreenBetrothalMixin {

    @Unique
    private static final ResourceLocation MCACAPITALS_BETROTHAL_DECREE_TEXTURE =
            new ResourceLocation("mcacapitals", "textures/item/betrothal_decree.png");

    @Unique
    private static final int MCACAPITALS_SLOT_X = 23;

    @Unique
    private static final int MCACAPITALS_SLOT_Y = 148;

    @Unique
    private static final int MCACAPITALS_TEXTURE_SIZE = 16;

    @Unique
    private static final float MCACAPITALS_ICON_SCALE = 1.5F;

    @Unique
    private static final int MCACAPITALS_TOOLTIP_SIZE = 18;

    @Unique
    private Object mcacapitals$suppressedMarriageState;

    @Unique
    private boolean mcacapitals$marriageStateSuppressed;

    @Inject(method = "drawIcons", at = @At("HEAD"), remap = false)
    private void mcacapitals$suppressMarriageIconForRoyalDecree(GuiGraphics graphics, CallbackInfo ci) {
        if (mcacapitals$getBetrothalDisplayData() == null) {
            return;
        }
        mcacapitals$suppressMarriageState();
    }

    @Inject(method = "drawIcons", at = @At("TAIL"), remap = false)
    private void mcacapitals$drawRoyalDecreeBetrothalIcon(GuiGraphics graphics, CallbackInfo ci) {
        try {
            RoyalDecreeBetrothalClientHelper.BetrothalDisplayData data = mcacapitals$getBetrothalDisplayData();
            if (data == null) {
                return;
            }

            graphics.pose().pushPose();
            graphics.pose().translate(MCACAPITALS_SLOT_X, MCACAPITALS_SLOT_Y, 0.0F);
            graphics.pose().scale(MCACAPITALS_ICON_SCALE, MCACAPITALS_ICON_SCALE, 1.0F);
            graphics.blit(
                    MCACAPITALS_BETROTHAL_DECREE_TEXTURE,
                    0,
                    0,
                    0,
                    0,
                    MCACAPITALS_TEXTURE_SIZE,
                    MCACAPITALS_TEXTURE_SIZE,
                    MCACAPITALS_TEXTURE_SIZE,
                    MCACAPITALS_TEXTURE_SIZE
            );
            graphics.pose().popPose();
        } finally {
            mcacapitals$restoreMarriageState();
        }
    }

    @Inject(method = "drawTextPopups", at = @At("HEAD"), remap = false)
    private void mcacapitals$suppressMarriageTooltipForRoyalDecree(GuiGraphics graphics, CallbackInfo ci) {
        if (mcacapitals$getBetrothalDisplayData() == null) {
            return;
        }
        mcacapitals$suppressMarriageState();
    }

    @Inject(method = "drawTextPopups", at = @At("TAIL"), remap = false)
    private void mcacapitals$drawRoyalDecreeBetrothalTooltip(GuiGraphics graphics, CallbackInfo ci) {
        try {
            RoyalDecreeBetrothalClientHelper.BetrothalDisplayData data = mcacapitals$getBetrothalDisplayData();
            if (data == null) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.font == null) {
                return;
            }

            double mouseX = minecraft.mouseHandler.xpos() * (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();

            if (mouseX < MCACAPITALS_SLOT_X
                    || mouseX > MCACAPITALS_SLOT_X + MCACAPITALS_TOOLTIP_SIZE
                    || mouseY < MCACAPITALS_SLOT_Y
                    || mouseY > MCACAPITALS_SLOT_Y + MCACAPITALS_TOOLTIP_SIZE) {
                return;
            }

            graphics.renderTooltip(
                    minecraft.font,
                    Component.literal("Betrothed to " + data.partnerName() + " by Royal Decree"),
                    MCACAPITALS_SLOT_X + MCACAPITALS_TOOLTIP_SIZE + 4,
                    MCACAPITALS_SLOT_Y + 4
            );
        } finally {
            mcacapitals$restoreMarriageState();
        }
    }

    @Unique
    private void mcacapitals$suppressMarriageState() {
        if (mcacapitals$marriageStateSuppressed) {
            return;
        }

        try {
            Field marriageStateField = mcacapitals$findField(this.getClass(), "marriageState");
            if (marriageStateField == null) {
                return;
            }

            marriageStateField.setAccessible(true);
            Object currentValue = marriageStateField.get(this);
            if (currentValue == null) {
                return;
            }

            mcacapitals$suppressedMarriageState = currentValue;
            marriageStateField.set(this, null);
            mcacapitals$marriageStateSuppressed = true;
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private void mcacapitals$restoreMarriageState() {
        if (!mcacapitals$marriageStateSuppressed) {
            return;
        }

        try {
            Field marriageStateField = mcacapitals$findField(this.getClass(), "marriageState");
            if (marriageStateField == null) {
                return;
            }

            marriageStateField.setAccessible(true);
            marriageStateField.set(this, mcacapitals$suppressedMarriageState);
        } catch (Throwable ignored) {
        } finally {
            mcacapitals$suppressedMarriageState = null;
            mcacapitals$marriageStateSuppressed = false;
        }
    }

    @Unique
    private RoyalDecreeBetrothalClientHelper.BetrothalDisplayData mcacapitals$getBetrothalDisplayData() {
        Entity villager = mcacapitals$getVillagerEntity();
        if (villager == null) {
            return null;
        }

        UUID villagerId = villager.getUUID();
        if (villagerId == null) {
            return null;
        }

        return RoyalDecreeBetrothalClientHelper.getPendingRoyalDecreeBetrothal(villagerId);
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