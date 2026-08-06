package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.MarriageSurnameService;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Capitals surname and House rules after MCA updates a family-tree
 * partnership. MCA Reborn 7.7.32 records marriage through
 * FamilyTreeNode.updatePartner(Entity, RelationshipState); it does not expose
 * BreedableRelationship.marry(Entity).
 */
@Pseudo
@Mixin(
        targets = "net.conczin.mca.server.world.data.FamilyTreeNode",
        remap = false
)
public abstract class FamilyTreeNodeMarriageSurnameMixin {

    @Inject(
            method = "updatePartner(Lnet/minecraft/world/entity/Entity;Lnet/conczin/mca/entity/ai/relationship/RelationshipState;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void mcacapitals$applyMarriageSurname(
            Entity partner,
            @Coerce Object relationshipState,
            CallbackInfo ci
    ) {
        MarriageSurnameService.onFamilyTreePartnerUpdate(
                this,
                partner,
                relationshipState
        );
    }
}
