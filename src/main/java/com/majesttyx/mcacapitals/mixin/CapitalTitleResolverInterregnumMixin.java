package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.capital.CapitalWartimeSuccessionService;
import com.majesttyx.mcacapitals.data.CapitalInterregnumRecord;
import net.minecraft.network.chat.Component;
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
        Component title = resolveInterregnumTitleComponent(level, capital, entityId);
        if (title != null) {
            cir.setReturnValue(title.getString());
            cir.cancel();
        }
    }

    @Inject(
            method = "getDisplayTitleComponent",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$resolveLocalInterregnumTitleComponent(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId,
            CallbackInfoReturnable<Component> cir
    ) {
        Component title = resolveInterregnumTitleComponent(level, capital, entityId);
        if (title != null) {
            cir.setReturnValue(title);
            cir.cancel();
        }
    }

    @Inject(
            method = "getResolvedTitleId",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$resolveLocalInterregnumTitleId(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId,
            CallbackInfoReturnable<CapitalTitleResolver.ResolvedTitleId> cir
    ) {
        CapitalTitleResolver.ResolvedTitleId titleId = resolveInterregnumTitleId(level, capital, entityId);
        if (titleId != null) {
            cir.setReturnValue(titleId);
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
        Component title = resolveGlobalInterregnumTitleComponent(level, entityId);
        if (title != null) {
            cir.setReturnValue(title.getString());
            cir.cancel();
        }
    }

    @Inject(
            method = "getDisplayTitleComponentForEntity",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$resolveGlobalInterregnumTitleComponent(
            ServerLevel level,
            UUID entityId,
            CallbackInfoReturnable<Component> cir
    ) {
        Component title = resolveGlobalInterregnumTitleComponent(level, entityId);
        if (title != null) {
            cir.setReturnValue(title);
            cir.cancel();
        }
    }

    @Inject(
            method = "getResolvedTitleIdForEntity",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$resolveGlobalInterregnumTitleId(
            ServerLevel level,
            UUID entityId,
            CallbackInfoReturnable<CapitalTitleResolver.ResolvedTitleId> cir
    ) {
        CapitalTitleResolver.ResolvedTitleId titleId = resolveGlobalInterregnumTitleId(level, entityId);
        if (titleId != null) {
            cir.setReturnValue(titleId);
            cir.cancel();
        }
    }

    private static Component resolveGlobalInterregnumTitleComponent(
            ServerLevel level,
            UUID entityId
    ) {
        if (level == null || entityId == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            Component title = resolveInterregnumTitleComponent(level, capital, entityId);
            if (title != null) {
                return title;
            }
        }

        return null;
    }

    private static CapitalTitleResolver.ResolvedTitleId resolveGlobalInterregnumTitleId(
            ServerLevel level,
            UUID entityId
    ) {
        if (level == null || entityId == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            CapitalTitleResolver.ResolvedTitleId titleId = resolveInterregnumTitleId(level, capital, entityId);
            if (titleId != null) {
                return titleId;
            }
        }

        return null;
    }

    private static Component resolveInterregnumTitleComponent(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        CapitalInterregnumRecord record = resolveInterregnumRecord(level, capital, entityId);
        if (record == null) {
            return null;
        }

        if (record.wasDeposition()) {
            return Component.translatable("mcacapitals.dynamic.title.commoner");
        }

        return Component.translatable(
                record.wasDeceasedSovereignFemale()
                        ? "mcacapitals.dynamic.title.sovereign.female"
                        : "mcacapitals.dynamic.title.sovereign.male"
        );
    }

    private static CapitalTitleResolver.ResolvedTitleId resolveInterregnumTitleId(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        CapitalInterregnumRecord record = resolveInterregnumRecord(level, capital, entityId);
        if (record == null) {
            return null;
        }

        return record.wasDeposition()
                ? CapitalTitleResolver.ResolvedTitleId.COMMONER
                : CapitalTitleResolver.ResolvedTitleId.SOVEREIGN;
    }

    private static CapitalInterregnumRecord resolveInterregnumRecord(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || entityId == null) {
            return null;
        }

        CapitalInterregnumRecord record = CapitalWartimeSuccessionService.getRecord(
                level,
                capital.getCapitalId()
        );

        if (record == null || !entityId.equals(record.getDeceasedSovereignId())) {
            return null;
        }

        return record;
    }
}
