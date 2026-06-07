package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RoyalScepterHandler {

    public static InteractionResult handleEntityInteract(Player player, Entity rawTarget, InteractionHand hand) {
        if (player == null || rawTarget == null || hand == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.ROYAL_SCEPTER.get())) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(rawTarget instanceof LivingEntity livingTarget)) {
            return InteractionResult.PASS;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(livingTarget)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide) {
            RoyalScepterClient.openScreen(livingTarget.getUUID(), livingTarget.getName().getString());
        }

        return InteractionResult.SUCCESS;
    }
}