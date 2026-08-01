package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalForeignStorageRaidService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeForeignStorageMixin {

    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Unique
    private CapitalForeignStorageRaidService.StorageSnapshot mcacapitals$storageBeforeBreak;

    @Inject(
            method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD")
    )
    private void mcacapitals$captureStorageBeforeBreak(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        mcacapitals$storageBeforeBreak = CapitalForeignStorageRaidService.snapshotBlock(
                level,
                pos
        );
    }

    @Inject(
            method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z",
            at = @At("RETURN")
    )
    private void mcacapitals$recordStorageBreakRaid(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        CapitalForeignStorageRaidService.StorageSnapshot before = mcacapitals$storageBeforeBreak;
        mcacapitals$storageBeforeBreak = null;
        if (!cir.getReturnValue()
                || before == null
                || before.owner() == null
                || before.contents().isEmpty()
                || !CapitalForeignStorageRaidService.isForeign(player, before)) {
            return;
        }
        CapitalForeignStorageRaidService.recordRaid(player, before.owner());
    }
}