package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class SuccessionDecreeHandler {

    public static InteractionResult handleEntityInteract(Player player, Entity rawTarget, InteractionHand hand) {
        if (player == null || rawTarget == null || hand == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.BLANK_SUCCESSION_DECREE.get())) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(rawTarget instanceof LivingEntity livingTarget)) {
            return InteractionResult.PASS;
        }

        boolean validTargetType = rawTarget instanceof Player
                || MCAIntegrationBridge.isMCAVillagerEntity(livingTarget);

        if (!validTargetType) {
            return InteractionResult.PASS;
        }

        if (!SuccessionDecreeItem.isBound(held)) {
            if (!player.level().isClientSide) {
                player.sendSystemMessage(Component.translatable("mcacapitals.system.succession_decree_handler.bind_the_succession_decree_to_a_capital_before_using_it"));
            }
            return InteractionResult.SUCCESS;
        }

        if (player.level().isClientSide) {
            UUID capitalId = SuccessionDecreeItem.getBoundCapitalId(held);
            if (capitalId != null) {
                SuccessionDecreeClient.openScreen(
                        capitalId,
                        SuccessionDecreeItem.getBoundCapitalName(held),
                        livingTarget.getUUID(),
                        livingTarget.getName().getString()
                );
            }
        }

        return InteractionResult.SUCCESS;
    }
}