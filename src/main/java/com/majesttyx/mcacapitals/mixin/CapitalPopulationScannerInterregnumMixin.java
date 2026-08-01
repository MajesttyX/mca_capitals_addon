package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalPopulationScanner;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalWartimeSuccessionService;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CapitalPopulationScanner.class, remap = false)
public abstract class CapitalPopulationScannerInterregnumMixin {

    @Inject(
            method = "processSuccession",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$processInterregnumBeforeNullSovereignCheck(
            ServerLevel level,
            CapitalRecord capital,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CapitalWartimeSuccessionService.handleIfNeeded(
                level,
                capital
        )) {
            CapitalDataAccess.markDirty(level);
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(
            method = "issuePendingCharters",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$blockCharterDuringInterregnum(
            ServerLevel level,
            CapitalRecord capital,
            CallbackInfo ci
    ) {
        if (capital != null
                && CapitalWartimeSuccessionService.isInInterregnum(
                level,
                capital.getCapitalId()
        )) {
            ci.cancel();
        }
    }
}