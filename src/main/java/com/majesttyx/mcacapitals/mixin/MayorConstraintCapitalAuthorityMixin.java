package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalExecutionAuthorityService;
import forge.net.conczin.mca.entity.VillagerLike;
import forge.net.conczin.mca.entity.interaction.Constraint;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = Constraint.class,
        remap = false
)
public abstract class MayorConstraintCapitalAuthorityMixin {

    @Inject(
            method = "test(Lforge/net/mca/entity/VillagerLike;Lnet/minecraft/server/level/ServerPlayer;)Z",
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

        Entity target = villager == null
                ? null
                : villager.asEntity();

        cir.setReturnValue(
                CapitalExecutionAuthorityService
                        .mayIssueDirectExecution(
                                player,
                                target
                        )
        );
    }
}