package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Pseudo
@Mixin(targets = "forge.net.conczin.mca.network.c2s.GetInteractDataRequest", remap = false)
public abstract class GetInteractDataRequestIdentityMixin {

    @Shadow(remap = false)
    @Final
    private UUID uuid;

    @Inject(
            method = "receive(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void mcacapitals$syncVillagerIdentityToClient(ServerPlayer player, CallbackInfo ci) {
        if (player == null || uuid == null) {
            return;
        }

        Entity entity = player.serverLevel().getEntity(uuid);
        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        VillagerIdentitySyncService.syncToPlayer(player, entity);
    }
}