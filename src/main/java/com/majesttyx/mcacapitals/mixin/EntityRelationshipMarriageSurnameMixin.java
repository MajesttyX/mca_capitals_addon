package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.MarriageSurnameService;
import forge.net.conczin.mca.advancement.criterion.CriterionMCA;
import forge.net.conczin.mca.entity.ai.BreedableRelationship;
import forge.net.conczin.mca.entity.ai.relationship.EntityRelationship;
import forge.net.conczin.mca.entity.ai.relationship.RelationshipState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "forge.net.conczin.mca.entity.ai.BreedableRelationship", remap = false)
public abstract class EntityRelationshipMarriageSurnameMixin {

    public void marry(Entity spouse) {
        EntityRelationship relationship = (EntityRelationship) (Object) this;

        RelationshipState state = spouse instanceof ServerPlayer
                ? RelationshipState.MARRIED_TO_PLAYER
                : RelationshipState.MARRIED_TO_VILLAGER;

        if (spouse instanceof ServerPlayer spouseEntity) {
            CriterionMCA.GENERIC_EVENT_CRITERION.trigger(spouseEntity, "marriage");
        }

        relationship.getFamilyEntry().updatePartner(spouse, state);

        if ((Object) this instanceof BreedableRelationship breedableRelationship) {
            MarriageSurnameService.onEntityRelationshipMarriage(breedableRelationship, spouse);
        } else {
            MarriageSurnameService.onEntityRelationshipMarriage(relationship, spouse);
        }
    }
}