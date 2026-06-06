package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.PlayerHouseIdentityService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.conczin.mca.server.world.data.FamilyTreeNode", remap = false)
public abstract class FamilyTreeNodeAdoptionIdentityMixin {

    @Inject(
            method = "assignParent(Lnet/conczin/mca/server/world/data/FamilyTreeNode;)Z",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void mcacapitals$applyPlayerHouseAfterAdoption(
            @Coerce Object parent,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        PlayerHouseIdentityService.applyAdoptionIdentityFromFamilyNodes(this, parent);
    }
}