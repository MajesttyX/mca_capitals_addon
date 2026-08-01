package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.PlayerHouseIdentityService;
import forge.net.mca.entity.VillagerEntityMCA;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Pseudo
@Mixin(targets = "forge.net.mca.item.BabyItem", remap = false)
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

        CompoundTag baby = getBabyTag(stack);
        if (baby == null) {
            return;
        }

        UUID mother = getUuid(baby, "mother");
        UUID father = getUuid(baby, "father");

        PlayerHouseIdentityService.applyBirthIdentityFromParentIds(level, child, mother, father);
    }

    private static CompoundTag getBabyTag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return null;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("baby")) {
            return null;
        }

        return tag.getCompound("baby");
    }

    private static UUID getUuid(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.hasUUID(key)) {
            return null;
        }

        return tag.getUUID(key);
    }
}