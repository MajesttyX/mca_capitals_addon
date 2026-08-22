package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.SyncBlueprintAuthorityPacket;
import fabric.net.conczin.mca.server.world.data.Village;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fabric.net.conczin.mca.network.c2s.GetVillageRequest", remap = false)
public abstract class GetVillageRequestCapitalAuthorityMixin {

    @Inject(
            method = "receive",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void mcacapitals$syncCapitalAuthority(ServerPlayer player, CallbackInfo ci) {
        Village.findNearest(player).ifPresentOrElse(
                village -> ModNetwork.sendToPlayer(player, SyncBlueprintAuthorityPacket.create(player, village)),
                () -> ModNetwork.sendToPlayer(player, SyncBlueprintAuthorityPacket.create(player, null))
        );
    }
}