package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalExecutionAuthorityService;
import fabric.net.mca.entity.VillagerLike;
import fabric.net.mca.entity.interaction.Constraint;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Constraint.class, remap = false)
public abstract class MayorConstraintCapitalAuthorityMixin {

    @Inject(
            method = "test(Lfabric/net/mca/entity/VillagerLike;Lnet/minecraft/class_3222;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$replaceLegacyMayorAuthority(
            VillagerLike<?> villager,
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this != Constraint.MAYOR) {
            return;
        }
        Entity target = villager == null ? null : villager.asEntity();
        cir.setReturnValue(CapitalExecutionAuthorityService.mayIssueDirectExecution(player, target));
    }
}
