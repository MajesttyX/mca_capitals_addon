package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalForeignStorageRaidService;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityForeignStorageMixin {

    @Inject(
            method = "canTakeItemFromContainer(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void mcacapitals$protectRecognizedStorageFromAutomation(
            Container destination,
            Container source,
            ItemStack stack,
            int slot,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CapitalForeignStorageRaidService.blocksAutomatedExtraction(source)) {
            cir.setReturnValue(false);
        }
    }
}