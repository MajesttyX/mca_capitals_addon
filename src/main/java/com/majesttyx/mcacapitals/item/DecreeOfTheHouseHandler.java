package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenDecreeOfTheHousePacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DecreeOfTheHouseHandler {

    public static InteractionResult handleEntityInteract(Player player, Entity rawTarget, InteractionHand hand) {
        if (player == null || rawTarget == null || hand == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.DECREE_OF_THE_HOUSE.get())) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(rawTarget instanceof LivingEntity target)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            player.sendSystemMessage(Component.literal("A Decree of the House can only be used on an MCA villager."));
            return InteractionResult.SUCCESS;
        }

        OpenDecreeOfTheHousePacket packet = DecreeOfTheHouseService.createOpenPacket(level, target);
        if (packet == null) {
            player.sendSystemMessage(Component.literal("The records for that villager could not be opened."));
            return InteractionResult.SUCCESS;
        }

        ModNetwork.sendToPlayer(serverPlayer, packet);
        return InteractionResult.SUCCESS;
    }
}