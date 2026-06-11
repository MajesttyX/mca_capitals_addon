package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.MarriageSurnameService;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fabric.net.mca.server.world.data.FamilyTreeNode", remap = false)
public class FamilyTreeNodeMarriageSurnameMixin {

    @Inject(
            method = "updatePartner(Lnet/minecraft/class_1297;Lfabric/net/mca/entity/ai/relationship/RelationshipState;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void mcacapitals$onUpdatePartner(Entity partner, @Coerce Object state, CallbackInfo ci) {
        MarriageSurnameService.onFamilyTreePartnerUpdate(this, partner, state);
    }
}