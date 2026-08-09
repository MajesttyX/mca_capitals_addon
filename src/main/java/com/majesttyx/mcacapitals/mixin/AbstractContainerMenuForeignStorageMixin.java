package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalForeignStorageRaidService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuForeignStorageMixin {

    @Unique
    private CapitalForeignStorageRaidService.StorageSnapshot mcacapitals$beforeStorage;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void mcacapitals$captureForeignStorage(
            int slotId,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            mcacapitals$beforeStorage = null;
            return;
        }
        mcacapitals$beforeStorage = CapitalForeignStorageRaidService.snapshot(
                serverPlayer.serverLevel(),
                (AbstractContainerMenu) (Object) this
        );
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void mcacapitals$recordForeignStorageRaid(
            int slotId,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || mcacapitals$beforeStorage == null
                || mcacapitals$beforeStorage.owner() == null
                || !CapitalForeignStorageRaidService.isForeign(serverPlayer, mcacapitals$beforeStorage)) {
            mcacapitals$beforeStorage = null;
            return;
        }
        CapitalForeignStorageRaidService.StorageSnapshot after =
                CapitalForeignStorageRaidService.snapshot(
                        serverPlayer.serverLevel(),
                        (AbstractContainerMenu) (Object) this
                );
        if (CapitalForeignStorageRaidService.removedItem(mcacapitals$beforeStorage, after)) {
            CapitalForeignStorageRaidService.recordRaid(
                    serverPlayer,
                    mcacapitals$beforeStorage.owner()
            );
        }
        mcacapitals$beforeStorage = null;
    }
}
