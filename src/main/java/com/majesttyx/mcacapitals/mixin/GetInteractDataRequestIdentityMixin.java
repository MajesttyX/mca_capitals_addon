package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorUrgentMatterService;
import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.conczin.mca.network.c2s.GetInteractDataRequest", remap = false)
public class GetInteractDataRequestIdentityMixin {

    @Shadow(remap = false)
    @Final
    private int id;

    @Inject(
            method = "handleServer",
            at = @At("TAIL"),
            remap = false
    )
    private void mcacapitals$syncIdentityOnInteractDataRequest(ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }

        Entity entity = player.serverLevel().getEntity(id);
        if (entity == null) {
            return;
        }

        VillagerIdentitySyncService.syncToPlayer(player, entity);
        CapitalAmbassadorUrgentMatterService.openIfNeeded(player, entity);
    }
}