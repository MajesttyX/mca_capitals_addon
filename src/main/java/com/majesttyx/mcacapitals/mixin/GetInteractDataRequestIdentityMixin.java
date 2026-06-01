package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import net.conczin.mca.entity.VillagerLike;
import net.conczin.mca.network.c2s.GetInteractDataRequest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.conczin.mca.network.c2s.GetInteractDataRequest", remap = false)
public abstract class GetInteractDataRequestIdentityMixin {

    @Shadow(remap = false)
    public abstract int id();

    @Inject(
            method = "handleServer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void mcacapitals$syncVillagerIdentityToClient(ServerPlayer player, CallbackInfo ci) {
        if (player == null || player.level() == null) {
            return;
        }

        Entity entity = player.level().getEntity(id());
        if (!(entity instanceof VillagerLike<?>)) {
            return;
        }

        VillagerIdentitySyncService.syncToPlayer(player, entity);
    }
}