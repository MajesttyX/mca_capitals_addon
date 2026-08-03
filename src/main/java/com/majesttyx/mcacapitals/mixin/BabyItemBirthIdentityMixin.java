package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.identity.PlayerHouseIdentityService;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

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
            CallbackInfoReturnable<Object> cir
    ) {
        if (!(cir.getReturnValue() instanceof Entity child) || stack == null || stack.isEmpty()) {
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
        if (stack == null || stack.isEmpty() || !ModItemStackData.hasCustomData(stack)) {
            return null;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);
        if (!tag.contains("baby")) {
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