package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalSuccessionService;
import com.majesttyx.mcacapitals.capital.CapitalWartimeSuccessionService;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CapitalSuccessionService.class, remap = false)
public abstract class CapitalSuccessionServiceInterregnumMixin {

    @Inject(
            method = "handleSuccessionIfNeeded",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$handleWartimeInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CapitalWartimeSuccessionService.handleIfNeeded(
                level,
                capital
        )) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
