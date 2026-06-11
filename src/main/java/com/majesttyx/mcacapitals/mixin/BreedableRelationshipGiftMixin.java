package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.item.BetrothalDecreeHandler;
import com.majesttyx.mcacapitals.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "fabric.net.mca.entity.ai.BreedableRelationship", remap = false)
public class BreedableRelationshipGiftMixin {

    @Inject(
            method = "handleSpecialCaseGift",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void mcacapitals$handleBetrothalDecreeGift(ServerPlayer player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || stack == null || !stack.is(ModItems.BETROTHAL_DECREE.get())) {
            return;
        }

        Entity villager = resolveEntity();
        if (villager == null) {
            return;
        }

        boolean handled = BetrothalDecreeHandler.tryGiftBetrothalDecree(player, villager, stack);
        if (handled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private Entity resolveEntity() {
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