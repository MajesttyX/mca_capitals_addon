package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.PlayerHouseIdentityService;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.item.components.BabyParentsComponent;
import net.conczin.mca.registry.DataComponentsMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.conczin.mca.item.BabyItem", remap = false)
public abstract class BabyItemBirthIdentityMixin {

    @Inject(
            method = "birthChild",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void mcacapitals$applyPlayerHouseBirthIdentity(
            ItemStack stack,
            ServerLevel level,
            ServerPlayer player,
            CallbackInfoReturnable<VillagerEntityMCA> cir
    ) {
        VillagerEntityMCA child = cir.getReturnValue();
        if (child == null || stack == null || stack.isEmpty()) {
            return;
        }

        BabyParentsComponent parents = stack.get(DataComponentsMCA.BABY_PARENTS);
        if (parents == null) {
            return;
        }

        PlayerHouseIdentityService.applyBirthIdentityFromParentIds(level, child, parents.mother(), parents.father());
    }
}