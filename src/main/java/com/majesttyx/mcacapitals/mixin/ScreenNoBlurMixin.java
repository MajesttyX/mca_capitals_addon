package com.majesttyx.mcacapitals.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenNoBlurMixin {
    private static final String CAPITALS_PACKAGE_PREFIX =
            "com.majesttyx.mcacapitals.";

    @Inject(
            method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mcacapitals$disableBackgroundForCapitalsScreens(
            GuiGraphics graphics,
            CallbackInfo ci
    ) {
        String screenClassName = ((Object) this).getClass().getName();
        if (screenClassName.startsWith(CAPITALS_PACKAGE_PREFIX)) {
            ci.cancel();
        }
    }
}
