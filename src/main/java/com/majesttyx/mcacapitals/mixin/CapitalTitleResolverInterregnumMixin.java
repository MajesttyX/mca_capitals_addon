package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.capital.CapitalWartimeSuccessionService;
import com.majesttyx.mcacapitals.data.CapitalInterregnumRecord;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = CapitalTitleResolver.class, remap = false)
public abstract class CapitalTitleResolverInterregnumMixin {

    @Inject(
            method = "getDisplayTitle",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$resolveLocalInterregnumTitle(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId,
            CallbackInfoReturnable<String> cir
    ) {
        String title = resolveInterregnumTitle(
                level,
                capital,
                entityId
        );
        if (!title.isBlank()) {
            cir.setReturnValue(title);
            cir.cancel();
        }
    }

    @Inject(
            method = "getDisplayTitleForEntity",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$resolveGlobalInterregnumTitle(
            ServerLevel level,
            UUID entityId,
            CallbackInfoReturnable<String> cir
    ) {
        if (level == null || entityId == null) {
            return;
        }
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            String title = resolveInterregnumTitle(
                    level,
                    capital,
                    entityId
            );
            if (!title.isBlank()) {
                cir.setReturnValue(title);
                cir.cancel();
                return;
            }
        }
    }

    private static String resolveInterregnumTitle(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || entityId == null) {
            return "";
        }
        CapitalInterregnumRecord record =
                CapitalWartimeSuccessionService.getRecord(
                        level,
                        capital.getCapitalId()
                );
        if (record == null
                || !entityId.equals(
                record.getDeceasedSovereignId()
        )) {
            return "";
        }
        if (record.wasDeposition()) {
            return "Commoner";
        }
        return record.wasDeceasedSovereignFemale()
                ? "Queen"
                : "King";
    }
}
