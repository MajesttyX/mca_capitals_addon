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
@Mixin(targets = "forge.net.mca.server.world.data.FamilyTreeNode", remap = false)
public abstract class FamilyTreeNodeMarriageSurnameMixin {

    @Inject(
            method = "updatePartner(Lnet/minecraft/world/entity/Entity;Lforge/net/mca/entity/ai/relationship/RelationshipState;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void mcacapitals$applyMarriageSurnameAfterPartnerUpdate(
            Entity newPartner,
            @Coerce Object relationshipState,
            CallbackInfo ci
    ) {
        MarriageSurnameService.onFamilyTreePartnerUpdate(this, newPartner, relationshipState);
    }
}