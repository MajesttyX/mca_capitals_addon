package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.item.SealedPurseHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "forge.net.mca.entity.ai.BreedableRelationship", remap = false)
public class SealedPurseGiftMixin {

    @Inject(
            method = "handleSpecialCaseGift(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$handleSealedPurseGift(ServerPlayer player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || stack == null || !stack.is(ModItems.SEALED_PURSE.get())) {
            return;
        }

        Entity villager = mcacapitals$resolveSealedPurseGiftEntity();
        if (villager == null) {
            return;
        }

        boolean handled = SealedPurseHandler.tryGiftSealedPurse(player, villager, stack);
        if (handled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Unique
    private Entity mcacapitals$resolveSealedPurseGiftEntity() {
        Class<?> type = this.getClass();

        while (type != null) {
            try {
                Field field = type.getDeclaredField("entity");
                field.setAccessible(true);
                Object value = field.get(this);
                if (value instanceof Entity entity) {
                    return entity;
                }
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
                continue;
            } catch (Throwable ignored) {
                return null;
            }

            type = type.getSuperclass();
        }

        return null;
    }
}