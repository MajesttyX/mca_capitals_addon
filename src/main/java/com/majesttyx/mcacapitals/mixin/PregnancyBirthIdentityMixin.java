package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.BirthIdentityService;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import forge.net.conczin.mca.entity.ai.relationship.Gender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "forge.net.conczin.mca.entity.ai.Pregnancy", remap = false)
public abstract class PregnancyBirthIdentityMixin {

    @Shadow
    @Final
    private VillagerEntityMCA mother;

    @Inject(
            method = "createChild(Lforge/net/conczin/mca/entity/ai/relationship/Gender;Lforge/net/conczin/mca/entity/VillagerEntityMCA;)Lforge/net/conczin/mca/entity/VillagerEntityMCA;",
            at = @At("RETURN"),
            remap = false
    )
    private void mcacapitals$assignBirthIdentity(
            Gender gender,
            VillagerEntityMCA partner,
            CallbackInfoReturnable<VillagerEntityMCA> cir
    ) {
        VillagerEntityMCA child = cir.getReturnValue();
        if (child == null || mother == null || partner == null) {
            return;
        }

        if (mother.level() instanceof net.minecraft.server.level.ServerLevel level) {
            BirthIdentityService.applyBirthIdentity(level, child, mother, partner);
        }
    }
}