package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.PlayerHouseIdentityService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.mca.server.world.data.FamilyTreeNode", remap = false)
public class FamilyTreeNodeAdoptionIdentityMixin {

    @Inject(
            method = "assignParent(Lnet/mca/server/world/data/FamilyTreeNode;)Z",
            at = @At("RETURN"),
            remap = false
    )
    private void mcacapitals$applyAdoptionIdentity(Object parent, CallbackInfoReturnable<Boolean> cir) {
        if (cir == null || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        PlayerHouseIdentityService.applyAdoptionIdentityFromFamilyNodes(this, parent);
    }
}